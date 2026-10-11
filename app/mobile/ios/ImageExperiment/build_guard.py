#!/usr/bin/env python3
"""Fail closed before any Flutter or signing work; emit only a fixed refusal code."""
import os
from pathlib import Path
import stat
import sys


def checked_path(value, *, directory, mode):
    path = Path(value)
    if not path.is_absolute() or path.resolve(strict=True) != path:
        raise ValueError()
    info = path.lstat()
    if stat.S_IMODE(info.st_mode) != mode:
        raise ValueError()
    if directory and not stat.S_ISDIR(info.st_mode):
        raise ValueError()
    if not directory and not stat.S_ISREG(info.st_mode):
        raise ValueError()
    return path


def main():
    env = os.environ
    source = Path(env["SRCROOT"]).resolve()
    repository = source.parents[2]
    target = env.get("TARGET_NAME", "")
    entry = env.get("FLUTTER_TARGET", "lib/main.dart")
    if target == "Runner":
        generated = source / "Flutter" / "Generated.xcconfig"
        if entry.endswith("main_image_experiment.dart") or (
            generated.exists() and "main_image_experiment.dart" in generated.read_text(encoding="utf-8")
        ):
            raise ValueError()
        return
    if target not in {"ImageExperiment", "ImageExperimentUITests"}:
        raise ValueError()
    config = env.get("CONFIGURATION", "")
    mode = {"Profile-imageexperiment": "profile", "Release-imageexperiment": "release"}.get(config)
    if mode is None or entry != "lib/main_image_experiment.dart" or env.get("FLUTTER_BUILD_MODE") != mode:
        raise ValueError()
    output = checked_path(env["IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT"], directory=True, mode=0o700)
    signing = checked_path(env["IMAGE_EXPERIMENT_SIGNING_CONFIG"], directory=False, mode=0o600)
    if output.is_relative_to(repository) or signing.is_relative_to(repository) or signing.is_relative_to(output):
        raise ValueError()
    for field in ("BUILD_DIR", "PROJECT_TEMP_DIR"):
        if not Path(env[field]).resolve().is_relative_to(output):
            raise ValueError()
    flutter_output = (source.parent / env["FLUTTER_BUILD_DIR"]).resolve()
    if not flutter_output.is_relative_to(output):
        raise ValueError()
    if not env.get("FLUTTER_ROOT") or env.get("CODE_SIGN_STYLE") != "Manual" or not env.get("DEVELOPMENT_TEAM"):
        raise ValueError()
    if not env.get("CODE_SIGN_IDENTITY") or not env.get("PROVISIONING_PROFILE_SPECIFIER"):
        raise ValueError()
    if env.get("PLATFORM_NAME") != "iphoneos" or env.get("CODE_SIGNING_ALLOWED") == "NO":
        raise ValueError()
    prefix = "com.tellpal.tellpalMobilePoc.imageexperiment."
    expected = prefix + mode + (".uitests" if target == "ImageExperimentUITests" else "")
    if env.get("PRODUCT_BUNDLE_IDENTIFIER") != expected:
        raise ValueError()


if __name__ == "__main__":
    try:
        main()
    except Exception:
        print("EXPERIMENT_BUILD_GUARD_REFUSED", file=sys.stderr)
        sys.exit(1)
