"""Yalnız sabit P4b5 kapsamı; yeni ölçüm açık kimlikle seçilir."""
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class QaScope:
    measurement_id: str
    proposal: str
    proposal_sha256: str
    private_root: Path
    entry: str

    def identity(self):
        return {'proposalSha256': self.proposal_sha256, 'measurementId': self.measurement_id,
                'budgetRunId': 'p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09'}

    def backend_arguments(self, state):
        prefix = [] if self is P4B5 else ['--scope', self.measurement_id]
        return [*prefix, '--execute', str(state)]


P4B5 = QaScope('p4b5-7981ad25-c851-4ed6-af89-2d0fbfffd0f8',
    'be/docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json',
    'e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833',
    Path('/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d'),
    'test_driver/reading_p4b5_qa_entry.dart')
P4B5_RETRY = QaScope('p4b5-retry-afde2e82-70d1-430a-9094-d4532d5cd798',
    'be/docs/evidence/p4b5-retry-measurement/bounded-run-proposal-2026-10-19.json',
    '06810424cfde11b22151aa4c719de448f5e4a1fa02d767c11765cac9627a2606',
    Path('/Volumes/MAC_EK/tellpal-qa-private/p4b5-retry-measurement-20261019-e11df87'),
    'test_driver/reading_p4b5_retry_qa_entry.dart')
P4B5_MANUAL = QaScope('p4b5-manual-afa6ba59-d0af-493b-b4c3-91b8024b94a4',
    'be/docs/evidence/p4b5-manual-consumer-measurement/bounded-run-proposal-2026-10-20.json',
    'f45fb1ac17bc8227af3fae5b284efd614409d590d57749fcff49e942a6e3ee92',
    Path('/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1'),
    'test_driver/reading_consumer_manual_entry.dart')
SCOPES = (P4B5, P4B5_RETRY, P4B5_MANUAL)


def select_scope(value=None):
    if value is None:
        return P4B5
    for candidate in SCOPES:
        if value == candidate.measurement_id:
            return candidate
    raise ValueError('UNKNOWN_QA_SCOPE')
