"""Passive private reader teardown evidence; never entry or metric qualification."""
from hashlib import sha256
from pathlib import Path
import threading

from native_entry_config import HASH, READER_CASES, write_private


_STATUSES = frozenset(('admitted', 'revoked', 'expired'))
_REASONS = frozenset(('session_active', 'app_revoked', 'host_revoked', 'session_expired',
    'token_verification_failed', 'admission_failed', 'bootstrap_delivery_failed'))


def _hash(value):
    return value if type(value) is str and HASH.fullmatch(value) else None


def _count(value):
    return value if type(value) is int and 0 <= value <= 2 ** 53 - 1 else None


def _finite(value, allowed):
    return value if type(value) is str and value in allowed else 'unavailable'


def _boolean(value):
    return value if type(value) is bool else None


class ReaderTerminalObservation:
    def __init__(self, output, manifest, build, source_sha256):
        self.output = Path(output)
        self.manifest = manifest
        self.identity = {'sourceSha256': _hash(source_sha256),
            'artifactSha256': _hash(build.get('artifactSha256')),
            'testArtifactSha256': _hash(build.get('testArtifactSha256')),
            'privateManifestSha256': manifest.private_sha256,
            'publicManifestSha256': manifest.public_sha256}
        self.lock = threading.Lock()
        self.requests = 0
        self.returned = False
        self.raised = False
        self.status = self.reason = 'unavailable'
        self.saved = False

    def request(self):
        with self.lock:
            self.requests += 1

    def response(self, value):
        with self.lock:
            self.returned = True
            self.status = _finite(value.get('status') if type(value) is dict else None, _STATUSES)
            self.reason = _finite(value.get('reason') if type(value) is dict else None, _REASONS)

    def exception(self):
        with self.lock:
            self.raised = True

    def before_close(self, broker):
        """Caller contains observer errors and always invokes the original close."""
        with broker.lock, self.lock:
            if self.saved:
                return
            self.saved = True
            deliveries = []
            for case in READER_CASES:
                value = broker.fixture_deliveries.get(case)
                if type(value) is dict:
                    deliveries.append({'caseSha256': sha256(case.encode()).hexdigest(),
                        'count': _count(value.get('count')), 'bodyBytes': _count(value.get('bodyBytes')),
                        'outputSha256': _hash(value.get('outputSha256'))})
            receipt = {'schemaVersion': 1, 'scope': 'reader-broker-terminal-observation-r1',
                'status': 'observed', **self.identity,
                'sessionSha256': sha256(broker.session_id.encode()).hexdigest(),
                'observedBeforeBrokerClose': True, 'requestObserved': self.requests > 0,
                'requestCount': self.requests, 'responseReturned': self.returned,
                'requestRaised': self.raised, 'responseStatus': self.status, 'responseReason': self.reason,
                'brokerReasonBeforeClose': _finite(broker.reason, _REASONS),
                'brokerReasonAbsentBeforeClose': broker.reason is None,
                'tokenPresentBeforeClose': broker.token is not None,
                'explicitAppRevokedObserved': _boolean(broker.explicit_app_revoked),
                'hostAdmissionSuccessfulObserved': _boolean(broker.admission_successful),
                'fixtureDeliveryCount': _count(broker.fixture_delivery_count),
                'fixtureDeliveries': deliveries, 'entryQualification': 'not_asserted',
                'nativeSettlement': 'not_asserted'}
        write_private(self.output / 'broker-terminal-observation.json', receipt)

    def failed_ledger(self, pairing, binding):
        """A complete chunk digest proves bytes only; a failed native run stays failed."""
        receipt = {'schemaVersion': 1, 'scope': 'reader-failed-ledger-observation-r1',
            **self.identity, 'sessionSha256': _hash((binding or {}).get('sessionSha256')),
            'status': 'unavailable', 'reason': 'incomplete_or_invalid_chunks',
            'ledgerSha256': None, 'ledgerBytes': None, 'entryQualification': 'unavailable',
            'nativeSettlement': 'not_promoted', 'performanceQualification': 'unavailable',
            'payloadSessionBinding': 'not_qualified'}
        try:
            data = pairing.chunks.finish()
            write_private(self.output / 'failure-device-ledger.json', data)
            receipt.update(status='observed_unqualified', reason='complete_digest_verified_failure_evidence',
                ledgerSha256=sha256(data).hexdigest(), ledgerBytes=len(data))
        except Exception:
            pass
        write_private(self.output / 'failure-ledger-observation.json', receipt)
