"""Synthetic reader protocol, direct source, geometry and conservative budget checks."""
from copy import deepcopy
from hashlib import sha256
from io import BytesIO
import os
from pathlib import Path
import struct
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile

from PIL import Image
import reader_resolution_experiment as reader
import native_entry_config as config
from native_entry_driver import validate_entry_outcome
from native_fixture_server import Broker
from test_native_entry_config import PrivateInputs
from test_native_entry_server import BrokerInputs


def geometry():
    return dict(zip(reader.GEOMETRY_FIELDS, (2.0, 3.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 'portrait')))


def budget():
    return {'observedAt': 1000, 'catalogObservedAt': 1000, 'usageObservedAt': 1000,
        'inventoryObservedAt': 1000, 'catalogSha256': '1' * 64, 'usageSha256': '2' * 64,
        'inventorySha256': '3' * 64, 'ownedReceiptsSha256': '4' * 64,
        'ownedReceiptsVerified': True, 'ownershipUnknown': False, 'cleanupUnknown': False,
        'activeOrPending': 0, 'oldUpperMinutesRetained': 40, 'originalClosedRootMinutes': 29,
        'originalReservationCount': 13, 'reportedMonthlyUsageMinutes': 42,
        'historicalMaximumQuotaMinutes': 45, 'minuteUsd': .15,
        'conversionTryPerUsd': reader.TRY_PER_USD, 'nonDeviceTry': reader.BASE_TRY,
        'nonDeviceCostVerified': True, 'nonDeviceReceiptSha256': '5' * 64,
        'noCostTierMinutes': 30, 'maximumNewWindows': 2, 'windows': []}


def prior_window(ordinal=1, seconds=99):
    return {'ordinal': ordinal, 'debitReceiptSha256': str(ordinal) * 64,
        'creationReceiptSha256': 'a' * 64, 'cleanupReceiptSha256': 'b' * 64,
        'status': 'finished', 'debitedMinutes': 6, 'wholeRootSeconds': seconds}


class ReaderSourceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='reader-source-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        os.chmod(self.root, 0o700)
        self.pages = tuple(reader.SourcePage(index, index, str(index) * 64, 'a' * 64,
            3, 5, bytes((index, 40, 80, 255)) * 15) for index in range(1, 7))

    def put(self, relative, data):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
        path.write_bytes(data)
        path.chmod(0o600)
        return path

    def synthetic_sources(self):
        records, references, archive = [], [], BytesIO()
        with zipfile.ZipFile(archive, 'w') as stream:
            for index in range(1, 19):
                image = Image.new('RGB', (3, 5), (index, 40, 80))
                encoded = BytesIO(); image.save(encoded, format='PNG')
                content = encoded.getvalue(); source = reader.digest(content)
                raw = reader.MAGIC + struct.pack('>II', 3, 5) + image.convert('RGBA').tobytes()
                self.put('sources/' + source + '.source', content)
                self.put('preparation-v7/normalized-references/' + source + '-rgba8-srgb-v1.pixels', raw)
                references.append({'id': source + '-rgba8-srgb-v1', 'source_hash': source,
                    'width': 3, 'height': 5, 'pixel_sha256': reader.digest(raw), 'bytes': len(raw)})
                records.append({'content_id': 71, 'asset_id': index, 'page_number': index,
                    'language': None, 'role': 'story_page_source', 'source_owner_role': 'story_page',
                    'sha256': source, 'metadata': {'width': 3, 'height': 5},
                    'cms_binding_verification': {'status': 'confirmed'}})
                stream.writestr(str(index) + '.png', content)
            zip_manifest = b'{"synthetic":"ordered"}'
            stream.writestr('manifest.json', zip_manifest)
        records.insert(0, {**records[1], 'role': 'story_cover_source', 'page_number': None})
        acquisition = {'content_id': 71, 'archive_sha256': reader.digest(archive.getvalue()),
            'manifest_sha256': reader.digest(zip_manifest), 'unique_entry_hashes': 18, 'records': records}
        preparation = {'schema': 'p0a-normalization-preparation-v3', 'source_failures': [],
            'checks': [{'result': 'pass'}] * 5, 'references': references}
        values = [archive.getvalue(), config.frozen_json(acquisition), config.frozen_json(preparation)]
        pins = {}
        for name, data in zip(reader.PINS, values):
            self.put(name, data); pins[name] = reader.digest(data)
        return pins

    def test_all18_order_source_zip_and_v7_are_bound_first6_distinct(self):
        pins = self.synthetic_sources()
        with patch.object(reader, 'PINS', pins), patch.object(reader, 'SOURCE_SIZE', (3, 5)):
            pages, admission = reader.admit_sources(self.root)
        self.assertEqual(tuple(page.page_number for page in pages), tuple(range(1, 7)))
        self.assertEqual(len({page.source_sha256 for page in pages}), 6)
        self.assertEqual(admission['pageCount'], 18)
        self.assertIsNone(admission['audio']); self.assertIsNone(admission['publication'])

    def test_changed_original_or_source_pin_refuses_without_writing(self):
        pins = self.synthetic_sources()
        first = next((self.root / 'sources').iterdir())
        first.write_bytes(b'changed')
        with patch.object(reader, 'PINS', pins), patch.object(reader, 'SOURCE_SIZE', (3, 5)):
            with self.assertRaisesRegex(config.EntryError, 'reader_original_changed'):
                reader.admit_sources(self.root)
        self.put(next(iter(pins)), b'changed')
        with patch.object(reader, 'PINS', pins), self.assertRaisesRegex(config.EntryError, 'reader_source_pin_rejected'):
            reader.admit_sources(self.root)

    def test_canonical_pixels_alpha_header_size_and_dimensions_refuse(self):
        raw = reader.MAGIC + struct.pack('>II', 3, 5) + self.pages[0].pixels
        self.assertEqual(reader.canonical_pixels(raw, 3, 5), self.pages[0].pixels)
        for invalid in (raw[:-1], raw.replace(reader.MAGIC, b'x'), raw[:-1] + b'\x00'):
            with self.subTest(length=len(invalid)), self.assertRaises(config.EntryError):
                reader.canonical_pixels(invalid, 3, 5)
        with self.assertRaises(config.EntryError): reader.canonical_pixels(raw, True, 5)

    def test_direct_lanczos_each_size_verified_and_original_pixels_unchanged(self):
        original = self.pages[0].pixels
        with patch.object(Image.Image, 'resize', wraps=Image.Image.resize, autospec=True) as mocked:
            # Spy through a concrete replacement to inspect the input and resampler.
            seen = []
            def resize(image, size, resample, **kwargs):
                seen.append((image.size, size, resample, kwargs))
                return Image.frombytes('RGB', image.size, image.tobytes())._new(
                    image.im.resize(size, resample, (0, 0, image.width, image.height)))
            mocked.side_effect = resize
            data = reader.encode_page(self.pages[0], {'width': 2, 'height': 3})
        self.assertEqual(seen[0], ((3, 5), (2, 3), Image.Resampling.LANCZOS, {'reducing_gap': None}))
        with Image.open(BytesIO(data)) as image: self.assertEqual(image.size, (2, 3))
        self.assertEqual(self.pages[0].pixels, original)
        with patch.object(reader, 'PILLOW_VERSION', '13.0.0'), self.assertRaises(config.EntryError):
            reader.encode_page(self.pages[0], {'width': 2, 'height': 3})

    def test_prepare_primary_full_bounded_and_direct_candidates_separate_quality(self):
        receipt = reader.prepare_variants(self.pages, {'synthetic': True}, geometry(), self.root / 'prepared')
        self.assertEqual(len(receipt['manifests']), 3)
        for batch in receipt['manifests']:
            manifest = config.strict_json(Path(receipt['manifests'][batch]['path']).read_bytes())
            self.assertEqual(len(manifest['entries']), 6)
            config.validate_reader_entries(batch, manifest['entries'])
            self.assertEqual(set(manifest['entries'][0]), set(config.ENTRY_KEYS))
            if batch.endswith('c_r1'): self.assertEqual((manifest['entries'][0]['width'], manifest['entries'][0]['height']), (2, 3))
            else: self.assertEqual((manifest['entries'][0]['width'], manifest['entries'][0]['height']), (3, 5))
        self.assertIsNone(receipt['qualityAccepted'])
        self.assertEqual((self.root / 'prepared').stat().st_mode & 0o777, 0o700)

    def test_discovery_only_before_geometry_and_lettered_quality_required(self):
        reader.prepare_variants(self.pages, {}, None, self.root / 'discovery', batches=['reader_resolution_a_r1'])
        with self.assertRaisesRegex(config.EntryError, 'reader_geometry_unavailable'):
            reader.prepare_variants(self.pages, {}, None, self.root / 'bad')
        with self.assertRaisesRegex(config.EntryError, 'reader_lettered_quality_sources_required'):
            reader.prepare_variants(self.pages, {}, geometry(), self.root / 'quality', batches=['reader_quality_full_r1'])
        quality = tuple(reader.SourcePage(1, None, 'a' * 64, 'b' * 64, 3, 5, self.pages[0].pixels,
            content_id, 'tr') for content_id in (71, 19))
        result = reader.prepare_variants(self.pages, {}, geometry(), self.root / 'tr-quality',
            batches=['reader_quality_full_r1', 'reader_quality_100_r1', 'reader_quality_075_r1'], quality_pages=quality)
        for item in result['manifests'].values():
            self.assertEqual(len(config.strict_json(Path(item['path']).read_bytes())['entries']), 2)

    def test_per_size_png_mismatch_or_corrupt_output_refuses(self):
        content = reader.encode_page(self.pages[0], {'width': 3, 'height': 5})
        for size, pixels in (((2, 3), b'x'), ((3, 5), b'x')):
            with self.assertRaises(config.EntryError): reader.verify_png(content, size, pixels)
        with self.assertRaises(config.EntryError): reader.verify_png(b'bad', (3, 5), b'bad')


class ReaderGeometryBudgetTests(unittest.TestCase):
    def test_contain_ceils_source_caps_no_upscale_or_crop(self):
        demand = reader.contain_dimensions(1712, 2778, 280.2, 460, 3.0)
        self.assertEqual((demand['width'], demand['height']), (841, 1365))
        self.assertTrue(demand['sourcePixelsSufficient'])
        stress = reader.contain_dimensions(1712, 2778, 280.2, 460, 3.0, .75)
        self.assertEqual((stress['width'], stress['height']), (631, 1024))
        capped = reader.contain_dimensions(3, 5, 20, 30, 1)
        self.assertEqual((capped['width'], capped['height']), (3, 5))
        self.assertFalse(capped['sourcePixelsSufficient']); self.assertFalse(capped['upscaled']); self.assertFalse(capped['cropped'])
        for value in (True, float('nan'), float('inf'), 0, -1):
            with self.subTest(value=value), self.assertRaises(config.EntryError):
                reader.contain_dimensions(1712, 2778, value, 460, 3)

    def test_finite_slot_and_dpr_with_overflowing_products_refuse_before_ceil(self):
        for width, height, dpr in ((1e308, 460, 1e308), (280, 1e308, 1e308),
                (10 ** 308, 460, 10 ** 308)):
            with self.subTest(width=width, height=height, dpr=dpr), \
                    self.assertRaisesRegex(config.EntryError, 'reader_demand_rejected'):
                reader.contain_dimensions(1712, 2778, width, height, dpr)

    def test_geometry_insets_text_scale_dpr_rotation_all_frozen(self):
        known = geometry()
        reader.require_same_geometry(known, deepcopy(known))
        for key in reader.GEOMETRY_FIELDS:
            changed = {**known, key: 'landscape' if key == 'orientation' else known[key] + .25}
            with self.subTest(key=key), self.assertRaises(config.EntryError):
                reader.require_same_geometry(known, changed)

    def test_two_windows_retain_all_services_old13_failures_reserve(self):
        snapshot = budget()
        first = reader.budget_before_create(snapshot, now=1000)
        self.assertEqual(first['pendingDebitMinutes'], 6)
        self.assertAlmostEqual(first['projectedAllServicesUpperUsd'], 8.75523914272981)
        snapshot['windows'].append(prior_window(seconds=59.5))
        second = reader.budget_before_create(snapshot, now=1000)
        self.assertAlmostEqual(second['projectedAllServicesUpperUsd'], 9.65523914272981)
        self.assertEqual(second['closedRootCeilMinutes'], [2])
        self.assertEqual(second['totalExtensionDebitedMinutes'], 12)
        # Fresh month usage already includes the first new debit; do not add it twice.
        snapshot['reportedMonthlyUsageMinutes'] = 75
        self.assertEqual(reader.budget_before_create(snapshot, now=1000)['projectedAllServicesUpperUsd'],
            second['projectedAllServicesUpperUsd'])
        snapshot['windows'].append(prior_window(2))
        with self.assertRaisesRegex(config.EntryError, 'reader_window_limit_refused'):
            reader.budget_before_create(snapshot, now=1000)

    def test_missing_debt_unknown_cleanup_changed_rate_or_stale_budget_refuses(self):
        for key, value in (('oldUpperMinutesRetained', 39), ('originalClosedRootMinutes', 28),
                ('originalReservationCount', 0), ('ownershipUnknown', True), ('cleanupUnknown', True),
                ('activeOrPending', 1), ('nonDeviceCostVerified', False), ('catalogSha256', None),
                ('maximumNewWindows', 3), ('minuteUsd', .2), ('observedAt', 699),
                ('nonDeviceTry', 1000), ('conversionTryPerUsd', 1), ('reportedMonthlyUsageMinutes', 200)):
            with self.subTest(key=key), self.assertRaises(config.EntryError):
                reader.budget_before_create({**budget(), key: value}, now=1000)
        for status in ('pending', 'failed', 'allocation_unknown'):
            snapshot = budget(); snapshot['windows'] = [{**prior_window(), 'status': status}]
            with self.assertRaises(config.EntryError): reader.budget_before_create(snapshot, now=1000)

    def test_exclusive_debit_receipt_precedes_create_and_cannot_overwrite(self):
        with tempfile.TemporaryDirectory() as temporary:
            base = Path(temporary).resolve(); os.chmod(base, 0o700)
            path = base / 'debit.json'
            reader.debit_before_create(budget(), path, now=1000)
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            prior = path.read_bytes()
            with self.assertRaises(config.EntryError): reader.debit_before_create(budget(), path, now=1000)
            self.assertEqual(path.read_bytes(), prior)

    def test_root_plus_second_and_entry_deadlines_are_strict(self):
        self.assertEqual([reader.closed_root_minutes(value) for value in (0, 59, 59.1, 60, 299, 300)], [1, 1, 2, 2, 5, 6])
        reader.entry_window_guard(104.9, 195.1)
        for elapsed, remaining in ((105, 196), (104, 195), (-1, 200), (True, 200), (100, None)):
            with self.subTest(elapsed=elapsed, remaining=remaining), self.assertRaises(config.EntryError):
                reader.entry_window_guard(elapsed, remaining)

    def test_authoritative_late_cleanup_overrun_increases_upper_charge(self):
        snapshot = budget(); snapshot['windows'] = [prior_window(seconds=360)]
        decision = reader.budget_before_create(snapshot, now=1000)
        self.assertEqual(decision['closedRootCeilMinutes'], [7])
        self.assertEqual(decision['totalExtensionDebitedMinutes'], 12)
        self.assertEqual(decision['chargedExtensionUpperMinutes'], 13)
        self.assertAlmostEqual(decision['projectedAllServicesUpperUsd'], 9.80523914272981)
        snapshot['windows'][0]['wholeRootSeconds'] = 600
        with self.assertRaisesRegex(config.EntryError, 'reader_total_budget_refused'):
            reader.budget_before_create(snapshot, now=1000)

    def test_legacy13_gate_unchanged_and_exhausted(self):
        from native_artwork_comparison import budget_before_create
        from test_native_artwork_comparison import NativeComparisonTests
        legacy = NativeComparisonTests().budget()
        with self.assertRaisesRegex(config.EntryError, 'budget_reservation_refused'):
            budget_before_create({**legacy, 'reservationCount': 13}, now=1000)
        self.assertEqual(budget_before_create({**legacy, 'reservationCount': 12}, now=1000)['maximumReservations'], 13)
        self.assertNotEqual(reader.budget_before_create, budget_before_create)


def make_reader_manifest(inputs, batch='reader_resolution_a_r1'):
    quality = batch.startswith('reader_quality_')
    inputs.values['batchId'] = batch
    inputs.manifest['batchId'] = batch
    inputs.manifest['entries'] = []
    for index, case in enumerate(config.READER_CASES[:2] if quality else config.READER_CASES):
        discover = batch == 'reader_resolution_a_r1'
        inputs.manifest['entries'].append({**inputs.entry, 'caseId': case,
            'sourceSha256': str(index + 1) * 64,
            'purposeAcceptanceReceipt': batch,
            'profileRevision': 'reader_discover_r1' if discover else 'reader_frozen_r1',
            'fit': 'contain', 'alignmentX': 0, 'alignmentY': 0,
            'logicalSlotWidth': 1 if discover else 2, 'logicalSlotHeight': 1 if discover else 3,
            'dpr': 1})
    return inputs.admitted_manifest()


class ReaderProtocolTests(PrivateInputs):
    def test_exact6_primary_exact2_quality_order_and_existing_schema(self):
        for batch in config.READER_BATCHES:
            with self.subTest(batch=batch):
                manifest = make_reader_manifest(self, batch)
                self.assertEqual(len(manifest.entries), 2 if batch.startswith('reader_quality_') else 6)
                public = config.strict_json(manifest.public_bytes)
                self.assertEqual(set(public), config.MANIFEST_KEYS)
                self.assertNotIn(b'filePath', manifest.public_bytes)

    def test_wrong_order_purpose_format_geometry_and_unknown_reader_batch_refuse(self):
        make_reader_manifest(self)
        for key, value in (('purposeAcceptanceReceipt', 'native_comparison_r1'), ('mime', 'image/jpeg'),
                ('profileRevision', 'reader_frozen_r1'), ('logicalSlotWidth', 2), ('alignmentY', .5)):
            original = self.manifest['entries'][0][key]
            self.manifest['entries'][0][key] = value
            with self.subTest(key=key), self.assertRaises(config.EntryError): self.admitted_manifest()
            self.manifest['entries'][0][key] = original
        self.manifest['entries'].reverse()
        with self.assertRaisesRegex(config.EntryError, 'reader_order_rejected'): self.admitted_manifest()
        self.values['batchId'] = 'reader_resolution_d_r1'
        with self.assertRaisesRegex(config.EntryError, 'reader_purpose_rejected'): self.configuration()

    def test_legacy_manifest_has_one_selected_permission_unchanged(self):
        manifest = self.admitted_manifest()
        broker = Broker(self.configuration(), manifest, lambda _: {'exp': 9999999999}, backend=lambda *_: True)
        self.addCleanup(broker.close)
        self.assertFalse(broker.reader)
        self.assertEqual(broker.allowed_cases, {'case_a'})


class ReaderBrokerTests(BrokerInputs):
    def reader_broker(self, batch='reader_resolution_a_r1'):
        self.broker.close()
        manifest = make_reader_manifest(self, batch)
        self.broker = Broker(self.configuration(), manifest, self.verifier, backend=lambda *_: True)
        self.addCleanup(self.broker.close)
        self.pairing.update(sessionId=self.broker.session_id, launchNonce=self.broker.nonce)
        return manifest

    def settled_pairing(self):
        return SimpleNamespace(poll=lambda: None, ready=SimpleNamespace(is_set=lambda: True),
            pending=None, count=1, native_result_count=1, native_result='settled')

    def test_all6_only_unknown_or_revoked_reject_and_delivery_totals_preserved(self):
        manifest = self.reader_broker()
        self.admit()
        for entry in manifest.entries:
            got, data = self.broker.fixture(entry['caseId'], self.broker.session_id, self.auth)
            self.assertEqual(got, entry)
            self.broker.completed_fixture(entry['caseId'], len(data), reader.digest(data))
        with self.assertRaisesRegex(config.EntryError, 'unknown_case'):
            self.broker.fixture('reader_page_7', self.broker.session_id, self.auth)
        self.broker.revoke_from_app(self.broker.session_id, self.auth)
        outcome = validate_entry_outcome(self.broker, self.settled_pairing())
        self.assertEqual(outcome['fixtureDeliveryCount'], 6)
        self.assertEqual(outcome['fixtureBodyBytes'], len(self.image) * 6)
        self.assertEqual(set(outcome['fixtureDeliveries']), set(config.READER_CASES))
        with self.assertRaises(config.EntryError):
            self.broker.fixture(config.READER_CASES[0], self.broker.session_id, self.auth)

    def test_missing_page_or_partial_wrong_hash_cannot_settle(self):
        manifest = self.reader_broker(); self.admit()
        first = manifest.entries[0]
        for size, digest in ((len(self.image) - 1, first['outputSha256']), (len(self.image), '0' * 64)):
            with self.assertRaises(config.EntryError):
                self.broker.completed_fixture(first['caseId'], size, digest)
        self.broker.completed_fixture(first['caseId'], len(self.image), first['outputSha256'])
        self.broker.revoke_from_app(self.broker.session_id, self.auth)
        with self.assertRaisesRegex(config.EntryError, 'fixture_delivery_unverified'):
            validate_entry_outcome(self.broker, self.settled_pairing())

    def test_quality2_protocol_does_not_dispatch_third_textless_page(self):
        manifest = self.reader_broker('reader_quality_full_r1'); self.admit()
        for entry in manifest.entries:
            _, data = self.broker.fixture(entry['caseId'], self.broker.session_id, self.auth)
            self.broker.completed_fixture(entry['caseId'], len(data), reader.digest(data))
        with self.assertRaisesRegex(config.EntryError, 'unknown_case'):
            self.broker.fixture('reader_page_3', self.broker.session_id, self.auth)
        self.broker.revoke_from_app(self.broker.session_id, self.auth)
        self.assertEqual(validate_entry_outcome(self.broker, self.settled_pairing())['fixtureDeliveryCount'], 2)


class ReaderResultTests(PrivateInputs):
    def test_failed_launch_restores_owned_observer_and_c_requires_fresh_prerequisites(self):
        import native_entry_driver as host
        import native_artwork_comparison as comparison
        manifest = make_reader_manifest(self)
        selected = config.Configuration({**self.values, 'platform': 'ios'}, 'a' * 64)
        output = self.base / 'run-failure'; output.mkdir(mode=0o700)
        original_broker, original_pairing = host.Broker, host.LauncherPairing
        with patch.object(host, 'validate_physical_prerequisites', return_value=(
                {'artifactSha256': 'a' * 64, 'testArtifactSha256': 'b' * 64}, {})), \
                patch.object(comparison, 'validate_comparison_audit'), \
                patch.object(host, 'source_snapshot', return_value=({}, 'c' * 64)), \
                patch.object(host, 'verify_entry', side_effect=config.EntryError('synthetic_oom')):
            with self.assertRaisesRegex(config.EntryError, 'synthetic_oom'):
                reader.run(selected, manifest, output, build_path='build', prerequisite_path='proof', audit_path='audit')
        self.assertIs(host.Broker, original_broker); self.assertIs(host.LauncherPairing, original_pairing)
        self.assertFalse(comparison.LOCK.locked())
        self.assertTrue((output / 'reader-tool-freeze.json').exists())
        self.assertTrue((output / 'reader-failure-receipt.json').exists())
        self.assertFalse((output / 'verification-receipt.json').exists())
        manifest = make_reader_manifest(self, 'reader_resolution_c_r1')
        selected = config.Configuration({**self.values, 'platform': 'ios'}, 'a' * 64)
        with patch.object(host, 'validate_physical_prerequisites', side_effect=config.EntryError('fresh_guard_refused')) as guard:
            with self.assertRaisesRegex(config.EntryError, 'fresh_guard_refused'):
                reader.run(selected, manifest, output, build_path='build', prerequisite_path='new-proof',
                    audit_path='audit', frozen_geometry=geometry())
            guard.assert_called_once()

    def fixture(self, batch='reader_resolution_a_r1'):
        manifest = make_reader_manifest(self, batch)
        binding = reader.reader_binding(manifest, {'batchId': batch, 'generation': 0,
            'sessionId': 'synthetic-reader-session', 'accessAlias': 'synthetic-access'},
            None if batch == 'reader_resolution_a_r1' else geometry())
        rows = []
        def row(event, at, **extra):
            rows.append({'event': event, 'monotonicMicroseconds': at, 'clockDomain': 'dart_monotonic',
                'clockUnit': 'microseconds', 'generation': 0, 'sessionSha256': binding['sessionSha256'], **extra})
        quality = batch.startswith('reader_quality_')
        phases = [('reader', 0), ('complete', 16000000)] if quality else [
            ('baseline', 0), ('reader', 20000000), ('natural_retention', 72000000),
            ('owned_eviction', 102000000), ('complete', 112000000)]
        for name, at in phases: row('phase', at, phase=name)
        reader_start = 0 if quality else 20000000
        row('reader_geometry', reader_start + 1, **geometry(), settingsSha256='8' * 64)
        row('device_facts', reader_start + 2, platform='ios', deviceFactsSha256='9' * 64,
            modelSha256='a' * 64, abisSha256='b' * 64, apiLevel=None, memTotalBytes=None,
            memoryClassMiB=None, lowRamDevice=None, isPhysicalDevice=True,
            qemu='unavailable', deviceReason='unavailable')
        # Actual Cubit schedule: initial, 4-second forward, 3-second backward,
        # 100-ms rapid burst, then the final selection at 43 seconds.
        schedule = [(0, 3), *[(index, index * 4000000) for index in range(1, 6)],
            *[(4 - step, 24000000 + step * 3000000) for step in range(5)],
            *[(page, 40000000 + step * 100000) for step, page in enumerate((5, 0, 4, 1, 3, 2))],
            (5, 43000000)]
        if not quality:
            for generation, (index, offset) in enumerate(schedule, 1):
                row('reader_selection', reader_start + offset, pageIndex=index,
                    selectionGeneration=generation)
        for index, entry in enumerate(manifest.entries):
            identity = binding['providers'][entry['caseId']]
            target = {'pageIndex': index, 'caseSha256': reader.digest(entry['caseId'].encode()),
                'providerSha256': identity['expectedProviderSha256'], 'attemptSha256': identity['expectedAttemptSha256']}
            row('body', reader_start + 10 + index * 10, **target, bodyBytes=entry['byteSize'])
            row('encoded_verified', reader_start + 11 + index * 10, **target, encodedVerified=True)
            row('decoded', reader_start + 12 + index * 10, **target,
                actualDecodeWidth=identity['expectedDecodeWidth'], actualDecodeHeight=identity['expectedDecodeHeight'])
            paint_offset = index * 10 if quality else index * 4000000
            row('page_paint', reader_start + 13 + paint_offset, **target, selectionGeneration=index + 1,
                physicalPresentationVerified=False)
        if not quality:
            previous = None
            for (name, at), (_, end) in zip(phases, phases[1:]):
                for start in range(at + 1000, end, 1000000):
                    row('memory_sample', start + 1, phase=name, sampleStartMicroseconds=start,
                        sampleEndMicroseconds=start + 1, observerOverheadMicroseconds=1,
                        sampleCadenceMicroseconds=None if previous is None else start - previous,
                        memoryBytes=1000 if name == 'baseline' else 1200, memoryCounter='ios_phys_footprint',
                        memoryReason='none', nativeReadMicroseconds=1)
                    previous = start
            row('lifecycle', 71000000, backgroundCount=1, returnCount=1, backgrounded=False,
                backgroundEpochMilliseconds=100, returnedEpochMilliseconds=200)
            for name, at in (('natural_retention', 72000000), ('owned_eviction', 102000000)):
                for entry in manifest.entries:
                    row('cache_state', at + 100, phase=name, caseSha256=reader.digest(entry['caseId'].encode()),
                        decodedLiveCount=0, decodedPendingCount=0, decodedKeepAliveCount=1 if name == 'natural_retention' else 0,
                        decodedLruBytes=100 if name == 'natural_retention' else 0)
            row('release_complete', 102000050, ownership='released')
        rows.sort(key=lambda row: row['monotonicMicroseconds'])
        outcome = {'mode': 'release', 'platform': 'ios', 'sessionSha256': binding['sessionSha256'],
            'generation': 0, 'nativeResult': 'settled', 'teardown': 'authenticated_app_revoke_observed',
            'fixtureDeliveries': {entry['caseId']: {'count': 1, 'bodyBytes': entry['byteSize'],
                'outputSha256': entry['outputSha256']} for entry in manifest.entries}}
        return manifest, binding, rows, outcome

    def qualify(self, fixture):
        manifest, binding, rows, outcome = fixture
        return reader.qualify(config.frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 0}),
            binding, outcome, manifest, platform='ios')

    def test_primary_decode_transfer_geometry_retention_eviction_and_null_facts_separate(self):
        result = self.qualify(self.fixture())
        self.assertEqual(result['qualification'], 'qualified')
        self.assertEqual(len(result['pages']), 6)
        self.assertEqual(result['memory']['naturalRetentionDeltaBytes'], 200)
        self.assertEqual(result['memory']['ownedEvictionDeltaBytes'], 200)
        self.assertEqual(result['lowMemoryPhysicalQualification'], 'unavailable')
        self.assertIsNone(result['qualityAccepted']); self.assertIsNone(result['p95'])
        self.assertEqual(result['framesReason'], 'unavailable')

    def test_real_primary_forward_back_rapid_schedule_and_timer_jitter_qualify(self):
        for batch in ('reader_resolution_a_r1', 'reader_resolution_b_r1', 'reader_resolution_c_r1'):
            fixture = self.fixture(batch)
            selections = [row for row in fixture[2] if row['event'] == 'reader_selection']
            self.assertEqual([row['selectionGeneration'] for row in selections], list(range(1, 19)))
            # Normal timer delays can follow an OS background/return. The host's rapid
            # observation tolerance is independent of device logs and the 100-ms target.
            for row in selections[1:11]: row['monotonicMicroseconds'] += 1500000
            for step, row in enumerate(selections[11:17]):
                row['monotonicMicroseconds'] += step * 20000
            fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
            with self.subTest(batch=batch):
                self.assertEqual(self.qualify(fixture)['qualification'], 'qualified')

    def test_missing_wrong_or_unbound_primary_journey_refuses(self):
        for missing in (None, 1, 7, 13, 17):
            fixture = self.fixture()
            selections = [row for row in fixture[2] if row['event'] == 'reader_selection']
            fixture[2][:] = [row for row in fixture[2] if row not in selections or
                missing is not None and row is not selections[missing]]
            with self.subTest(missing=missing), \
                    self.assertRaisesRegex(config.EntryError, 'reader_selection_journey_rejected'):
                self.qualify(fixture)
        for key, value in (('pageIndex', 5), ('selectionGeneration', None),
                ('selectionGeneration', 0), ('selectionGeneration', 9)):
            fixture = self.fixture()
            selections = [row for row in fixture[2] if row['event'] == 'reader_selection']
            selections[2][key] = value
            with self.subTest(key=key, value=value), \
                    self.assertRaisesRegex(config.EntryError, 'reader_selection_journey_rejected'):
                self.qualify(fixture)
        fixture = self.fixture()
        last = [row for row in fixture[2] if row['event'] == 'reader_selection'][-1]
        fixture[2].append({**last, 'monotonicMicroseconds': last['monotonicMicroseconds'] + 1})
        fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
        with self.assertRaisesRegex(config.EntryError, 'reader_selection_journey_rejected'):
            self.qualify(fixture)
        for index, at in ((0, 19999999), (-1, 72000000)):
            fixture = self.fixture()
            selections = [row for row in fixture[2] if row['event'] == 'reader_selection']
            selections[index]['monotonicMicroseconds'] = at
            fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
            with self.subTest(index=index, at=at), \
                    self.assertRaisesRegex(config.EntryError, 'reader_selection_journey_rejected'):
                self.qualify(fixture)

    def test_primary_rapid_burst_cannot_be_simultaneous_or_slow_selections(self):
        for spacing in (0, 250001, 1000001):
            fixture = self.fixture()
            selections = [row for row in fixture[2] if row['event'] == 'reader_selection']
            for step, row in enumerate(selections[11:17]):
                row['monotonicMicroseconds'] = 60000000 + step * spacing
            # Keep the final selection after the modified burst and inside the reader phase.
            selections[-1]['monotonicMicroseconds'] = selections[-2]['monotonicMicroseconds'] + 1
            fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
            with self.subTest(spacing=spacing), \
                    self.assertRaisesRegex(config.EntryError, 'reader_selection_rapid_rejected'):
                self.qualify(fixture)

    def test_missing_null_or_reversed_lifecycle_timestamps_make_primary_unavailable(self):
        for key in ('backgroundEpochMilliseconds', 'returnedEpochMilliseconds'):
            for missing in (True, False):
                fixture = self.fixture()
                lifecycle = next(row for row in fixture[2] if row['event'] == 'lifecycle')
                if missing: lifecycle.pop(key)
                else: lifecycle[key] = None
                result = self.qualify(fixture)
                with self.subTest(key=key, missing=missing):
                    self.assertEqual(result['lifecycleReason'], 'unavailable')
                    self.assertEqual(result['qualification'], 'unavailable')
                    self.assertEqual(result['lifecycle'], lifecycle)
        fixture = self.fixture()
        next(row for row in fixture[2] if row['event'] == 'lifecycle')['returnedEpochMilliseconds'] = 99
        result = self.qualify(fixture)
        self.assertEqual(result['lifecycleReason'], 'unavailable')
        self.assertEqual(result['qualification'], 'unavailable')

    def test_lifecycle_timestamps_require_integer_types_and_allow_ordered_equality(self):
        for key in ('backgroundEpochMilliseconds', 'returnedEpochMilliseconds'):
            for value in (True, 1.5, '100'):
                fixture = self.fixture()
                next(row for row in fixture[2] if row['event'] == 'lifecycle')[key] = value
                with self.subTest(key=key, value=value), self.assertRaises(config.EntryError):
                    self.qualify(fixture)
        fixture = self.fixture()
        next(row for row in fixture[2] if row['event'] == 'lifecycle')['returnedEpochMilliseconds'] = 100
        self.assertEqual(self.qualify(fixture)['lifecycleReason'], 'observed')

    def test_ordinary_samples_retain_existing_metrics_with_only_zero_crossing_fields(self):
        memory = self.qualify(self.fixture())['memory']
        self.assertEqual(memory['eligibility'], 'observed_samples')
        expected_counts = dict(baseline=20, reader=52, natural_retention=30, owned_eviction=10)
        for phase, window in memory['windows'].items():
            self.assertEqual({key: value for key, value in window.items() if key.startswith('crossing')}, {
                key: 0 for key in ('crossingSampleCount', 'crossingValidCount', 'crossingNullCount',
                    'crossingObserverMicroseconds', 'crossingNativeReadCount', 'crossingNativeReadNullCount',
                    'crossingNativeReadMicroseconds')})
            self.assertEqual(window['sampleCount'], expected_counts[phase])
            self.assertEqual(window['validCount'], expected_counts[phase])
            self.assertEqual(window['medianBytes'], 1000 if phase == 'baseline' else 1200)
            self.assertEqual(window['maximumGapMicroseconds'], 1000000)

    def test_phase_crossing_valid_and_null_native_samples_remain_explicit_and_unavailable(self):
        for unavailable in (False, True):
            fixture = self.fixture()
            sample = next(row for row in fixture[2] if row['event'] == 'memory_sample' and
                row['sampleStartMicroseconds'] == 19001000)
            sample.update(sampleEndMicroseconds=20000010, monotonicMicroseconds=20000011,
                observerOverheadMicroseconds=999010, memoryBytes=99999, nativeReadMicroseconds=17)
            if unavailable:
                sample.update(memoryBytes=None, memoryCounter='unavailable', memoryReason='sample_timeout',
                    nativeReadMicroseconds=None, nativeResultCode=None, nativeResultCount=None)
            fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
            result = self.qualify(fixture)
            memory = result['memory']; baseline = memory['windows']['baseline']
            with self.subTest(unavailable=unavailable):
                self.assertEqual(result['qualification'], 'unavailable')
                self.assertEqual(memory['eligibility'], 'phase_crossing_counter_unavailable')
                self.assertEqual(baseline['sampleCount'], 19)
                self.assertEqual(baseline['medianBytes'], 1000)
                self.assertEqual(baseline['crossingSampleCount'], 1)
                self.assertEqual(baseline['crossingValidCount'], 0 if unavailable else 1)
                self.assertEqual(baseline['crossingNullCount'], 1 if unavailable else 0)
                self.assertEqual(baseline['crossingObserverMicroseconds'], 999010)
                self.assertEqual(baseline['crossingNativeReadCount'], 0 if unavailable else 1)
                self.assertEqual(baseline['crossingNativeReadNullCount'], 1 if unavailable else 0)
                self.assertEqual(baseline['crossingNativeReadMicroseconds'], None if unavailable else 17)
                crossing = baseline['crossingSamples'][0]
                for key in ('sampleStartMicroseconds', 'sampleEndMicroseconds', 'observerOverheadMicroseconds',
                        'memoryBytes', 'memoryCounter', 'memoryReason', 'nativeReadMicroseconds'):
                    self.assertEqual(crossing[key], sample[key])
                self.assertEqual(crossing['reason'], 'phase_boundary_crossing')
                self.assertEqual(crossing['crossedBoundaries'], [
                    {'phase': 'reader', 'monotonicMicroseconds': 20000000}])

    def test_sample_ending_exactly_at_boundary_is_not_crossing_and_wrong_start_phase_refuses(self):
        fixture = self.fixture()
        sample = next(row for row in fixture[2] if row['event'] == 'memory_sample' and
            row['sampleStartMicroseconds'] == 19001000)
        sample.update(sampleEndMicroseconds=20000000, monotonicMicroseconds=20000000,
            observerOverheadMicroseconds=999000)
        fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
        result = self.qualify(fixture)
        self.assertEqual(result['qualification'], 'qualified')
        self.assertEqual(result['memory']['windows']['baseline']['crossingSampleCount'], 0)
        sample['phase'] = 'reader'
        with self.assertRaisesRegex(config.EntryError, 'reader_sample_phase_rejected'):
            self.qualify(fixture)

    def test_each_phase_boundary_preserves_crossing_sample_and_native_observer_time(self):
        for phase, start, boundary, next_phase in (('baseline', 19001000, 20000000, 'reader'),
                ('reader', 71001000, 72000000, 'natural_retention'),
                ('natural_retention', 101001000, 102000000, 'owned_eviction'),
                ('owned_eviction', 111001000, 112000000, 'complete')):
            fixture = self.fixture()
            sample = next(row for row in fixture[2] if row['event'] == 'memory_sample' and
                row['sampleStartMicroseconds'] == start)
            sample.update(sampleEndMicroseconds=boundary + 10, monotonicMicroseconds=boundary + 11,
                observerOverheadMicroseconds=boundary + 10 - start, nativeReadMicroseconds=17)
            fixture[2].sort(key=lambda row: row['monotonicMicroseconds'])
            result = self.qualify(fixture)
            window = result['memory']['windows'][phase]
            with self.subTest(phase=phase):
                self.assertEqual(result['qualification'], 'unavailable')
                self.assertEqual(window['crossingSampleCount'], 1)
                self.assertEqual(window['crossingNativeReadMicroseconds'], 17)
                self.assertEqual(window['crossingObserverMicroseconds'], boundary + 10 - start)
                self.assertEqual(window['crossingSamples'][0]['crossedBoundaries'], [
                    {'phase': next_phase, 'monotonicMicroseconds': boundary}])

    def test_missing_counter_death_or_gap_is_explicit_not_zero(self):
        fixture = self.fixture()
        for row in fixture[2]:
            if row['event'] == 'memory_sample':
                row.update(memoryBytes=None, memoryCounter='unavailable', memoryReason='sample_failed', nativeReadMicroseconds=None)
        result = self.qualify(fixture)
        self.assertEqual(result['qualification'], 'unavailable')
        self.assertIsNone(result['memory']['sampledOnstageDeltaBytes'])
        self.assertIsNone(result['memory']['ownedEvictionDeltaBytes'])
        self.assertEqual(result['memory']['eligibility'], 'missing_or_gapped_phase_counter')
        missing = self.fixture(); missing[2][:] = [row for row in missing[2] if row['event'] != 'lifecycle']
        self.assertEqual(self.qualify(missing)['lifecycleReason'], 'unavailable')

    def test_zero_frame_callbacks_remain_explicitly_unavailable(self):
        fixture = self.fixture()
        fixture[2].append({**fixture[2][-1], 'event': 'frame_aggregate', 'phase': 'complete',
            'frameCount': 0, 'frameBuildTotalMicroseconds': 0, 'frameRasterTotalMicroseconds': 0,
            'frameBuildMaxMicroseconds': 0, 'frameRasterMaxMicroseconds': 0, 'reason': 'frame_unavailable'})
        result = self.qualify(fixture)
        self.assertEqual(result['framesReason'], 'unavailable')
        self.assertEqual(result['frames'][0]['frameCount'], 0)

    def test_actual_decode_independent_of_reported_admission_and_stale_bindings_refuse(self):
        for event in ('decoded', 'page_paint', 'body', 'encoded_verified'):
            fixture = self.fixture()
            row = next(row for row in fixture[2] if row['event'] == event)
            row['providerSha256'] = 'f' * 64
            with self.subTest(event=event), self.assertRaises(config.EntryError): self.qualify(fixture)
        fixture = self.fixture(); fixture[1]['providers']['reader_page_1']['expectedDecodeWidth'] = 1
        for row in fixture[2]:
            if row['event'] == 'decoded' and row['pageIndex'] == 0: row['actualDecodeWidth'] = 1
        with self.assertRaisesRegex(config.EntryError, 'reader_expected_decode_rejected'): self.qualify(fixture)

    def test_c_frozen_geometry_text_insets_or_actual_slot_changes_refuse(self):
        for key in reader.GEOMETRY_FIELDS:
            fixture = self.fixture('reader_resolution_c_r1')
            row = next(row for row in fixture[2] if row['event'] == 'reader_geometry')
            row[key] = 'landscape' if key == 'orientation' else row[key] + .1
            with self.subTest(key=key), self.assertRaises(config.EntryError): self.qualify(fixture)

    def test_failure_missing_page_ownership_or_unsafe_phase_refuses(self):
        for event in ('page_paint', 'release_complete', 'cache_state', 'encoded_verified', 'body'):
            fixture = self.fixture(); fixture[2][:] = [row for row in fixture[2] if row['event'] != event]
            with self.subTest(event=event), self.assertRaises(config.EntryError): self.qualify(fixture)
        fixture = self.fixture(); fixture[2][0]['event'] = 'failure'
        with self.assertRaises(config.EntryError): self.qualify(fixture)
        fixture = self.fixture(); fixture[3]['nativeResult'] = 'refused'
        with self.assertRaises(config.EntryError): self.qualify(fixture)

    def test_quality_two_lettered_pages_is_separate20second_denominator(self):
        for batch in ('reader_quality_full_r1', 'reader_quality_100_r1', 'reader_quality_075_r1'):
            fixture = self.fixture(batch)
            result = self.qualify(fixture)
            with self.subTest(batch=batch):
                self.assertEqual(len(result['pages']), 2)
                self.assertIsNone(result['memory'])
                self.assertIsNone(result['qualityAccepted'])
                self.assertEqual(result['qualification'], 'awaiting_human_quality')
                self.assertEqual(result['lifecycleReason'], 'unavailable')
                self.assertFalse(any(row['event'] == 'reader_selection' for row in fixture[2]))
        next(row for row in fixture[2] if row.get('phase') == 'complete')['monotonicMicroseconds'] = 20000001
        with self.assertRaisesRegex(config.EntryError, 'reader_quality_budget_rejected'): self.qualify(fixture)

    def test_ledger_limits_unknown_fields_duplicate_json_and_overflow_refuse(self):
        fixture = self.fixture()
        binding = fixture[1]
        for data in (b'{}', b'{"x":1,"x":2}', b' ' * 250001,
            config.frozen_json({'schemaVersion': 1, 'records': fixture[2], 'droppedCount': 1}),
            config.frozen_json({'schemaVersion': 1, 'records': fixture[2] * 12, 'droppedCount': 0})):
            with self.assertRaises(config.EntryError): reader.reader_ledger_rows(data, binding)
        fixture[2][0]['rawDeviceModel'] = 'private-model'
        with self.assertRaises(config.EntryError): self.qualify(fixture)

    def test_android_operator_physical_binding_allows_native_unknown_but_refuses3gb_emulator_or_false(self):
        def android_fixture():
            manifest, binding, rows, outcome = self.fixture()
            binding['physicalTwoGiBAdmitted'] = True
            outcome['platform'] = 'android'
            for row in rows:
                if row['event'] == 'memory_sample': row['memoryCounter'] = 'android_total_pss'
                if row['event'] == 'device_facts':
                    row.update(platform='android', isPhysicalDevice=None, qemu='unavailable',
                        apiLevel=30, lowRamDevice=True, memTotalBytes=1800 * 1024 ** 2, memoryClassMiB=128)
            return manifest, binding, rows, outcome
        manifest, binding, rows, outcome = android_fixture()
        result = reader.qualify(config.frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 0}),
            binding, outcome, manifest, platform='android')
        self.assertEqual(result['lowMemoryPhysicalQualification'], 'observed')
        self.assertEqual(result['qualification'], 'qualified')
        for key, value in (('memTotalBytes', 3 * 1024 ** 3), ('qemu', '1'), ('isPhysicalDevice', False)):
            manifest, binding, rows, outcome = android_fixture()
            next(row for row in rows if row['event'] == 'device_facts')[key] = value
            result = reader.qualify(config.frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 0}),
                binding, outcome, manifest, platform='android')
            self.assertEqual(result['qualification'], 'unavailable')
            self.assertEqual(result['lowMemoryPhysicalQualification'], 'unavailable')

    def test_gap_native_read_unknown_and_source_shortage_cannot_qualify(self):
        manifest, binding, rows, outcome = self.fixture()
        # Missing a sample is a disclosed gap, retaining the actual next-start cadence.
        rows[:] = [row for row in rows if row['event'] != 'memory_sample' or not
            22000000 < row['sampleStartMicroseconds'] < 26000000]
        previous = None
        for row in rows:
            if row['event'] == 'memory_sample':
                row['sampleCadenceMicroseconds'] = None if previous is None else row['sampleStartMicroseconds'] - previous
                previous = row['sampleStartMicroseconds']
        result = self.qualify((manifest, binding, rows, outcome))
        self.assertEqual(result['qualification'], 'unavailable')
        self.assertGreater(result['memory']['windows']['reader']['maximumGapMicroseconds'], 2000000)
        fixture = self.fixture()
        for row in fixture[2]:
            if row['event'] == 'reader_geometry': row['observedSlotWidth'] = row['observedSlotHeight'] = 100
        result = self.qualify(fixture)
        self.assertEqual(result['qualification'], 'unavailable')
        self.assertEqual(result['sourceSufficiencyReason'], 'insufficient_pixels')


if __name__ == '__main__':
    unittest.main()
