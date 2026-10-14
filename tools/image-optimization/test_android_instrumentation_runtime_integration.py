"""Portable linkage refusal cases. Real private APK/SDK checks run explicitly."""
from hashlib import sha256
from pathlib import Path
import os
import re
import contextlib
import io
import tempfile
import time
import unittest
from unittest.mock import patch
from types import SimpleNamespace

import android_instrumentation_runtime_integration as runtime
import native_entry_android_plugins as plugins
import native_entry_driver as driver
from native_entry_config import EntryError, frozen_json, write_private
from test_native_entry_android_plugins import GENERATED, registrant


def definition(name, signature="<init>()V", flags="public static", body=" return-void", parent="java.lang.Object"):
    return (".class public " + plugins.descriptor(name) + "\n.super " + plugins.descriptor(parent) +
        "\n.method " + flags + " " + signature + "\n" + body + "\n.end method\n").encode()


class RuntimeTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="instrumentation-runtime-")
        self.addCleanup(temporary.cleanup)
        self.base = Path(temporary.name).resolve()
        self.base.chmod(0o700)
        self.app, self.test = self.file("app.apk", b"app bytes"), self.file("test.apk", b"test bytes")
        self.output = self.base / "output"
        self.output.mkdir(mode=0o700)
        self.definitions = {
            "kotlin.text.StringsKt": definition("kotlin.text.StringsKt", parent="kotlin.text.StringsKt__StringsJVMKt"),
            "kotlin.text.StringsKt__StringsJVMKt": definition("kotlin.text.StringsKt__StringsJVMKt",
                "repeat(Ljava/lang/CharSequence;I)Ljava/lang/String;", body=" return-object v0"),
            "kotlin.ranges.RangesKt": definition("kotlin.ranges.RangesKt", "coerceAtLeast(JJ)J", body=" return-wide v0"),
            "kotlin.ResultKt": definition("kotlin.ResultKt", "createFailure(Ljava/lang/Throwable;)Ljava/lang/Object;", body=" return-object v0")}
        calls = (" invoke-static {v0, v1}, Lkotlin/text/StringsKt;->repeat(Ljava/lang/CharSequence;I)Ljava/lang/String;\n"
            " invoke-static/range {v0 .. v3}, Lkotlin/ranges/RangesKt;->coerceAtLeast(JJ)J\n"
            " invoke-static {v0}, Lkotlin/ResultKt;->createFailure(Ljava/lang/Throwable;)Ljava/lang/Object;\n return-void")
        self.test_classes = {name: definition(name, body=calls) for name in runtime.ROOT_CLASSES}
        self.inventory = "".join("C d 1\t1\t100\t" + name + "\n" for name in runtime.ROOT_CLASSES).encode()
        self.calls = []

    def file(self, name, raw):
        path = self.base / name
        path.write_bytes(raw)
        path.chmod(0o600)
        return path

    def inspect(self, command=None, hash_function=None):
        with patch.object(driver, "checked_run", side_effect=command or self.command), \
                patch.object(driver, "artifact_hash", side_effect=hash_function or driver.artifact_hash):
            return runtime.check_runtime(self.app, self.test, Path("/explicit/apkanalyzer"), self.output, time.monotonic() + 5)

    def command(self, argv, output, alias, **kwargs):
        self.calls.append(argv)
        for suffix, raw in ((".stderr", b"valid diagnostic\n"), (".json", frozen_json({"status": "ok"}))):
            path = output / (alias + suffix)
            path.write_bytes(raw)
            path.chmod(0o600)
        if argv[2] == "packages":
            return self.inventory
        name = argv[-2]
        values = self.test_classes if str(argv[-1]) == str(self.test) else self.definitions
        if name not in values:
            raise EntryError("tool_failed")
        return values[name]

    def test_actual_static_calls_and_inherited_definitions_are_byte_bound(self):
        result = self.inspect()
        self.assertEqual(result["artifactSha256"], {"app": sha256(b"app bytes").hexdigest(), "test": sha256(b"test bytes").hexdigest()})
        self.assertEqual(len(result["calls"]), 3)
        self.assertEqual(next(call["definedBy"] for call in result["calls"] if call["owner"] == "kotlin.text.StringsKt"),
            "kotlin.text.StringsKt__StringsJVMKt")
        self.assertTrue(all("--proguard-mappings" not in argv for argv in self.calls))

    def test_definition_and_mapping_reference_decoys_refuse(self):
        for raw in (b"", b"kotlin.text.StringsKt -> kotlin.text.StringsKt:\n",
                b"C r 0 1 0 kotlin.text.StringsKt\n", b"const-class v0, Lkotlin/text/StringsKt;\n",
                definition("decoy.Class", body=' const-string v0, "kotlin.text.StringsKt"\n return-void'), b"\xff"):
            with self.subTest(raw=raw), self.assertRaises(EntryError):
                runtime.runtime_definition(raw, "kotlin.text.StringsKt")

    def test_facade_only_and_partial_retention_cannot_satisfy_calls(self):
        for missing in ("kotlin.text.StringsKt__StringsJVMKt", "kotlin.ranges.RangesKt", "kotlin.ResultKt"):
            removed = self.definitions.pop(missing)
            with self.subTest(missing=missing), self.assertRaisesRegex(EntryError, "^tool_failed$"):
                self.inspect()
            self.definitions[missing] = removed

    def test_non_callable_or_reference_only_method_refuses(self):
        name, signature = "kotlin.ResultKt", "createFailure(Ljava/lang/Throwable;)Ljava/lang/Object;"
        for raw in (definition(name, signature, flags="private static", body=" return-object v0"),
                definition(name, signature, flags="public native"), definition(name, signature, flags="public abstract"),
                definition(name, signature, flags="public", body=" return-object v0"),
                definition(name, body=" const-string v0, \"" + signature + "\"\n return-void"),
                definition(name, signature, body=" nop")):
            with self.subTest(raw=raw), self.assertRaises(EntryError):
                runtime.resolve_static(name, signature, lambda _: raw)

    def test_malformed_duplicate_truncated_and_cyclic_definitions_refuse(self):
        raw = definition("kotlin.ResultKt")
        for broken in (raw.replace(b".end method\n", b""), raw + raw,
                raw.replace(b".method public static <init>()V", b".method   "),
                raw + b".method public static <init>()V\n return-void\n.end method\n"):
            with self.subTest(raw=broken), self.assertRaises(EntryError):
                runtime.runtime_definition(broken, "kotlin.ResultKt")
        with self.assertRaisesRegex(EntryError, "runtime_inheritance_rejected"):
            runtime.resolve_static("kotlin.ResultKt", "missing()V",
                lambda name: definition(name, parent=name))

    def test_inventory_reference_rows_duplicates_unbounded_or_missing_roots_refuse(self):
        for raw in (b"", self.inventory.replace(b"C d", b"C r"), self.inventory + self.inventory,
                self.inventory.splitlines()[0] + b"\n", b"\xff"):
            with self.subTest(raw=raw), self.assertRaisesRegex(EntryError, "runtime_inventory_rejected"):
                runtime.defined_inventory(raw)

    def test_expected_runtime_call_references_cannot_be_replaced_by_strings(self):
        self.test_classes = {name: definition(name, body=' const-string v0, "Lkotlin/text/StringsKt;->repeat"\n return-void')
            for name in runtime.ROOT_CLASSES}
        with self.assertRaisesRegex(EntryError, "runtime_references_incomplete"):
            self.inspect()

    def test_tool_failures_timeouts_missing_tool_and_output_limit_refuse(self):
        for reason in ("tool_timeout", "tool_failed", "tool_output_limit", "tool_missing_executable"):
            with self.subTest(reason=reason), self.assertRaisesRegex(EntryError, "^" + reason + "$"):
                self.inspect(command=lambda *args, **kwargs: (_ for _ in ()).throw(EntryError(reason)))

    def test_either_apk_byte_mismatch_refuses_even_after_valid_definitions(self):
        for hashes in (("a" * 64, "b" * 64, "c" * 64, "b" * 64), ("a" * 64, "b" * 64, "a" * 64, "c" * 64)):
            selected = iter(hashes)
            with self.subTest(hashes=hashes), self.assertRaisesRegex(EntryError, "runtime_apk_binding_changed"):
                self.inspect(hash_function=lambda _: next(selected))

    def test_missing_or_incomplete_explicit_inputs_refuse_without_skips(self):
        with self.assertRaises(EntryError):
            runtime.load_inputs(self.base / "absent.json")
        path = self.file("config.json", frozen_json({"scope": runtime.SCOPE, "schemaVersion": 1}))
        with self.assertRaisesRegex(EntryError, "schema_rejected"):
            runtime.load_inputs(path)

    def test_runtime_retention_remains_in_existing_experiment_only_release_selection(self):
        app = Path(__file__).resolve().parents[2] / "app/mobile/android/app"
        gradle = (app / "build.gradle.kts").read_text()
        selectors = re.findall(r"[^\n]*proguardFiles\([^\n]+", gradle)
        self.assertEqual(selectors, ['            if (experimentEnabled) proguardFiles("src/imageExperiment/proguard-rules.pro")'])
        self.assertRegex(gradle, r"buildTypes\s*\{\s*release\s*\{")
        rules = (app / "src/imageExperiment/proguard-rules.pro").read_text()
        self.assertIn("-keep class com.tellpal.imageexperiment.** { *; }", rules)
        self.assertIn("-keep class kotlin.** { *; }", rules)
        self.assertNotRegex(rules, r"-dont(?:shrink|optimize|obfuscate)\b")
        self.assertNotRegex(gradle, r"(?:isMinifyEnabled|minifyEnabled)\s*(?:=\s*)?false\b")

    def test_archive_hash_and_immutable_r233_pin_must_both_match(self):
        receipt = {"platform": "android", "mode": "release",
            "expectedAppId": "com.tellpal.tellpal_mobile_poc.imageexperiment.release",
            "testAppId": "com.tellpal.tellpal_mobile_poc.imageexperiment.release.test",
            "artifactPath": str(self.app), "testArtifactPath": str(self.test),
            "artifactSha256": driver.artifact_hash(self.app), "testArtifactSha256": driver.artifact_hash(self.test)}
        path = self.file("receipt.json", frozen_json(receipt))
        with self.assertRaisesRegex(EntryError, "runtime_r233_binding_rejected"):
            runtime.load_receipt(path, "release", old=True)
        receipt["artifactSha256"] = "f" * 64
        path.write_bytes(frozen_json(receipt))
        with self.assertRaisesRegex(EntryError, "runtime_archive_binding_rejected"):
            runtime.load_receipt(path, "release")

    def test_changed_tool_bytes_never_create_an_accepted_receipt(self):
        tool = self.file("apkanalyzer", b"original public tool bytes")
        tool.chmod(0o755)
        receipt = {"artifactPath": str(self.app), "testArtifactPath": str(self.test)}
        receipts = {mode: (receipt, "a" * 64) for mode in ("old", "profile", "release")}
        def inspect(*args, **kwargs):
            tool.write_bytes(b"changed public tool bytes")
            return {"status": "synthetic_complete"}
        public = io.StringIO()
        with patch.object(runtime, "load_inputs", return_value=({"apkanalyzer": tool}, receipts, self.output, "b" * 64)), \
                patch.object(runtime, "old_negative", return_value={}), \
                patch.object(runtime, "check_runtime", side_effect=inspect), \
                patch.object(runtime, "check_plugins", return_value={}), contextlib.redirect_stdout(public):
            self.assertEqual(runtime.main(["--config", "/explicit/config.json"]), 4)
        self.assertIn('"reason":"runtime_tool_binding_changed"', public.getvalue())
        self.assertFalse((self.output / "integration-receipt.json").exists())
        self.assertTrue((self.output / "integration-failure.json").exists())

    def test_malformed_build_receipt_roots_have_fixed_cli_refusal(self):
        for index, root in enumerate((None, [], "receipt", 1, True)):
            path = self.file("malformed-receipt-" + str(index), frozen_json(root))
            public = io.StringIO()
            with patch.object(runtime, "load_inputs", side_effect=lambda _: runtime.load_receipt(path, "release")), \
                    contextlib.redirect_stdout(public), self.subTest(root=root):
                self.assertEqual(runtime.main(["--config", "/explicit/config.json"]), 4)
                self.assertIn('"reason":"runtime_build_receipt_rejected"', public.getvalue())

    def plugin_fixture(self):
        build = self.base / "build"
        mobile = build / "source/repository/app/mobile"
        source, copied = mobile / plugins.SOURCE, build / "gradle/app" / plugins.COPIED
        for path in (source, copied):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(GENERATED)
            path.chmod(0o600)
        digest = sha256(GENERATED).hexdigest()
        receipt = {"mode": "profile", "sourceCopyPath": str(mobile), "artifactPath": str(self.app),
            "artifactSha256": driver.artifact_hash(self.app), "androidPluginRegistration": {
                "status": "defined_registration_verified", "artifactSha256": driver.artifact_hash(self.app),
                "generatedSourceSha256": digest, "compiledSourceSha256": digest}}
        return receipt, source, copied

    def plugin_inspection(self, argv, *args):
        name = argv[-2]
        return (registrant() if name == plugins.REGISTRANT else definition(name)), {"syntheticInspection": True}

    def test_malformed_plugin_provenance_and_source_ancestry_refuse(self):
        receipt, _, _ = self.plugin_fixture()
        for value in (None, [], "provenance", 3, True):
            with self.subTest(value=value), self.assertRaisesRegex(EntryError, "^runtime_plugin_provenance_rejected$"):
                runtime.check_plugins(receipt | {"androidPluginRegistration": value}, "/tool", self.output, time.monotonic() + 5)
        link = self.base / "link"
        link.symlink_to(self.base, target_is_directory=True)
        for path in ("/", "/private", "/private/tmp", "relative", None, [], str(link)):
            with self.subTest(path=path), self.assertRaisesRegex(EntryError, "^runtime_plugin_provenance_rejected$"):
                runtime.check_plugins(receipt | {"sourceCopyPath": path}, "/tool", self.output, time.monotonic() + 5)

    def test_equal_source_copy_comment_mutation_diverges_from_recorded_provenance(self):
        receipt, source, copied = self.plugin_fixture()
        with patch.object(runtime, "inspection", side_effect=self.plugin_inspection):
            self.assertEqual(runtime.check_plugins(receipt, "/tool", self.output, time.monotonic() + 5)["status"],
                "defined_registration_verified")
            for path in (source, copied):
                path.write_bytes(GENERATED + b"// equal post-build source mutation\n")
            self.assertEqual(plugins.generated_source(source), plugins.generated_source(copied))
            self.assertNotEqual(plugins.generated_source(source), receipt["androidPluginRegistration"]["generatedSourceSha256"])
            with self.assertRaisesRegex(EntryError, "^runtime_plugin_provenance_rejected$"):
                runtime.check_plugins(receipt, "/tool", self.output, time.monotonic() + 5)

    def main_fixture(self, output):
        tool = self.file("stable-tool", b"stable executable bytes")
        paths, receipts = {"apkanalyzer": tool}, {}
        for key, mode in (("profileBuildReceipt", "profile"), ("releaseBuildReceipt", "release"), ("oldReleaseBuildReceipt", "old")):
            selected_mode = "release" if mode == "old" else mode
            app_id = "com.tellpal.tellpal_mobile_poc.imageexperiment." + selected_mode
            receipt = {"platform": "android", "mode": selected_mode, "expectedAppId": app_id,
                "testAppId": app_id + ".test", "artifactPath": str(self.app), "testArtifactPath": str(self.test),
                "artifactSha256": driver.artifact_hash(self.app), "testArtifactSha256": driver.artifact_hash(self.test)}
            paths[key] = self.file(key + ".json", frozen_json(receipt))
            receipts[mode] = runtime.load_receipt(paths[key], selected_mode)
        return paths, receipts, output, "a" * 64

    def test_each_build_receipt_mutation_refuses_with_immutable_apks(self):
        for index, key in enumerate(("profileBuildReceipt", "releaseBuildReceipt", "oldReleaseBuildReceipt")):
            output = self.base / ("receipt-mutation-" + str(index))
            output.mkdir(mode=0o700)
            fixture = self.main_fixture(output)
            paths = fixture[0]
            before = (driver.artifact_hash(self.app), driver.artifact_hash(self.test))
            def inspect(*args):
                path = paths[key]
                path.write_bytes(path.read_bytes() + b"\n")
                return {"status": "synthetic_complete"}
            public = io.StringIO()
            with patch.object(runtime, "load_inputs", return_value=fixture), \
                    patch.object(runtime, "old_negative", return_value={}), \
                    patch.object(runtime, "check_runtime", side_effect=inspect), \
                    patch.object(runtime, "check_plugins", return_value={}), \
                    patch.object(runtime, "OLD_APP", before[0]), patch.object(runtime, "OLD_TEST", before[1]), \
                    contextlib.redirect_stdout(public), self.subTest(receipt=key):
                self.assertEqual(runtime.main(["--config", "/explicit/config.json"]), 4)
            self.assertEqual(before, (driver.artifact_hash(self.app), driver.artifact_hash(self.test)))
            self.assertIn('"reason":"runtime_receipt_binding_changed"', public.getvalue())
            self.assertFalse((output / "integration-receipt.json").exists())

    def test_failure_receipt_storage_failure_keeps_public_refusal(self):
        fixture = self.main_fixture(self.output)
        for failure in (OSError("full diagnostic storage"), EntryError("receipt_write_failed")):
            public = io.StringIO()
            with patch.object(runtime, "load_inputs", return_value=fixture), \
                    patch.object(runtime, "old_negative", side_effect=EntryError("runtime_negative_unverified")), \
                    patch.object(runtime, "write_private", side_effect=failure), contextlib.redirect_stdout(public):
                self.assertEqual(runtime.main(["--config", "/explicit/config.json"]), 4)
            self.assertIn('"reason":"runtime_negative_unverified"', public.getvalue())
            self.assertFalse((self.output / "integration-receipt.json").exists())
        occupied = self.output / "integration-failure.json"
        occupied.write_bytes(b"original diagnostic bytes")
        occupied.chmod(0o600)
        public = io.StringIO()
        with patch.object(runtime, "load_inputs", return_value=fixture), \
                patch.object(runtime, "old_negative", side_effect=EntryError("runtime_negative_unverified")), \
                contextlib.redirect_stdout(public):
            self.assertEqual(runtime.main(["--config", "/explicit/config.json"]), 4)
        self.assertIn('"reason":"runtime_negative_unverified"', public.getvalue())
        self.assertEqual(occupied.read_bytes(), b"original diagnostic bytes")

    def test_success_receipt_storage_failure_cannot_become_success(self):
        fixture = self.main_fixture(self.output)
        public = io.StringIO()
        def write(path, value):
            if path.name == "integration-receipt.json":
                raise EntryError("receipt_write_failed")
            return write_private(path, value)
        with patch.object(runtime, "load_inputs", return_value=fixture), \
                patch.object(runtime, "old_negative", return_value={}), \
                patch.object(runtime, "check_runtime", return_value={}), patch.object(runtime, "check_plugins", return_value={}), \
                patch.object(runtime, "OLD_APP", driver.artifact_hash(self.app)), \
                patch.object(runtime, "OLD_TEST", driver.artifact_hash(self.test)), \
                patch.object(runtime, "write_private", side_effect=write), contextlib.redirect_stdout(public):
            self.assertEqual(runtime.main(["--config", "/explicit/config.json"]), 4)
        self.assertIn('"reason":"receipt_write_failed"', public.getvalue())
        self.assertFalse((self.output / "integration-receipt.json").exists())

    def test_public_tool_reads_allow_atime_only_changes(self):
        tool = self.file("tool", b"stable public SDK bytes")
        tool.chmod(0o755)
        original = os.fstat
        observations = []
        def changed_atime(descriptor):
            info = original(descriptor)
            observations.append(info.st_atime_ns + len(observations) + 1)
            fields = {name: getattr(info, name) for name in ("st_dev", "st_ino", "st_mode", "st_size", "st_mtime_ns", "st_ctime_ns")}
            return SimpleNamespace(**fields, st_atime_ns=observations[-1])
        with patch.object(driver.os, "fstat", side_effect=changed_atime):
            self.assertEqual(runtime.tool_digest(tool), sha256(b"stable public SDK bytes").hexdigest())
        self.assertGreater(len(observations), 1)
        self.assertNotEqual(observations[0], observations[-1])

    def test_tool_content_size_mtime_ctime_and_path_mutations_refuse(self):
        original_read, original_stat = os.read, os.fstat
        for change in ("content", "size", "mtime", "ctime", "path"):
            tool = self.file("tool-" + change, b"stable tool")
            mutated = [False]
            def read(descriptor, count):
                raw = original_read(descriptor, count)
                if raw and not mutated[0]:
                    mutated[0] = True
                    if change == "content":
                        tool.write_bytes(b"changed one")
                    elif change == "size":
                        tool.write_bytes(b"changed longer tool")
                    elif change == "mtime":
                        info = tool.stat()
                        os.utime(tool, ns=(info.st_atime_ns, info.st_mtime_ns + 1_000_000_000))
                    elif change == "path":
                        replacement = self.file("replacement", b"stable tool")
                        replacement.replace(tool)
                return raw
            def stat_changed(descriptor):
                info = original_stat(descriptor)
                if change != "ctime" or not mutated[0]:
                    return info
                fields = {name: getattr(info, name) for name in ("st_dev", "st_ino", "st_mode", "st_size", "st_mtime_ns")}
                return SimpleNamespace(**fields, st_ctime_ns=info.st_ctime_ns + 1)
            with patch.object(driver.os, "read", side_effect=read), \
                    patch.object(driver.os, "fstat", side_effect=stat_changed), \
                    self.subTest(change=change), self.assertRaises(EntryError):
                runtime.tool_digest(tool)
        tool = self.file("too-large-tool", b"")
        with tool.open("wb") as handle:
            handle.truncate(1024 * 1024 + 1)
        with self.assertRaisesRegex(EntryError, "runtime_tool_size_rejected"):
            runtime.tool_digest(tool)

    def test_old_negative_requires_genuine_tool_missing_definition_and_unchanged_apks(self):
        receipt = {"artifactPath": str(self.app), "testArtifactPath": str(self.test),
            "artifactSha256": driver.artifact_hash(self.app), "testArtifactSha256": driver.artifact_hash(self.test)}
        def absent(argv, output, alias, **kwargs):
            for suffix, raw in ((".stdout", b""), (".stderr", ("ERROR: The given class (" + argv[-2] + ") not found\n").encode()),
                    (".json", frozen_json({"status": "failed"}))):
                path = output / (alias + suffix)
                path.write_bytes(raw)
                path.chmod(0o600)
            return {"status": "failed"}
        with patch.object(driver, "run_bounded", side_effect=absent):
            self.assertEqual(runtime.old_negative(receipt, "/explicit/tool", self.output, time.monotonic() + 5)["status"],
                "missing_runtime_refused")
        for status in ("ok", "timeout", "output_limit", "missing_executable"):
            with patch.object(driver, "run_bounded", return_value={"status": status}), \
                    self.subTest(status=status), self.assertRaisesRegex(EntryError, "runtime_negative_unverified"):
                runtime.old_negative(receipt, "/explicit/tool", self.output, time.monotonic() + 5)


if __name__ == "__main__":
    unittest.main()
