"""Private terminal evidence never substitutes for an unchanged native outcome."""
import base64
from hashlib import sha256
from pathlib import Path
import tempfile
import threading
from types import SimpleNamespace
import unittest

from native_artwork_comparison import LedgerChunks
from native_entry_config import Manifest, frozen_json, strict_json
from reader_terminal_observation import ReaderTerminalObservation


def ledger_chunks(data):
    chunks = LedgerChunks()
    parts = [data[index:index + 3072] for index in range(0, len(data), 3072)]
    digest = sha256(data).hexdigest().encode()
    for index, part in enumerate(parts):
        for field, value in ((b'event', b'ledger-chunk'), (b'index', str(index).encode()),
                (b'count', str(len(parts)).encode()), (b'ledgerSha256', digest),
                (b'payloadBase64', base64.b64encode(part))):
            chunks.feed(b'INSTRUMENTATION_STATUS: ' + field + b'=' + value)
        chunks.feed(b'INSTRUMENTATION_STATUS_CODE: 2')
    return chunks


class TerminalObservationTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix='reader-terminal-test-')
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name).resolve(); self.root.chmod(0o700)
        self.manifest = Manifest((), {}, 'a' * 64, b'{}')
        self.observer = ReaderTerminalObservation(self.root, self.manifest,
            {'artifactSha256': 'b' * 64, 'testArtifactSha256': 'c' * 64}, 'd' * 64)

    def test_terminal_snapshot_is_finite_hashes_counts_and_presence_only(self):
        self.observer.request()
        self.observer.response({'status': 'revoked', 'reason': 'token_verification_failed',
            'token': 'NEVER_RECORD_TOKEN'})
        broker = SimpleNamespace(lock=threading.Lock(), fixture_deliveries={
            'reader_page_1': {'count': 1, 'bodyBytes': 20, 'outputSha256': 'e' * 64},
            'NEVER_RECORD_CASE': {'count': 1, 'bodyBytes': 20, 'outputSha256': 'e' * 64}},
            session_id='NEVER_RECORD_SESSION', reason='token_verification_failed',
            token='NEVER_RECORD_TOKEN', explicit_app_revoked=False, admission_successful=True,
            fixture_delivery_count=1)
        self.observer.before_close(broker)
        self.observer.before_close(broker)
        data = (self.root / 'broker-terminal-observation.json').read_bytes()
        receipt = strict_json(data)
        self.assertNotIn(b'NEVER_RECORD', data)
        self.assertTrue(receipt['requestObserved']); self.assertEqual(receipt['requestCount'], 1)
        self.assertEqual(receipt['responseReason'], 'token_verification_failed')
        self.assertTrue(receipt['tokenPresentBeforeClose'])
        self.assertFalse(receipt['explicitAppRevokedObserved'])
        self.assertEqual(len(receipt['fixtureDeliveries']), 1)
        self.assertEqual(receipt['nativeSettlement'], 'not_asserted')
        self.assertEqual(receipt['entryQualification'], 'not_asserted')
        self.assertEqual((self.root / 'broker-terminal-observation.json').stat().st_mode & 0o777, 0o600)

    def test_unknown_or_raw_status_reason_does_not_escape_finite_projection(self):
        self.observer.request(); self.observer.exception()
        self.observer.response({'status': 'NEVER_RECORD', 'reason': 'NEVER_RECORD'})
        broker = SimpleNamespace(lock=threading.Lock(), fixture_deliveries={},
            session_id='session', reason='NEVER_RECORD', token=None,
            explicit_app_revoked='NEVER_RECORD', admission_successful='NEVER_RECORD',
            fixture_delivery_count=None)
        self.observer.before_close(broker)
        data = (self.root / 'broker-terminal-observation.json').read_bytes()
        receipt = strict_json(data)
        self.assertNotIn(b'NEVER_RECORD', data)
        self.assertEqual(receipt['responseStatus'], 'unavailable')
        self.assertEqual(receipt['responseReason'], 'unavailable')
        self.assertTrue(receipt['requestRaised'])
        self.assertIsNone(receipt['fixtureDeliveryCount'])
        self.assertIsNone(receipt['explicitAppRevokedObserved'])

    def test_complete_digest_verified_failure_ledger_stays_unqualified(self):
        data = frozen_json({'schemaVersion': 1, 'records': [{'event': 'failure'}], 'droppedCount': 0})
        chunks = ledger_chunks(data)
        self.observer.failed_ledger(SimpleNamespace(chunks=chunks), {'sessionSha256': 'f' * 64})
        self.assertEqual((self.root / 'failure-device-ledger.json').read_bytes(), data)
        receipt = strict_json((self.root / 'failure-ledger-observation.json').read_bytes())
        self.assertEqual(receipt['ledgerSha256'], sha256(data).hexdigest())
        self.assertEqual(receipt['ledgerBytes'], len(data))
        self.assertEqual(receipt['status'], 'observed_unqualified')
        self.assertEqual(receipt['nativeSettlement'], 'not_promoted')
        self.assertEqual(receipt['performanceQualification'], 'unavailable')
        self.assertFalse((self.root / 'reader-result.json').exists())

    def test_incomplete_or_bad_digest_is_unavailable_without_zero_or_native_success(self):
        for chunks in (LedgerChunks(), ledger_chunks(b'complete')):
            if chunks.parts: chunks.parts[0] = b'changed'
            folder = self.root / ('missing' if not chunks.parts else 'corrupt'); folder.mkdir(mode=0o700)
            observer = ReaderTerminalObservation(folder, self.manifest, {}, 'd' * 64)
            observer.failed_ledger(SimpleNamespace(chunks=chunks), None)
            receipt = strict_json((folder / 'failure-ledger-observation.json').read_bytes())
            self.assertEqual(receipt['status'], 'unavailable')
            self.assertIsNone(receipt['ledgerBytes']); self.assertIsNone(receipt['ledgerSha256'])
            self.assertFalse((folder / 'failure-device-ledger.json').exists())


if __name__ == '__main__':
    unittest.main()
