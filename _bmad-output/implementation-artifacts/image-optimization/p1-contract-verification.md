# P1 Contract Review Patch Verification

Date: 2026-10-16. Scope: documentation/design checks only; no application behavior was executed. Runtime implementation and all production profile/normalization/access/cache/retention/rollout gates remain pending.

## Reviewed corrections

| Finding | Documentation correction | Design coverage |
| --- | --- | --- |
| Full token equality alone could admit a contradictory terminal callback from the same still-valid attempt. | Require locked `PROCESSING` state before either terminal transition; first committed outcome wins. Identical replay is read-only proof, contradictory replay cannot mutate terminal state/immutable READY, and explicit retry claims a new token. Committed READY survives later lease expiry. | D17 covers success then failure, failure then success, identical replay, retry and expiry/replacement; D03 retains stale-attempt coverage. |
| Equivalent JSON escape spellings could produce different fingerprint bytes. | Decode before canonicalization; define literal printable ASCII, mandatory quote/backslash escapes, literal slash, five short control escapes and lowercase six-character escapes for all remaining ASCII controls/DEL. | Two equivalent-input fixtures, all 128 ASCII code points in names/values, duplicate decoded-name/float/non-ASCII refusal checks. |

The approved P1 frozen block is unchanged (SHA-256 `619e560e218d060cb8bd6d41af58a24d5b046a3dece7730d5ce2ba9223cb4db8`). Existing six structured and two source-byte golden hashes remain unchanged; the additional equivalent-escape pair shares one new digest. Method A uses Python JSON serialization and `hashlib`; method B independently implements the documented ASCII escaping/ordering and hashes with system `shasum -a 256`.

## Changed delivery files

- [Image revision/readiness contract](../../../be/docs/image-revision-readiness-contract.md)
- [ADR-0021](../../../be/docs/adr/ADR-0021-image-revision-readiness-contract.md)
- [Canonical architecture](../../../architecture.md)
- [Project memory](../../../be/docs/project-memory.md)
- [Parent SPEC](../../specs/spec-publish-time-image-optimization/SPEC.md)
- [Parent implementation plan](../../specs/spec-publish-time-image-optimization/implementation-plan.md)
- This verification report, retaining the program and raw check output below.

All six approved edge-matrix rows remain covered by D02-D07; D01 and D08-D17 cover automatic publication, reuse/growth/profile, retained renewal, language isolation, restart/conflict, superseded policy, mutable page lifetime, unsupported semantics and same-attempt terminal replay. These are written Given/When/Then walkthroughs and source/design consistency checks, not runtime state-machine or storage tests.

The only known missing local target is the pre-existing `architecture.md` link to `.kiro/specs/tellpal-v2-backend/tasks.md`; no new target is missing. The main BMad workflow separately records formal review outcomes. No Maven, converter, cloud/storage, device, dependency, migration or application command is run here.

## Reproduction command

Run from any directory with the repository at the recorded absolute path. This command executes the retained check program directly from this report; no temporary original is required.

```bash
python3 - <<'CHECK'
from pathlib import Path
report = Path('/Users/gurhankucuk/Documents/GitHub/tellpalv2/_bmad-output/implementation-artifacts/image-optimization/p1-contract-verification.md').read_text()
program = report.split('```python\n', 1)[1].split('\n```', 1)[0]
exec(compile(program, 'p1-contract-verification', 'exec'))
CHECK
```

## Retained check program

```python
import hashlib
import json
import re
import subprocess
from pathlib import Path

ROOT = Path('/Users/gurhankucuk/Documents/GitHub/tellpalv2')
CONTRACT = ROOT / 'be/docs/image-revision-readiness-contract.md'
SPEC = ROOT / '_bmad-output/implementation-artifacts/spec-p1-image-revision-readiness-contract.md'
FILES = [
    CONTRACT,
    ROOT / 'be/docs/adr/ADR-0021-image-revision-readiness-contract.md',
    ROOT / 'architecture.md',
    ROOT / 'be/docs/project-memory.md',
    ROOT / '_bmad-output/specs/spec-publish-time-image-optimization/SPEC.md',
    ROOT / '_bmad-output/specs/spec-publish-time-image-optimization/implementation-plan.md',
    ROOT / '_bmad-output/implementation-artifacts/image-optimization/p1-contract-verification.md',
]


def unique_members(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('duplicate decoded member')
        result[key] = value
    return result


def reject_number(value):
    raise ValueError('float/non-finite value')


def parse(source):
    return json.loads(source, object_pairs_hook=unique_members,
                      parse_float=reject_number, parse_constant=reject_number)


def explicit_ascii_string(value):
    short = {8: r'\b', 9: r'\t', 10: r'\n', 12: r'\f', 13: r'\r'}
    result = ['"']
    for char in value:
        code = ord(char)
        if code > 127:
            raise ValueError('non-ASCII string')
        if code == 34:
            result.append(r'\"')
        elif code == 92:
            result.append(r'\\')
        elif code in short:
            result.append(short[code])
        elif code < 32 or code == 127:
            result.append('\\u%04x' % code)
        else:
            result.append(char)
    result.append('"')
    return ''.join(result)


def explicit_canonical(value):
    if isinstance(value, str):
        return explicit_ascii_string(value)
    if value is None:
        return 'null'
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, int):
        return str(value)
    if isinstance(value, list):
        return '[' + ','.join(explicit_canonical(x) for x in value) + ']'
    if isinstance(value, dict):
        return '{' + ','.join(explicit_ascii_string(k) + ':' + explicit_canonical(value[k])
                              for k in sorted(value)) + '}'
    raise ValueError('unsupported type')


def canonical_methods(value):
    method_a = json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=True)
    method_b = explicit_canonical(value)
    assert method_a == method_b
    return method_a.encode('ascii'), method_b.encode('ascii')


def shasum(raw):
    result = subprocess.run(['/usr/bin/shasum', '-a', '256'], input=raw,
                            capture_output=True, check=True)
    return result.stdout.decode().split()[0]


def compare_hash(name, domain, value, expected):
    a, b = canonical_methods(value)
    framing = domain.encode('ascii') + b'\n'
    digest_a = hashlib.sha256(framing + a).hexdigest()
    digest_b = shasum(framing + b)
    assert digest_a == digest_b == expected and expected in document
    print(name + ': ' + expected + ' MATCH (Python JSON/hashlib; explicit serializer/shasum)')
    return digest_a


document = CONTRACT.read_text()
source_a = hashlib.sha256(b'abc').hexdigest()
source_b = hashlib.sha256(b'abd').hexdigest()
for raw, expected in [(b'abc', source_a), (b'abd', source_b)]:
    assert shasum(raw) == expected and expected in document
    print('source ' + raw.decode() + ': ' + expected + ' MATCH')
profile_a = compare_hash('profile A', 'tellpal.profile.fixture.v1',
                         {'fixtureSetting': 'A', 'schema': 1},
                         '109be3ad7fffcd6e1a5aadd356ff20ee43fb10451b8cc72b5afe891e513bc6b1')
profile_b = compare_hash('profile B', 'tellpal.profile.fixture.v1',
                         {'fixtureSetting': 'B', 'schema': 1},
                         '7ee75c51c36ccf81b746d265721125d1cf6b16c93817ee5463e26ce8aedc5a74')


def rendition(source, profile):
    return {'profileSha256': profile, 'role': 'READING_PAGE', 'schema': 1,
            'sourceSha256': source}


rendition_a = compare_hash('rendition A', 'tellpal.rendition.fixture.v1', rendition(source_a, profile_a),
                           'c7605439b944114472f45b18e37c386c91c5e21047fc099114edf1ac9c58ffd6')
compare_hash('changed source', 'tellpal.rendition.fixture.v1', rendition(source_b, profile_a),
             'f617e4dfe5639dbcb8d7a9acda9dd18bdcaedfa3fd979eab84292950410f0f17')
compare_hash('changed profile', 'tellpal.rendition.fixture.v1', rendition(source_a, profile_b),
             'bf8ec629c8d885530a2e78b83f8ff8b8814ed343b93ccb0d1ef2e34af9110daa')
revision = {'contentId': 7, 'experience': 'READING', 'language': 'tr',
            'pages': [{'pageId': 11, 'pageNumber': 1, 'renditionKey': rendition_a}],
            'schema': 1, 'type': 'STORY'}
compare_hash('revision', 'tellpal.revision.fixture.v1', revision,
             'feade7c9ee646245e2e95669ab3fd2c672ca2bedd4cf1ca678fe5d7605e7e81e')
for match in re.finditer(r'^canonical(?: changed profile)? = (.+)$', document, re.M):
    value = parse(match.group(1))
    assert canonical_methods(value)[0].decode() == match.group(1)
inputs = [re.search(r'^input ' + label + r' = (.+)$', document, re.M).group(1) for label in 'AB']
assert inputs[0] != inputs[1] and parse(inputs[0]) == parse(inputs[1])
for label, raw in zip('AB', inputs):
    canonical = canonical_methods(parse(raw))[0].decode()
    recorded = re.search(r'^canonical ' + label + r' = (.+)$', document, re.M).group(1)
    assert canonical == recorded
    compare_hash('equivalent escapes ' + label, 'tellpal.serialization.fixture.v1', parse(raw),
                 '1344b5810263686899aad5407311faa026d5da31ebf32a9eea34f65a32884234')
for code in range(128):
    canonical_methods({chr(code): chr(code)})
print('All 128 ASCII code points in member names and values: identical canonical bytes')
invalid = [r'{"x":1,"\u0078":2}', '{"x":1.0}', r'{"x":"\u0080"}']
for raw in invalid:
    try:
        explicit_canonical(parse(raw))
    except ValueError:
        continue
    raise AssertionError('invalid canonical input admitted')
print('Duplicate decoded names, floats and non-ASCII strings: rejected')
assert re.findall(r'^\| (D\d\d) ', document, re.M) == [f'D{i:02}' for i in range(1, 18)]
assert '**`PROCESSING`**' in document and '**`PROCESSING` precondition**' in document
for rule in ['first committed terminal outcome wins', '`COMPLETED` cannot become `FAILED`',
             '`FAILED` cannot become `COMPLETED`/READY', 'read-only idempotent proof',
             'new captured attempt/token', 'Lease expiry after a valid committed completion does not erase READY']:
    assert rule in document
print('D01-D17 design coverage and terminal-state/replay/retry/expiry rules: present (not runtime tests)')
frozen = re.search(rb'<frozen-after-approval\b.*?</frozen-after-approval>', SPEC.read_bytes(), re.S).group()
expected_frozen = '619e560e218d060cb8bd6d41af58a24d5b046a3dece7730d5ce2ba9223cb4db8'
assert hashlib.sha256(frozen).hexdigest() == shasum(frozen) == expected_frozen
print('Approved frozen block unchanged: ' + expected_frozen)
known_missing = ('architecture.md', './.kiro/specs/tellpal-v2-backend/tasks.md')
links = 0
for path in FILES:
    content = path.read_text()
    assert content.endswith('\n')
    assert all(line.rstrip() == line for line in content.splitlines())
    prose = re.sub(r'```.*?```', '', content, flags=re.S)
    for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)', prose):
        if '://' in target or target.startswith('#'):
            continue
        target = target.split('#')[0]
        links += 1
        assert (path.parent / target).exists() or (str(path.relative_to(ROOT)), target) == known_missing
print('Seven delivery documents: whitespace passed; ' + str(links) + ' local links checked')
print('Only known pre-existing missing link: architecture.md -> .kiro/specs/tellpal-v2-backend/tasks.md')
anchors = [
    ('asset/application/LibraryCoverPreparationService.java', 25, 'Preparation prepare'),
    ('asset/application/LibraryCoverPreparationService.java', 34, 'void publish'),
    ('asset/application/AssetProcessingService.java', 153, 'AssetProcessingRecord complete'),
    ('asset/application/AssetProcessingService.java', 167, 'AssetProcessingRecord fail'),
    ('asset/application/AssetProcessingService.java', 225, 'AssetProcessingRecord scheduleListingCover'),
    ('asset/application/AssetProcessingService.java', 263, 'AssetProcessingStatus.PROCESSING'),
    ('asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java', 172, 'StoryPageMediaManifest manifest'),
    ('asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java', 177, 'assetProcessingApi.complete'),
    ('content/application/ContentManagementService.java', 135, 'createLocalization'),
    ('content/application/ContentManagementService.java', 163, 'updateLocalization'),
    ('content/application/ContentManagementService.java', 190, 'markLocalizationProcessingStatus'),
    ('content/application/ContentManagementService.java', 206, 'markAsReady'),
    ('content/application/StoryPageManagementService.java', 143, 'schedulePageMedia'),
    ('content/application/StoryPageManagementService.java', 153, 'orderedSources.isEmpty'),
    ('content/application/ContentPublicationService.java', 42, 'publishLocalization'),
    ('content/application/AssetProcessingStatusListener.java', 19, 'EventListener'),
    ('user/web/mobile/TestStoryPageMediaController.java', 73, 'extractBearerToken'),
]
for name, line, needle in anchors:
    path = ROOT / 'be/src/main/java/com/tellpal/v2' / name
    assert needle in path.read_text().splitlines()[line - 1]
print('17 precise Java source anchors: passed')
subprocess.run(['git', 'diff', '--check'], cwd=ROOT, check=True)
print('git diff --check: passed')
print('No application, Maven, converter, cloud or device execution; all checks are documentation/design checks')
```

## Raw command/check output

```text
source abc: ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad MATCH
source abd: a52d159f262b2c6ddb724a61840befc36eb30c88877a4030b65cbe86298449c9 MATCH
profile A: 109be3ad7fffcd6e1a5aadd356ff20ee43fb10451b8cc72b5afe891e513bc6b1 MATCH (Python JSON/hashlib; explicit serializer/shasum)
profile B: 7ee75c51c36ccf81b746d265721125d1cf6b16c93817ee5463e26ce8aedc5a74 MATCH (Python JSON/hashlib; explicit serializer/shasum)
rendition A: c7605439b944114472f45b18e37c386c91c5e21047fc099114edf1ac9c58ffd6 MATCH (Python JSON/hashlib; explicit serializer/shasum)
changed source: f617e4dfe5639dbcb8d7a9acda9dd18bdcaedfa3fd979eab84292950410f0f17 MATCH (Python JSON/hashlib; explicit serializer/shasum)
changed profile: bf8ec629c8d885530a2e78b83f8ff8b8814ed343b93ccb0d1ef2e34af9110daa MATCH (Python JSON/hashlib; explicit serializer/shasum)
revision: feade7c9ee646245e2e95669ab3fd2c672ca2bedd4cf1ca678fe5d7605e7e81e MATCH (Python JSON/hashlib; explicit serializer/shasum)
equivalent escapes A: 1344b5810263686899aad5407311faa026d5da31ebf32a9eea34f65a32884234 MATCH (Python JSON/hashlib; explicit serializer/shasum)
equivalent escapes B: 1344b5810263686899aad5407311faa026d5da31ebf32a9eea34f65a32884234 MATCH (Python JSON/hashlib; explicit serializer/shasum)
All 128 ASCII code points in member names and values: identical canonical bytes
Duplicate decoded names, floats and non-ASCII strings: rejected
D01-D17 design coverage and terminal-state/replay/retry/expiry rules: present (not runtime tests)
Approved frozen block unchanged: 619e560e218d060cb8bd6d41af58a24d5b046a3dece7730d5ce2ba9223cb4db8
Seven delivery documents: whitespace passed; 40 local links checked
Only known pre-existing missing link: architecture.md -> .kiro/specs/tellpal-v2-backend/tasks.md
17 precise Java source anchors: passed
git diff --check: passed
No application, Maven, converter, cloud or device execution; all checks are documentation/design checks
```

## Formal workflow review

All three context-free reviewers were launched together and all returned before triage. The reviewed tracked/untracked diff since baseline `124a5b22fd324793a5d14220715c839617b6848b` contained79,786bytes, SHA-256 `487a59a6a1e7d13fa7afa1d727a13b6e1e449814b2ca8cee7e947b90b67ee7b0`. Blind review returned “No actionable findings.” Verification-gap review returned “No verification gaps found.” Edge review returned the two findings above.

| Finding | Severity | Disposition | Evidence after correction |
| --- | --- | --- | --- |
| Same-attempt contradictory callback after first terminal commit | High | patch | Locked PROCESSING precondition and first-terminal-wins rule; D17 covers both callback orders, identical replay, retry and expiry. |
| Equivalent JSON string escape spellings yield different fingerprints | Medium | patch | Exact ASCII escape grammar, equivalent-input fixture, independent serializers/hash tools and all128ASCII code points. |

Both patches make the already-approved terminal ownership/idempotency and canonical-identity intent precise; neither introduces a new product decision or changes the frozen scope. Root re-executed the retained program after both corrections and matched the retained raw output byte-for-byte. No runtime test or production guarantee is inferred. There are no unresolved findings introduced by P1.

The pre-existing architecture tasks-link issue is deferred separately. The original P0 raw-evidence limitation and all downstream profile/access/cache/retention/rollout gates remain explicit.
