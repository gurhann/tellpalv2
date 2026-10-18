"""Kaynak JSON sözleşmesi ve gerçek Dart çıktısının saf doğrulaması."""

import copy
import json
import os
from pathlib import Path
import unittest

from p4_resource_observation import ResourceObservationError, validate_resource_observations


def sample(slots=0, decoded="0", attempts="7", **fields):
    return {
        "activeBuffers": 0, "activeDescriptors": 0, "activeCodecs": 0, "activeImages": 0,
        "resourceObservation": {"version": 1, "lifetimeId": "isolate_a", "reservedSlots": slots,
                                "reservedDecodedBytes": decoded, "bufferCreationAttempts": attempts},
        **fields,
    }


class ResourceObservationTest(unittest.TestCase):
    def reject(self, samples, code):
        with self.assertRaises(ResourceObservationError) as caught:
            validate_resource_observations(samples)
        self.assertEqual(caught.exception.code, code)

    def test_legacy_has_no_fabricated_measurement_or_delta(self):
        self.assertEqual(validate_resource_observations([{}, {"oldSuccess": True}]),
                         {"schema": "legacy", "measured": False})

    def test_claimed_v1_null_missing_unknown_and_mixed_refused(self):
        for value in (None, False, [], 1):
            self.reject([{"resourceObservation": value}], "observation")
        valid = sample()
        for key in valid["resourceObservation"]:
            with self.subTest(missing=key):
                broken = copy.deepcopy(valid)
                del broken["resourceObservation"][key]
                self.reject([broken], {"version": "version", "lifetimeId": "lifetime",
                                      "reservedSlots": "integer"}.get(key, "decimal"))
        for version in (0, 2, True, 1.0, "1", None):
            broken = sample()
            broken["resourceObservation"]["version"] = version
            self.reject([broken], "version")
        self.reject([{}, sample()], "mixed_schema")
        self.reject([sample(), {}], "mixed_schema")

    def test_decimal_type_and_canonical_format_matrix(self):
        for field in ("reservedDecodedBytes", "bufferCreationAttempts"):
            for value in (None, True, False, 0, 1.0, -1, "", "-1", "+1", " 1", "1 ", "1\n",
                          "1e2", "1.0", "00", "01", "١", "１２"):
                with self.subTest(field=field, value=value):
                    broken = sample()
                    broken["resourceObservation"][field] = value
                    self.reject([broken], "decimal")

    def test_exact_integer_slots_active_dimensions_matrix(self):
        for field in ("reservedSlots", "activeBuffers", "activeDescriptors", "activeCodecs", "activeImages"):
            for value in (None, True, 0.0, -1, "0"):
                with self.subTest(field=field, value=value):
                    broken = sample()
                    target = broken["resourceObservation"] if field == "reservedSlots" else broken
                    target[field] = value
                    self.reject([broken], "integer")
        self.reject([sample(2, "16")], "slots")
        for field in ("decodedWidth", "decodedHeight"):
            for value in (None, True, 2.0, 0, -1, "2"):
                broken = sample(1, "16", activeImages=1, status="loaded", decodedWidth=2, decodedHeight=2)
                broken[field] = value
                self.reject([broken], "integer")

    def test_pending_and_transfer_are_not_equated_with_visible_images(self):
        result = validate_resource_observations([sample(), sample(1, "16", "8", status="loading"),
                                                 sample(attempts="8", stopped=True)])
        self.assertEqual(result["maxObservedReservedSlots"], 1)
        self.assertEqual(result["maxObservedReservedDecodedBytes"], "16")
        self.assertEqual(result["bufferCreationAttemptsDelta"], "1")

    def test_slot_byte_consistency(self):
        for slots, decoded in ((0, "1"), (1, "0")):
            self.reject([sample(slots, decoded)], "slot_bytes")

    def test_loaded_geometry_and_handles(self):
        loaded = sample(1, "16", activeImages=1, status="loaded", decodedWidth=2, decodedHeight=2)
        self.assertTrue(validate_resource_observations([loaded])["measured"])
        snapshot = copy.deepcopy(loaded)
        del snapshot["decodedWidth"], snapshot["decodedHeight"]
        snapshot["events"] = [{"kind": "state", **loaded}]
        self.assertTrue(validate_resource_observations([snapshot])["measured"])
        for fields in ({"activeImages": 0}, {"activeBuffers": 1}, {"activeDescriptors": 1},
                       {"activeCodecs": 1}, {"decodedWidth": 3}):
            self.reject([{**loaded, **fields}], "loaded_resources")
        self.reject([sample(status="loaded")], "loaded_dimensions")

    def test_drain_requires_all_handles_and_reservation(self):
        for claim in ("stopped", "drained"):
            for field in ("activeBuffers", "activeDescriptors", "activeCodecs", "activeImages"):
                self.reject([sample(**{claim: True, field: 1})], "drain")
            self.reject([sample(1, "16", **{claim: True})], "drain")
            self.assertTrue(validate_resource_observations([sample(**{claim: True})])["measured"])

    def test_nonzero_baseline_large_lossless_delta_and_monotonic_order(self):
        baseline = 10 ** 40 + 7
        report = validate_resource_observations([sample(attempts=str(baseline)),
                                                sample(1, str(10 ** 50), str(baseline + 2)),
                                                sample(attempts=str(baseline + 2), drained=True)])
        self.assertEqual(report["baselineBufferCreationAttempts"], str(baseline))
        self.assertEqual(report["bufferCreationAttemptsDelta"], "2")
        self.assertEqual(report["maxObservedReservedDecodedBytes"], str(10 ** 50))
        self.reject([sample(attempts="8"), sample(attempts="7")], "attempts_decreased")

    def test_lifetime_changes_refused_and_separate_sequence_has_own_baseline(self):
        other = sample(attempts="2")
        other["resourceObservation"]["lifetimeId"] = "isolate_b"
        self.reject([sample(), other], "lifetime_changed")
        self.assertEqual(validate_resource_observations([other])["bufferCreationAttemptsDelta"], "0")
        for value in (None, True, 1, "", "with space", "https://secret", "a" * 129):
            broken = sample()
            broken["resourceObservation"]["lifetimeId"] = value
            self.reject([broken], "lifetime")

    def test_more_than_4300_digits_remain_lossless_without_global_settings(self):
        import sys
        limit_before = sys.get_int_max_str_digits()
        baseline = "1" + "0" * 4500
        final = "1" + "0" * 4499 + "2"
        decoded = "9" * 5000
        report = validate_resource_observations([sample(attempts=baseline), sample(1, decoded, final)])
        self.assertEqual(report["baselineBufferCreationAttempts"], baseline)
        self.assertEqual(report["finalBufferCreationAttempts"], final)
        self.assertEqual(report["bufferCreationAttemptsDelta"], "2")
        self.assertEqual(report["maxObservedReservedDecodedBytes"], decoded)
        report = validate_resource_observations([sample(attempts="0"), sample(attempts=final)])
        self.assertEqual(report["bufferCreationAttemptsDelta"], final)
        self.assertEqual(sys.get_int_max_str_digits(), limit_before)

    def test_empty_and_nonobject_sequence_refused(self):
        for value in (None, [], {}, "[]"):
            self.reject(value, "samples")
        for value in (None, [], True):
            self.reject([value], "sample")

    @unittest.skipUnless(os.environ.get("P4_RESOURCE_DART_JSON_LOG"), "Özel gerçek Dart log yolu verilmedi")
    def test_real_dart_native_and_loopback_json_reaches_report(self):
        chains = {}
        for line in Path(os.environ["P4_RESOURCE_DART_JSON_LOG"]).read_text().splitlines():
            if "P4_RESOURCE_JSON:" in line:
                exported = json.loads(line.split("P4_RESOURCE_JSON:", 1)[1])
                chains[exported["chain"]] = validate_resource_observations(exported["samples"])
        self.assertEqual(set(chains), {"native-pending", "loopback-codec"})
        for report in chains.values():
            self.assertTrue(report["measured"])
            self.assertEqual(report["maxObservedReservedSlots"], 1)
            self.assertGreater(int(report["bufferCreationAttemptsDelta"]), 0)
        print("P4_RESOURCE_REPORT:" + json.dumps(chains, sort_keys=True))


if __name__ == "__main__":
    unittest.main()
