"""Verilen QA kaynak örneklerini doğrular; çalıştırma veya fiziksel ölçüm yapmaz."""

import re


class ResourceObservationError(ValueError):
    """Ret nedeni ve konumu makine tarafından okunabilir kalır."""

    def __init__(self, code, path):
        self.code = code
        self.path = path
        super().__init__(f"{code}: {path}")


_ACTIVE = ("activeBuffers", "activeDescriptors", "activeCodecs", "activeImages")
_DECIMAL = re.compile(r"(?:0|[1-9][0-9]*)\Z", re.ASCII)
_LIFETIME = re.compile(r"[A-Za-z0-9_-]{1,128}\Z", re.ASCII)


def _require(condition, code, path):
    if not condition:
        raise ResourceObservationError(code, path)


def _integer(value, path, positive=False):
    _require(type(value) is int and value >= (1 if positive else 0), "integer", path)
    return value


def _decimal(value, path):
    _require(type(value) is str and _DECIMAL.fullmatch(value), "decimal", path)
    # Python'ın metinden int dönüşümündeki basamak sınırına bağlı kalmaz.
    result = 0
    for digit in value:
        result = result * 10 + ord(digit) - ord("0")
    return result


def _decimal_text(value):
    # Her parça en fazla dokuz basamaktır; global interpreter ayarı değişmez.
    parts = []
    while value >= 1_000_000_000:
        value, remainder = divmod(value, 1_000_000_000)
        parts.append(str(remainder).zfill(9))
    return str(value) + "".join(reversed(parts))


def _loaded_dimensions(sample, path):
    if "decodedWidth" in sample or "decodedHeight" in sample:
        return sample
    # Snapshot/paint mevcut biçimde boyutları son state olayında taşır.
    events = sample.get("events")
    _require(type(events) is list, "loaded_dimensions", path)
    states = [event for event in events if type(event) is dict and event.get("kind") == "state"]
    _require(bool(states) and states[-1].get("status") == "loaded", "loaded_dimensions", path)
    return states[-1]


def validate_resource_observations(samples):
    """Tek isolate dizisinden yalnız gözlenen rezervasyon ve sayaç farkını döndürür.

    Legacy kabulünü yeniden değerlendirmez. Boş/karma dizi veya bozuk v1 iddiası
    tipli ret verir. Dönen maksimum örnekler arası peak/RSS/GPU kanıtı değildir.
    """
    _require(type(samples) is list and bool(samples), "samples", "samples")
    for index, sample in enumerate(samples):
        _require(type(sample) is dict, "sample", f"samples[{index}]")
    present = ["resourceObservation" in sample for sample in samples]
    if not any(present):
        return {"schema": "legacy", "measured": False}
    _require(all(present), "mixed_schema", "samples")
    lifetime = None
    baseline = previous = None
    maximum_slots = maximum_bytes = 0
    for index, sample in enumerate(samples):
        path = f"samples[{index}]"
        observation = sample["resourceObservation"]
        _require(type(observation) is dict, "observation", path)
        version = observation.get("version")
        _require(type(version) is int and version == 1, "version", path)
        current_lifetime = observation.get("lifetimeId")
        _require(type(current_lifetime) is str and _LIFETIME.fullmatch(current_lifetime), "lifetime", path)
        if lifetime is None:
            lifetime = current_lifetime
        _require(lifetime == current_lifetime, "lifetime_changed", path)
        slots = _integer(observation.get("reservedSlots"), path + ".reservedSlots")
        _require(slots in (0, 1), "slots", path)
        decoded = _decimal(observation.get("reservedDecodedBytes"), path + ".reservedDecodedBytes")
        attempts = _decimal(observation.get("bufferCreationAttempts"), path + ".bufferCreationAttempts")
        _require((slots == 0 and decoded == 0) or (slots == 1 and decoded > 0), "slot_bytes", path)
        active = {key: _integer(sample.get(key), path + "." + key) for key in _ACTIVE}
        if baseline is None:
            baseline = attempts
        _require(previous is None or attempts >= previous, "attempts_decreased", path)
        previous = attempts
        if sample.get("status") == "loaded":
            dimensions = _loaded_dimensions(sample, path)
            width = _integer(dimensions.get("decodedWidth"), path + ".decodedWidth", True)
            height = _integer(dimensions.get("decodedHeight"), path + ".decodedHeight", True)
            _require(active == dict.fromkeys(_ACTIVE, 0) | {"activeImages": 1}
                     and slots == 1 and decoded == width * height * 4, "loaded_resources", path)
        if sample.get("stopped") is True or sample.get("drained") is True:
            _require(not any(active.values()) and slots == 0 and decoded == 0, "drain", path)
        maximum_slots = max(maximum_slots, slots)
        maximum_bytes = max(maximum_bytes, decoded)
    return {
        "schema": "v1", "measured": True, "lifetimeId": lifetime,
        "sampleCount": len(samples), "baselineBufferCreationAttempts": _decimal_text(baseline),
        "finalBufferCreationAttempts": _decimal_text(previous), "bufferCreationAttemptsDelta": _decimal_text(previous - baseline),
        "maxObservedReservedSlots": maximum_slots,
        "maxObservedReservedDecodedBytes": _decimal_text(maximum_bytes),
    }
