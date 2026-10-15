import base64
from copy import deepcopy
from hashlib import sha256
import unittest
from native_entry_config import EntryError, Manifest, frozen_json
from critical_ios_guard_pilot import provider_binding
from native_artwork_comparison import (PRIMARY, QUALITY, LedgerChunks, budget_before_create, run_plan,
    quality_plan, qualify, memory_summary, paired_differences, qualify_capture, ledger_rows, AndroidQualityBindingReader,
    validate_comparison_audit, backend_inputs, stable_capture_binding, validate_capture_binding)


class NativeComparisonTests(unittest.TestCase):
    def expected_provider(self):
        return {'expectedProviderSha256': '2' * 64, 'expectedAttemptSha256': '3' * 64,
            'expectedDecodeWidth': 840, 'expectedDecodeHeight': 1364}

    def fixture(self):
        binding = {'generation': 0, 'caseSha256': '1' * 64, 'providerSha256': '2' * 64,
            'attemptSha256': '3' * 64, 'sessionSha256': '4' * 64}
        rows = []
        def row(event, at, **extra):
            rows.append({'event': event, 'monotonicMicroseconds': at, 'clockDomain': 'dart_monotonic',
                'clockUnit': 'microseconds', **binding, **extra})
        row('phase', 0, phase='baseline')
        for index in range(60):
            row('memory_sample', index * 500000 + 1, phase='baseline', sampleStartMicroseconds=index * 500000,
                sampleEndMicroseconds=index * 500000 + 1, observerOverheadMicroseconds=1,
                memoryBytes=1000, memoryCounter='ios_phys_footprint', memoryReason='none', nativeReadMicroseconds=1)
        row('phase', 30000000, phase='load')
        row('dispatch', 30000001)
        row('ownership', 30000002, ownership='owned', sourceSha256='5' * 64, referenceSha256='6' * 64, settingsSha256='7' * 64, scopeSha256='8' * 64)
        row('cache_state', 30000003, cacheState='namespace_keys_absent')
        row('request', 30000003, requestCount=1, retryCount=0)
        row('body', 30000003, bodyBytes=669608)
        row('encoded_verified', 30000004, encodedVerified=True)
        row('decoded', 30000005, actualDecodeWidth=840, actualDecodeHeight=1364)
        row('draw', 30000006, firstRasterizedTargetFrame=None, physicalPresentationVerified=False)
        row('layout', 30000007, observedSlotWidth=280.0, observedSlotHeight=460.0, observedDpr=3.0, actualDecodeWidth=840, actualDecodeHeight=1364)
        row('phase', 30000008, phase='hold')
        for index in range(20):
            start = 30000100 + index * 500000
            row('memory_sample', start + 1, phase='hold', sampleStartMicroseconds=start,
                sampleEndMicroseconds=start + 1, observerOverheadMicroseconds=1,
                memoryBytes=1500, memoryCounter='ios_phys_footprint', memoryReason='none', nativeReadMicroseconds=1)
        row('phase', 40000008, phase='releasing')
        row('ownership', 40000009, ownership='released')
        row('release_complete', 40000010, ownership='released')
        row('phase', 40000011, phase='post_release')
        for index in range(60):
            start = 40000100 + index * 500000
            row('memory_sample', start + 1, phase='post_release', sampleStartMicroseconds=start,
                sampleEndMicroseconds=start + 1, observerOverheadMicroseconds=1,
                memoryBytes=1100, memoryCounter='ios_phys_footprint', memoryReason='none', nativeReadMicroseconds=1)
        row('phase', 70000011, phase='complete')
        row('revoke', 70000012)
        last_start = None
        for sample in rows:
            if sample['event'] == 'memory_sample':
                sample['sampleCadenceMicroseconds'] = None if last_start is None else sample['sampleStartMicroseconds'] - last_start
                last_start = sample['sampleStartMicroseconds']
        outcome = {'platform': 'ios', 'mode': 'release', 'sessionSha256': binding['sessionSha256'],
            'caseSha256': binding['caseSha256'], 'generation': 0, 'nativeResult': 'settled',
            'fixtureDeliveryCount': 1, 'fixtureBodyBytes': 669608, 'fixtureOutputSha256': 'a' * 64,
            'artifactSha256': 'b' * 64, 'testArtifactSha256': 'c' * 64,
            'teardown': 'authenticated_app_revoke_observed'}
        return binding, rows, outcome

    def qualified(self, rows=None, binding=None, outcome=None):
        known_binding, known_rows, known_outcome = self.fixture()
        return qualify(frozen_json({'schemaVersion': 1, 'records': known_rows if rows is None else rows,
            'droppedCount': 0}), known_binding if binding is None else binding,
            known_outcome if outcome is None else outcome, platform='ios', expected_provider=self.expected_provider())

    def test_expected_provider_is_required_exact_typed_and_bound_independently(self):
        binding, rows, outcome = self.fixture()
        expected = self.expected_provider()
        mutations = [None, {}, {key: value for key, value in expected.items() if key != 'expectedDecodeHeight'}]
        mutations += [{**expected, key: value} for key, value in
            (('expectedDecodeWidth', True), ('expectedDecodeHeight', 0), ('expectedDecodeHeight', '1364'),
             ('expectedProviderSha256', '9' * 64), ('expectedAttemptSha256', '0' * 64),
             ('expectedDecodeWidth', 16777217), ('unknown', 1))]
        for admission in mutations:
            with self.subTest(admission=admission), self.assertRaises(EntryError):
                qualify(frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 0}),
                    binding, outcome, platform='ios', expected_provider=admission)
        for events in (('decoded', 'layout'), ('decoded',), ('layout',)):
            changed = deepcopy(rows)
            for row in changed:
                if row['event'] in events: row.update(actualDecodeWidth=1, actualDecodeHeight=1)
            with self.subTest(events=events), self.assertRaises(EntryError): self.qualified(rows=changed)

    def test_c71_c19_dimensions_are_derived_from_admitted_manifest_identity(self):
        envelope = {'batchId': PRIMARY, 'sessionId': 'synthetic-admitted-session', 'generation': 0,
            'accessAlias': 'synthetic-access'}
        for source, width, height, expected_size in ((71, 1712, 2778, (840, 1364)), (19, 1284, 2279, (778, 1380))):
            entry = {'caseId': f'c{source}', 'role': 'isolated_raster_fixture', 'arm': 'jpeg',
                'sourceSha256': '5' * 64, 'referenceSha256': '6' * 64, 'outputSha256': 'a' * 64,
                'mime': 'image/jpeg', 'byteSize': 669608, 'width': width, 'height': height,
                'encoderRevision': 'synthetic-r1', 'profileRevision': 'synthetic-r1',
                'logicalSlotWidth': 280.0, 'logicalSlotHeight': 460.0, 'dpr': 3.0, 'fit': 'contain',
                'alignmentX': 0.0, 'alignmentY': 0.0, 'purposeAcceptanceReceipt': PRIMARY}
            manifest = Manifest((entry,), {}, 'a' * 64, b'{}')
            admission = provider_binding(manifest, envelope, entry['caseId'])
            self.assertEqual((admission['expectedDecodeWidth'], admission['expectedDecodeHeight']), expected_size)
            binding, rows, outcome = self.fixture()
            binding.update(caseSha256=sha256(entry['caseId'].encode()).hexdigest(),
                providerSha256=admission['expectedProviderSha256'], attemptSha256=admission['expectedAttemptSha256'])
            for row in rows:
                row.update(binding)
                if row['event'] in ('decoded', 'layout'):
                    row.update(actualDecodeWidth=expected_size[0], actualDecodeHeight=expected_size[1])
            outcome.update(caseSha256=binding['caseSha256'])
            result = qualify(frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 0}),
                binding, outcome, platform='ios', expected_provider=admission)
            self.assertEqual(tuple(result['decode'].values()), expected_size)

    def test_target_sequence_outside_load_refuses_even_with_ordered_ledger(self):
        binding, rows, outcome = self.fixture()
        target_events = {'dispatch', 'ownership', 'cache_state', 'request', 'body', 'encoded_verified', 'decoded', 'draw', 'layout'}
        for purpose in (PRIMARY, QUALITY):
            selected = deepcopy(rows)
            if purpose == QUALITY:
                selected = [row for row in selected if row['event'] != 'memory_sample' and not
                    (row['event'] == 'phase' and row['phase'] in ('baseline', 'post_release'))]
            for shift in (-1000000, 1000000):
                changed = deepcopy(selected)
                for row in changed:
                    if row['event'] in target_events and row.get('ownership') != 'released':
                        row['monotonicMicroseconds'] += shift
                changed.sort(key=lambda row: row['monotonicMicroseconds'])
                with self.subTest(purpose=purpose, shift=shift), self.assertRaisesRegex(EntryError, '^target_outside_load$'):
                    qualify(frozen_json({'schemaVersion': 1, 'records': changed, 'droppedCount': 0}),
                        binding, outcome, platform='ios', purpose=purpose, expected_provider=self.expected_provider())

    def test_capture_expected_binding_requires_exact_complete_strict_identity(self):
        binding, _, _ = self.fixture()
        row = {**binding, 'observedSlotWidth': 280, 'observedSlotHeight': 460, 'observedDpr': 3,
            'actualDecodeWidth': 840, 'actualDecodeHeight': 1364, 'phase': 'hold', 'orientation': 'portrait',
            'nativeContext': 'foreground_application', 'holdDeadlineEpochMilliseconds': 11000}
        mutations = [{}, *[{key: value for key, value in binding.items() if key != missing} for missing in binding],
            {**binding, 'generation': False}, {**binding, 'unknown': 'x'}, {**binding, 'caseSha256': 1},
            {**binding, 'providerSha256': 'x' * 64}]
        for expected in mutations:
            with self.subTest(expected=expected), self.assertRaises(EntryError): validate_capture_binding(row, expected, 'ios')
        validate_capture_binding(row, binding, 'ios')

    def test_frozen_order_and_separate_quality_denominators(self):
        ios, android = run_plan('ios'), run_plan('android')
        self.assertEqual(len(ios), 20); self.assertEqual(len(android), 4)
        expected = [(1, 71, 'jpeg'), (1, 71, 'webp'), (1, 19, 'jpeg'), (1, 19, 'webp'),
            (2, 71, 'webp'), (2, 71, 'jpeg'), (2, 19, 'webp'), (2, 19, 'jpeg'),
            (3, 71, 'jpeg'), (3, 71, 'webp'), (3, 19, 'jpeg'), (3, 19, 'webp'),
            (4, 71, 'webp'), (4, 71, 'jpeg'), (4, 19, 'webp'), (4, 19, 'jpeg'),
            (5, 71, 'jpeg'), (5, 71, 'webp'), (5, 19, 'jpeg'), (5, 19, 'webp')]
        self.assertEqual([(item['repetition'], item['source'], item['arm']) for item in ios], expected)
        self.assertEqual([(item['source'], item['arm']) for item in android], [(71, 'jpeg'), (71, 'webp'), (19, 'webp'), (19, 'jpeg')])
        self.assertEqual(len(quality_plan('ios')), 6)

    def test_phase_medians_sampled_maximum_and_same_dart_clock(self):
        result = self.qualified()
        self.assertEqual(result['dispatchToReadyMicroseconds'], 4)
        self.assertEqual(result['dispatchToTargetPaintMicroseconds'], 5)
        self.assertEqual(result['memory']['sampledLoadOnstageDeltaBytes'], 500)
        self.assertEqual(result['memory']['finalReleaseDeltaBytes'], 100)
        self.assertEqual(result['memory']['windows']['baseline']['sampleCount'], 60)
        self.assertIsNone(result['memory']['truePeak']); self.assertIsNone(result['memory']['leakConclusion'])
        self.assertIsNone(result['qualityQualifiedSavings'])

    def test_missing_counter_and_gaps_stay_explicit(self):
        _, rows, _ = self.fixture()
        rows = [row for row in rows if not (row.get('event') == 'memory_sample' and row.get('phase') == 'baseline')]
        next(row for row in rows if row['event'] == 'memory_sample')['sampleCadenceMicroseconds'] = None
        result = self.qualified(rows=rows)
        self.assertIsNone(result['memory']['sampledLoadOnstageDeltaBytes'])
        self.assertEqual(result['memory']['windows']['baseline']['validCount'], 0)
        self.assertEqual(result['memory']['windows']['baseline']['maximumGapMicroseconds'], 30000000)

    def test_stale_overflow_failure_and_unknown_field_refused(self):
        binding, rows, outcome = self.fixture()
        for mutated in (dict(binding, sessionSha256='9' * 64), dict(binding, generation=1)):
            with self.assertRaises(EntryError): self.qualified(binding=mutated)
        for key, value in (('idToken', 'secret'), ('event', 'failure')):
            changed = deepcopy(rows); changed[-1][key] = value
            with self.assertRaises(EntryError): self.qualified(rows=changed)
        with self.assertRaises(EntryError):
            qualify(frozen_json({'schemaVersion': 1, 'records': rows, 'droppedCount': 1}), binding, outcome,
                platform='ios', expected_provider=self.expected_provider())

    def test_bad_release_order_duration_overlap_and_counter_refused(self):
        _, rows, _ = self.fixture()
        for key, value in (('memoryCounter', 'android_total_pss'), ('sampleEndMicroseconds', 999999999)):
            changed = deepcopy(rows); changed[1][key] = value
            with self.assertRaises(EntryError): self.qualified(rows=changed)
        changed = [row for row in rows if row.get('event') != 'release_complete']
        with self.assertRaises(EntryError): self.qualified(rows=changed)
        changed = deepcopy(rows)
        next(row for row in changed if row.get('phase') == 'complete')['phase'] = 'load'
        with self.assertRaises(EntryError): memory_summary(changed, 'ios')

    def test_layout_missing_release_mode_and_transfer_count_refused(self):
        _, rows, outcome = self.fixture()
        for replacement in ({'mode': 'profile'}, {'fixtureDeliveryCount': 2}, {'nativeResult': 'refused'}):
            with self.assertRaises(EntryError): self.qualified(outcome={**outcome, **replacement})
        changed = [row for row in rows if row.get('event') != 'layout']
        with self.assertRaises(EntryError): self.qualified(rows=changed)
        changed = [row for row in rows if row.get('event') != 'cache_state']
        with self.assertRaises(EntryError): self.qualified(rows=changed)

    def budget(self):
        return {'observedAt': 1000, 'catalogObservedAt': 1000, 'usageObservedAt': 1000,
            'inventoryObservedAt': 1000, 'ownedReceiptsVerified': True, 'ownershipUnknown': False,
            'cleanupUnknown': False, 'activeOrPending': 0, 'reservationCount': 0,
            'reconciledUpperMinutes': 40, 'minuteUsd': 0.15, 'conversionTryPerUsd': 48.98225000067947,
            'noCostTierMinutes': 30, 'catalogSha256': '1' * 64, 'usageSha256': '2' * 64,
            'inventorySha256': '3' * 64, 'ownedReceiptsSha256': '4' * 64,
            'costModelRevision': 'closed_root_ceil_plus_one_second_r2', 'maximumReservations': 13}

    def test_budget_full_pending_debit_no_reported_zero_credit(self):
        result = budget_before_create(self.budget(), now=1000)
        self.assertEqual(result['projectedMonthlyUpperMinutes'], 46)
        self.assertAlmostEqual(result['projectedUsageCostUpperUsd'], 2.4)
        ninth = {**self.budget(), 'reservationCount': 8, 'reconciledUpperMinutes': 88}
        self.assertAlmostEqual(budget_before_create(ninth, now=1000)['projectedUsageCostUpperUsd'], 9.6)
        thirteenth = {**self.budget(), 'reservationCount': 12, 'reconciledUpperMinutes': 83}
        self.assertAlmostEqual(budget_before_create(thirteenth, now=1000)['projectedUsageCostUpperUsd'], 8.85)

    def test_budget_unknown_cleanup_stale_overflow_and_limit_refuse(self):
        for key, value in (('cleanupUnknown', True), ('ownershipUnknown', True), ('ownedReceiptsVerified', False),
            ('activeOrPending', 1), ('reservationCount', 13), ('reconciledUpperMinutes', 91),
            ('costModelRevision', 'unapproved'), ('maximumReservations', 14), ('maximumReservations', True),
            ('catalogObservedAt', 699), ('conversionTryPerUsd', float('nan'))):
            with self.assertRaises(EntryError): budget_before_create({**self.budget(), key: value}, now=1000)

    def chunks(self, raw):
        digest = sha256(raw).hexdigest(); count = math.ceil(len(raw) / 3072)
        for index in range(count):
            for field, value in {'event': 'ledger-chunk', 'index': str(index), 'count': str(count),
                'ledgerSha256': digest, 'payloadBase64': base64.b64encode(raw[index * 3072:(index + 1) * 3072]).decode()}.items():
                yield f'INSTRUMENTATION_STATUS: {field}={value}'.encode()
            yield b'INSTRUMENTATION_STATUS_CODE: 2'

    def test_release_ledger_chunking_reconstructs_large_private_ledger(self):
        raw = b'x' * 250000
        collector = LedgerChunks()
        for line in self.chunks(raw):
            self.assertLessEqual(len(line), 4200); collector.feed(line)
        self.assertEqual(collector.finish(), raw)

    def test_ledger_chunks_incomplete_duplicate_and_corrupt_refuse(self):
        collector = LedgerChunks()
        for line in list(self.chunks(b'x' * 5000))[:6]: collector.feed(line)
        with self.assertRaises(EntryError): collector.finish()
        collector = LedgerChunks()
        collector.feed(b'INSTRUMENTATION_STATUS: event=ledger-chunk')
        with self.assertRaises(EntryError): collector.feed(b'INSTRUMENTATION_STATUS: event=ledger-chunk')
        collector = LedgerChunks()
        for line in self.chunks(b'abc'):
            collector.feed(line.replace(b'YWJj', b'YWJk'))
        with self.assertRaises(EntryError): collector.finish()

    def test_capture_before_after_provenance_and_human_verdict_are_separate(self):
        from io import BytesIO
        from PIL import Image
        binding, _, _ = self.fixture()
        row = {**binding, 'observedSlotWidth': 280, 'observedSlotHeight': 460,
            'observedDpr': 3, 'actualDecodeWidth': 840, 'actualDecodeHeight': 1364,
            'phase': 'hold', 'orientation': 'portrait', 'nativeContext': 'foreground_application',
            'holdDeadlineEpochMilliseconds': 11000}
        buffer = BytesIO(); Image.new('RGB', (900, 1500)).save(buffer, format='PNG'); png = buffer.getvalue()
        receipt = {'schemaVersion': 1, 'before': row, 'after': row, 'beforeReadEpochMilliseconds': 1000,
            'afterReadEpochMilliseconds': 1001, 'pngSha256': sha256(png).hexdigest(),
            'pngBytes': len(png), 'capture': 'private_xctest_screen', 'orientation': 'portrait', 'attachments': False}
        provenance = {'sourceSha256': 'a' * 64, 'referenceSha256': 'b' * 64,
            'referencePngSha256': 'c' * 64, 'normalizedPixelEquality': True}
        result = qualify_capture(png, receipt, binding, provenance)
        self.assertEqual(result['quality'], 'human_verdict_pending')
        self.assertIsNone(result['lettering']); self.assertIsNone(result['qualityQualifiedSavings'])
        with self.assertRaises(EntryError):
            qualify_capture(png, {**receipt, 'after': {**row, 'sessionSha256': '9' * 64}}, binding, provenance)
        with self.assertRaises(EntryError):
            qualify_capture(png, receipt, binding, {**provenance, 'normalizedPixelEquality': False})
        with self.assertRaises(EntryError): qualify_capture(png + b'x', receipt, binding, provenance)

    def test_every_target_event_requires_each_producer_binding_field(self):
        binding, rows, _ = self.fixture()
        for index, row in enumerate(rows):
            target = row['event'] in ('request', 'body', 'encoded_verified', 'decoded', 'draw', 'layout', 'cache_state', 'ownership')
            required = ('generation', 'caseSha256', 'providerSha256', 'attemptSha256') if target else (
                ('generation', 'sessionSha256', 'caseSha256') if row['event'] == 'dispatch' else
                ('generation', 'sessionSha256') if row['event'] in ('phase', 'memory_sample', 'release_complete') else ())
            for key in required:
                changed = deepcopy(rows); changed[index].pop(key)
                with self.subTest(event=row['event'], key=key), self.assertRaises(EntryError): self.qualified(rows=changed)

    def test_duplicate_target_events_requests_body_truncation_and_retries_refuse(self):
        _, rows, _ = self.fixture()
        for event in ('dispatch', 'encoded_verified', 'decoded', 'layout', 'release_complete', 'request'):
            changed = deepcopy(rows)
            index = next(index for index, row in enumerate(changed) if row['event'] == event)
            changed.insert(index, deepcopy(changed[index]))
            with self.subTest(event=event), self.assertRaises(EntryError): self.qualified(rows=changed)
        for event, key, value in (('request', 'retryCount', 1), ('request', 'requestCount', True),
            ('body', 'bodyBytes', 669607), ('body', 'bodyBytes', True)):
            changed = deepcopy(rows); next(row for row in changed if row['event'] == event)[key] = value
            with self.assertRaises(EntryError): self.qualified(rows=changed)
        changed = deepcopy(rows)
        body_index = next(index for index, row in enumerate(changed) if row['event'] == 'body')
        changed.insert(body_index, {**changed[body_index], 'event': 'status_body', 'bodyBytes': 12})
        self.assertEqual(self.qualified(rows=changed)['fixtureBodyBytes'], 669608)
        changed = deepcopy(rows)
        draw_index = next(index for index, row in enumerate(changed) if row['event'] == 'draw')
        changed.insert(draw_index, deepcopy(changed[draw_index]))
        self.assertEqual(self.qualified(rows=changed)['dispatchToTargetPaintMicroseconds'], 5)

    def test_paired_geometry_decode_counter_scope_and_settings_must_match(self):
        left = {**self.qualified(), 'source': 71, 'repetition': 1, 'arm': 'jpeg'}
        right = {**deepcopy(left), 'arm': 'webp', 'sessionSha256': '9' * 64}
        self.assertEqual(paired_differences([left, right])[0]['denominator'], 1)
        for key, value in (('layout', {**right['layout'], 'observedDpr': 2}),
            ('decode', {**right['decode'], 'actualDecodeWidth': 839}), ('scope', 'other'),
            ('purpose', 'other'), ('artifactSha256', '9' * 64),
            ('comparisonIdentity', {**right['comparisonIdentity'], 'settingsSha256': '9' * 64}),
            ('memory', {**right['memory'], 'counter': 'android_total_pss'})):
            with self.subTest(key=key), self.assertRaises(EntryError): paired_differences([left, {**right, key: value}])

    def test_capture_types_phase_context_hold_expiry_and_unexpected_fields_refuse(self):
        from io import BytesIO
        from PIL import Image
        binding, _, _ = self.fixture()
        row = {**binding, 'observedSlotWidth': 280, 'observedSlotHeight': 460, 'observedDpr': 3,
            'actualDecodeWidth': 840, 'actualDecodeHeight': 1364, 'phase': 'hold', 'orientation': 'portrait',
            'nativeContext': 'foreground_application', 'holdDeadlineEpochMilliseconds': 11000}
        buffer = BytesIO(); Image.new('RGB', (900, 1500)).save(buffer, format='PNG'); png = buffer.getvalue()
        receipt = {'schemaVersion': 1, 'before': row, 'after': row, 'beforeReadEpochMilliseconds': 1000,
            'afterReadEpochMilliseconds': 1001, 'pngSha256': sha256(png).hexdigest(), 'pngBytes': len(png),
            'capture': 'private_xctest_screen', 'orientation': 'portrait', 'attachments': False}
        provenance = {'sourceSha256': 'a' * 64, 'referenceSha256': 'b' * 64, 'referencePngSha256': 'c' * 64, 'normalizedPixelEquality': True}
        for key, value in (('schemaVersion', True), ('pngBytes', True), ('beforeReadEpochMilliseconds', True),
            ('afterReadEpochMilliseconds', 11000), ('orientation', 'landscape')):
            with self.subTest(key=key), self.assertRaises(EntryError): qualify_capture(png, {**receipt, key: value}, binding, provenance)
        for key, value in (('generation', False), ('actualDecodeWidth', True), ('observedDpr', True),
            ('phase', 'releasing'), ('nativeContext', 'background_application'), ('holdDeadlineEpochMilliseconds', True),
            ('unknown', 'secret')):
            changed = {**row, key: value}
            with self.subTest(key=key), self.assertRaises(EntryError):
                qualify_capture(png, {**receipt, 'before': changed, 'after': changed}, binding, provenance)

    def test_android_binding_reader_waits_for_new_native_observation_and_refuses_clear(self):
        import threading
        import time
        binding, _, _ = self.fixture()
        reader = AndroidQualityBindingReader(lambda: binding)
        row = {**binding, 'observedSlotWidth': 280, 'observedSlotHeight': 460, 'observedDpr': 3,
            'actualDecodeWidth': 840, 'actualDecodeHeight': 1364, 'phase': 'hold', 'orientation': 'portrait',
            'nativeContext': 'foreground_activity', 'holdDeadlineEpochMilliseconds': 11000, 'nativeObservedAtEpochMilliseconds': 1000}
        def feed(value):
            encoded = base64.b64encode(frozen_json(value))
            for line in (b'INSTRUMENTATION_STATUS: event=quality-binding', b'INSTRUMENTATION_STATUS: bindingBase64=' + encoded,
                b'INSTRUMENTATION_STATUS_CODE: 3'): reader.feed(line)
        feed(row)
        def publish(value):
            time.sleep(.02); feed(value)
        worker = threading.Thread(target=publish, args=({**row, 'nativeObservedAtEpochMilliseconds': 1001},))
        worker.start(); self.assertEqual(reader.read()['nativeObservedAtEpochMilliseconds'], 1001); worker.join()
        worker = threading.Thread(target=publish, args=(None,))
        worker.start()
        with self.assertRaises(EntryError): reader.read()
        worker.join(); reader.close()
        with self.assertRaises(EntryError): reader.read()
        with self.assertRaises(EntryError): feed({**row, 'sessionSha256': '0' * 64})

    def test_android_audit_exact_schema_size_signature_semantics_and_live_apk_hash(self):
        import tempfile
        import time
        from pathlib import Path
        from native_entry_config import write_private
        with tempfile.TemporaryDirectory(dir=Path(tempfile.gettempdir()).resolve()) as temporary:
            base = Path(temporary)
            app, launcher, build_path, audit_path = (base / name for name in ('app.apk', 'test.apk', 'build.json', 'audit.json'))
            write_private(app, b'app'); write_private(launcher, b'launcher'); write_private(build_path, b'{}')
            build = {'platform': 'android', 'mode': 'release', 'sourceSha256': 'a' * 64,
                'artifactSha256': sha256(b'app').hexdigest(), 'testArtifactSha256': sha256(b'launcher').hexdigest(),
                'artifactPath': str(app), 'testArtifactPath': str(launcher),
                'expectedAppId': 'com.tellpal.tellpal_mobile_poc.imageexperiment.release',
                'testAppId': 'com.tellpal.tellpal_mobile_poc.imageexperiment.release.test', 'certificateSha256': 'b' * 64}
            artifacts = []
            for identity, digest, size in ((build['expectedAppId'], build['artifactSha256'], 3),
                (build['testAppId'], build['testArtifactSha256'], 8)):
                artifacts.append({'bundleId': identity, 'observedSha256': digest, 'certificateSha256': build['certificateSha256'],
                    'certificateBinding': 'host_verified_signature_and_exact_full_device_apk_equivalence',
                    'independentDeviceCertificateExtraction': False, 'fullDeviceApkPullPerformed': False,
                    'deviceHashMethod': 'toybox_sha256sum', 'regularFileMetadataStable': True, 'observedByteCount': size})
            audit = {'scope': 'image-optimization-actual-android-apk-readback', 'observedAt': int(time.time() * 1000),
                'buildReceiptSha256': sha256(b'{}').hexdigest(), 'artifacts': artifacts,
                'sourceSha256': build['sourceSha256'], 'deviceSelectorSha256': 'c' * 64}
            proof = {'deviceSelectorSha256': 'c' * 64, 'installedArtifactSha256': build['artifactSha256'],
                'installedTestArtifactSha256': build['testArtifactSha256']}
            def check(value, changed_build=build):
                audit_path.write_bytes(frozen_json(value)); audit_path.chmod(0o600)
                selected = {**proof, 'installationReceiptSha256': sha256(audit_path.read_bytes()).hexdigest()}
                return validate_comparison_audit(audit_path, build_path, changed_build, selected, 'android')
            self.assertEqual(check(audit), sha256(audit_path.read_bytes()).hexdigest())
            for key, value in (('observedAt', True), ('deviceSelectorSha256', 'd' * 64), ('unexpected', 'secret')):
                with self.subTest(key=key), self.assertRaises(EntryError): check({**audit, key: value})
            for key, value in (('observedByteCount', 2), ('observedByteCount', True),
                ('certificateBinding', 'independently_extracted'), ('independentDeviceCertificateExtraction', True),
                ('fullDeviceApkPullPerformed', True), ('unknown', True)):
                changed = deepcopy(audit); changed['artifacts'][0][key] = value
                with self.subTest(key=key), self.assertRaises(EntryError): check(changed)
            with self.assertRaises(EntryError): check(audit, {**build, 'mode': 'profile'})
            app.write_bytes(b'new')
            with self.assertRaises(EntryError): check(audit)

    def test_quality_short_hold_late_load_and_outside_release_bound_refuse(self):
        binding, rows, outcome = self.fixture()
        rows = [row for row in rows if row.get('event') != 'memory_sample' and not (
            row.get('event') == 'phase' and row.get('phase') in ('baseline', 'post_release'))]
        def check(selected):
            return qualify(frozen_json({'schemaVersion': 1, 'records': selected, 'droppedCount': 0}),
                binding, outcome, platform='ios', purpose=QUALITY, expected_provider=self.expected_provider())
        self.assertEqual(check(rows)['purpose'], QUALITY)
        shortened = deepcopy(rows)
        for row in shortened:
            if row['monotonicMicroseconds'] >= 40000008: row['monotonicMicroseconds'] -= 1
        with self.assertRaisesRegex(EntryError, '^phase_duration_rejected$'): check(shortened)
        late = deepcopy(rows)
        for row in late:
            if row.get('phase') == 'load': row['monotonicMicroseconds'] = 20000000
        with self.assertRaisesRegex(EntryError, '^phase_duration_rejected$'): check(late)
        outside = deepcopy(rows)
        releasing = next(row for row in outside if row.get('phase') == 'releasing')
        outside.remove(releasing)
        release_index = next(index for index, row in enumerate(outside) if row.get('event') == 'release_complete')
        releasing['monotonicMicroseconds'] = outside[release_index]['monotonicMicroseconds'] + 1
        outside.insert(release_index + 1, releasing)
        with self.assertRaisesRegex(EntryError, '^phase_duration_rejected$'): check(outside)

    def test_android_incomplete_pair_has_zero_denominator_no_quantile(self):
        result = paired_differences([{'platform': 'android', 'source': 71, 'repetition': 1, 'arm': 'jpeg', 'status': 'failed'}])
        self.assertEqual(result[0]['denominator'], 0); self.assertIsNone(result[0]['quantile'])


import math
if __name__ == '__main__': unittest.main()
