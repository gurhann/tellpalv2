#!/usr/bin/env python3
"""Compile the unchanged production Kotlin object with offline configurable OS stubs."""
from hashlib import sha256
from pathlib import Path
import os
import subprocess
import tempfile


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SOURCE = ROOT / 'app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentLedger.kt'


def cached_jar(group, artifact, version):
    directory = Path.home() / '.gradle/caches/modules-2/files-2.1' / group / artifact / version
    jars = list(directory.glob(f'*/{artifact}-{version}.jar'))
    if len(jars) != 1:
        raise RuntimeError(f'required cached compiler dependency unavailable: {artifact}/{version}')
    return jars[0]


def run():
    source = SOURCE.read_bytes()
    text = source.decode('utf-8')
    start = text.index('object ImageExperimentMemory {')
    end = text.index('\n/** A single native hold lease.', start)
    # Exact source slice, including all production reader/exception/conversion code.
    actual_object = text[start:end]
    if not actual_object.rstrip().endswith('}') or actual_object.count('{') != actual_object.count('}'):
        raise RuntimeError('production object boundary changed; review extraction')
    version = '2.0.21'
    kotlin = ['kotlin-compiler-embeddable', 'kotlin-stdlib', 'kotlin-script-runtime', 'kotlin-reflect']
    jars = [cached_jar('org.jetbrains.kotlin', artifact, version) for artifact in kotlin]
    jars += [cached_jar('org.jetbrains.intellij.deps', 'trove4j', '1.0.20200330'),
        cached_jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.6.4'),
        cached_jar('org.jetbrains', 'annotations', '13.0')]
    runtimes = [Path('/Applications') / app / 'Contents/jbr/Contents/Home/bin/java'
        for app in ('Android Studio.app', 'DataGrip.app', 'IntelliJ IDEA.app')]
    java = next((candidate for candidate in runtimes if candidate.is_file()), None)
    if java is None:
        raise RuntimeError('installed JetBrains Java runtime unavailable')
    with tempfile.TemporaryDirectory(prefix='native-memory-test-') as temporary:
        base = Path(temporary)
        extracted = base / 'ImageExperimentMemory.kt'
        extracted.write_text('package com.tellpal.imageexperiment\n\n' + actual_object)
        classes = base / 'classes'
        classpath = os.pathsep.join(map(str, jars))
        compile_command = [str(java), '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
            '-no-stdlib', '-no-reflect', '-classpath', classpath, '-d', str(classes),
            str(extracted), str(HERE / 'AndroidOsStubs.kt'), str(HERE / 'MemorySampleTest.kt')]
        compiled = subprocess.run(compile_command, capture_output=True, text=True, timeout=60)
        if compiled.returncode:
            raise RuntimeError('Kotlin compilation failed:\n' + compiled.stdout + compiled.stderr)
        executed = subprocess.run([str(java), '-cp', str(classes) + os.pathsep + classpath,
            'com.tellpal.imageexperiment.MemorySampleTestKt'], capture_output=True, text=True, timeout=15)
        if executed.returncode:
            raise RuntimeError('Kotlin assertions failed:\n' + executed.stdout + executed.stderr)
        if SOURCE.read_bytes() != source:
            raise RuntimeError('production source changed during test')
        return executed.stdout.strip() + '\nproductionSourceSha256=' + sha256(source).hexdigest()


if __name__ == '__main__':
    print(run())
