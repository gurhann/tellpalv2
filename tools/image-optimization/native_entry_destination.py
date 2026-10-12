"""Private XCTest parameters that only launch exact already-installed experiment IDs."""
from hashlib import sha256
from pathlib import Path
import plistlib
import re

from native_entry_config import EntryError, no_symlink, private_file, require, write_private

AUDIT_ENV = "IMAGE_EXPERIMENT_INSTALLATION_AUDIT"
FIXTURE_ENV = "IMAGE_EXPERIMENT_METADATA"
AUDIT_TEST = "ImageExperimentBootstrapTests/testInstallationAudit"
FIXTURE_TEST = "ImageExperimentBootstrapTests/testPairedPublicKeyLaunch"
TEST_BUNDLE = "__TESTHOST__/PlugIns/ImageExperimentUITests.xctest"
MAX_RUN_BYTES = 256 * 1024
TEST_ENV_KEYS = frozenset(("DYLD_FRAMEWORK_PATH", "DYLD_LIBRARY_PATH", "DYLD_INSERT_LIBRARIES",
    "XCTestBundlePath", "XCTestConfigurationFilePath", "XCInjectBundleInto", "XCTestSessionIdentifier",
    "__XCODE_BUILT_PRODUCTS_DIR_PATHS", "XCODE_DBG_XPC_EXCLUSIONS", "OS_ACTIVITY_DT_MODE",
    "XCTestManagerVariant", "XCODE_SCHEME_NAME"))
RUN_ENV_KEYS = TEST_ENV_KEYS | frozenset(("APP_DISTRIBUTOR_ID_OVERRIDE", "IDE_CHOSEN_LOGGING_METHOD",
    "PERFC_ENABLE_EXTENDED_DIAGNOSTIC_FORMAT", "PERFC_ENABLE_PROFILE_MODE", "PERFC_RESET_INSERT_LIBRARIES",
    "PERFC_SUPPRESS_SYSTEM_REPORTS", "SQLITE_ENABLE_THREAD_ASSERTIONS", "TERM"))
APP_ENV_KEYS = frozenset(("APP_DISTRIBUTOR_ID_OVERRIDE", "XCODE_SCHEME_NAME"))


def _bounded_tree(value, depth=0):
    require(depth <= 8, "xctestrun_rejected")
    if type(value) is dict:
        require(len(value) <= 64, "xctestrun_rejected")
        for key, item in value.items():
            require(type(key) is str and len(key) <= 128 and
                not any(name in key for name in (FIXTURE_ENV, AUDIT_ENV)), "xctestrun_metadata_rejected")
            _bounded_tree(item, depth + 1)
    elif type(value) is list:
        require(len(value) <= 64, "xctestrun_rejected")
        for item in value:
            _bounded_tree(item, depth + 1)
    elif type(value) is str:
        require(len(value.encode("utf-8")) <= 8192 and "\x00" not in value and
            not any(name in value for name in (FIXTURE_ENV, AUDIT_ENV)), "xctestrun_metadata_rejected")
    else:
        require(type(value) in (bool, int, float), "xctestrun_rejected")


def _expanded_path(value, original, test_host=None):
    require(type(value) is str, "xctestrun_binding_rejected")
    value = value.replace("__TESTROOT__", str(original.parent))
    if test_host is not None:
        value = value.replace("__TESTHOST__", str(test_host))
    require("__" not in value, "xctestrun_binding_rejected")
    return no_symlink(value)


def destination_run(build, output, encoded, *, audit):
    """Preserve the original run file; discard install paths and unrelated test selection."""
    require(type(encoded) is str and 0 < len(encoded) <= (8192 if audit else 24000), "xctestrun_metadata_rejected")
    app_id, runner_id = build.get("expectedAppId"), build.get("testAppId")
    require(app_id in ("com.tellpal.tellpalMobilePoc.imageexperiment.profile",
        "com.tellpal.tellpalMobilePoc.imageexperiment.release") and
        runner_id == app_id + ".uitests.xctrunner", "app_identity_rejected")
    original = no_symlink(build.get("xctestrunPath"))
    data = private_file(original, MAX_RUN_BYTES)
    require(sha256(data).hexdigest() == build.get("xctestrunSha256"), "xctestrun_binding_rejected")
    try:
        source = plistlib.loads(data)
    except (ValueError, TypeError, plistlib.InvalidFileException, OverflowError):
        raise EntryError("xctestrun_rejected") from None
    _bounded_tree(source)
    require(type(source) is dict and set(source) == {"__xctestrun_metadata__", "ImageExperimentUITests"},
        "xctestrun_rejected")
    metadata = source["__xctestrun_metadata__"]
    require(type(metadata) is dict and set(metadata) in ({"FormatVersion"}, {"FormatVersion", "ContainerInfo"}) and
        type(metadata.get("FormatVersion")) is int and metadata["FormatVersion"] == 1, "xctestrun_rejected")
    if "ContainerInfo" in metadata:
        require(metadata["ContainerInfo"] == {"ContainerName": "Runner", "SchemeName": "imageexperiment"},
            "xctestrun_rejected")
    target = source["ImageExperimentUITests"]
    require(type(target) is dict and target.get("BlueprintName") == "ImageExperimentUITests" and
        target.get("IsUITestBundle") is True, "xctestrun_binding_rejected")
    app, runner = no_symlink(build["artifactPath"]), no_symlink(build["testArtifactPath"])
    require(_expanded_path(target.get("TestHostPath"), original) == runner and
        _expanded_path(target.get("UITargetAppPath"), original) == app and
        _expanded_path(target.get("TestBundlePath"), original, runner) == runner / "PlugIns/ImageExperimentUITests.xctest",
        "xctestrun_binding_rejected")
    def admitted_environment(name, keys, *, required=False):
        environment = target.get(name, {})
        require(type(environment) is dict and (not required or environment) and len(environment) <= 16 and
            set(environment) <= keys and all(type(key) is str and
                re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]{0,127}", key) and type(value) is str and
                len(value.encode("utf-8")) <= 8192 for key, value in environment.items()), "xctestrun_environment_rejected")
        return {key: value.replace("__TESTROOT__", str(original.parent)) for key, value in environment.items()}
    testing_environment = admitted_environment("TestingEnvironmentVariables", TEST_ENV_KEYS, required=True)
    run_environment = admitted_environment("EnvironmentVariables", RUN_ENV_KEYS)
    app_environment = admitted_environment("UITargetAppEnvironmentVariables", APP_ENV_KEYS)
    # __TESTROOT__ must keep its original meaning after placing the private run file elsewhere.
    selected = dict(target)
    for name in ("TestHostPath", "TestBundlePath", "UITargetAppPath", "DependentProductPaths",
        "EnvironmentVariables", "UITargetAppEnvironmentVariables", "CommandLineArguments",
        "UITargetAppCommandLineArguments", "OnlyTestIdentifiers", "SkipTestIdentifiers"):
        selected.pop(name, None)
    selected.update(UseDestinationArtifacts=True, TestHostBundleIdentifier=runner_id,
        UITargetAppBundleIdentifier=app_id, TestBundleDestinationRelativePath=TEST_BUNDLE,
        TestingEnvironmentVariables=testing_environment,
        EnvironmentVariables={**run_environment, (AUDIT_ENV if audit else FIXTURE_ENV): encoded},
        UITargetAppEnvironmentVariables=app_environment,
        OnlyTestIdentifiers=[AUDIT_TEST if audit else FIXTURE_TEST], ParallelizationEnabled=False,
        SystemAttachmentLifetime="keepNever", UserAttachmentLifetime="keepNever")
    run = {"ImageExperimentUITests": selected, "__xctestrun_metadata__": metadata}
    destination = Path(output) / ("installation-audit.xctestrun" if audit else "fixture-entry.xctestrun")
    write_private(destination, plistlib.dumps(run, fmt=plistlib.FMT_XML, sort_keys=True))
    require(private_file(original, MAX_RUN_BYTES) == data, "xctestrun_changed")
    return destination
