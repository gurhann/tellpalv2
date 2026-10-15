#!/usr/bin/env python3
"""Resolve external tests with installed mobile packages; never run pub or edit mobile source."""
import json
from pathlib import Path
import shutil
import subprocess
from urllib.parse import urljoin


def run():
    here = Path(__file__).resolve().parent
    mobile = here.parents[2] / 'app/mobile'
    original = mobile / '.dart_tool/package_config.json'
    packages = json.loads(original.read_text())
    for package in packages['packages']:
        package['rootUri'] = urljoin(original.as_uri(), package['rootUri'])
    external = here / '.dart_tool'
    external.mkdir()  # Refuse an existing config instead of replacing it.
    try:
        (external / 'package_config.json').write_text(json.dumps(packages))
        flutter = shutil.which('flutter')
        if flutter is None:
            raise RuntimeError('installed Flutter SDK unavailable')
        test = str(here / 'require_absent_keys_test.dart')
        for arguments in (['test', '--no-pub', '--no-test-assets', '--reporter', 'expanded', test],
                ['analyze', '--no-pub', test]):
            subprocess.run([flutter, *arguments], cwd=mobile, check=True, timeout=60)
    finally:
        (external / 'package_config.json').unlink(missing_ok=True)
        external.rmdir()


if __name__ == '__main__':
    run()
