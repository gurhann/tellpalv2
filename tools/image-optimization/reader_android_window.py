"""Finite leases for one operator-owned selected-075 Android allocation.

This module never allocates, extends, cancels or discovers a provider resource.
The private operator retains original ownership and independent watchdog evidence.
"""
from dataclasses import dataclass, field
from hashlib import sha256
import math
import threading
import time

from native_entry_config import HASH, finite_number, private_file, require, strict_json


ORDER = ('reader_resolution_a_r1', 'reader_resolution_075_r1', 'reader_resolution_c_r1')
DEFAULT_PROFILE = 'eight_minutes_r1'
SEVEN_MINUTES_PROFILE = 'seven_minutes_after_terminal_r2'
NINE_MINUTES_PROFILE = 'nine_minutes_matrix_r3'
TWELVE_MINUTES_PROFILE = 'twelve_minutes_matrix_r11'
_PROFILE_LIMITS = {DEFAULT_PROFILE: (470000, 450, 478, 8),
    SEVEN_MINUTES_PROFILE: (418000, 390, 418, 7),
    NINE_MINUTES_PROFILE: (538000, 510, 538, 9),
    TWELVE_MINUTES_PROFILE: (718000, 690, 718, 12)}
_CONSUMED_TERMINAL_LEDGERS = set()
_TERMINAL_CONSUMPTION_LOCK = threading.Lock()
RETAINED_MODELED_UPPER_USD = '9.95523914272981'


@dataclass(frozen=True, eq=False, repr=False)
class SequentialAndroidWindow:
    root_epoch_milliseconds: int
    root_monotonic: float
    reservation_expires_at_milliseconds: int
    reservation_receipt_sha256: str
    allocation_sha256: str
    device_selector_sha256: str
    device_unit_sha256: str
    source_sha256: str
    artifact_sha256: str
    test_artifact_sha256: str
    certificate_sha256: str
    watchdog_receipt_sha256: str
    profile: str = DEFAULT_PROFILE
    prior_terminal_ledger_path: str | None = None
    prior_terminal_ledger_sha256: str | None = None
    prior_whole_root_milliseconds: int | None = None
    current_allocation_name_sha256: str | None = None
    _next: int = field(default=0, init=False)
    _active: object = field(default=None, init=False)
    _failed: bool = field(default=False, init=False)
    _closed: bool = field(default=False, init=False)
    _geometry_sha256: str | None = field(default=None, init=False)
    _state_lock: object = field(default_factory=threading.RLock, init=False, repr=False)

    def __post_init__(self):
        require(type(self.profile) is str and self.profile in _PROFILE_LIMITS,
            'reader_sequential_profile_rejected')
        require(type(self.root_epoch_milliseconds) is int and self.root_epoch_milliseconds > 0 and
            finite_number(self.root_monotonic) and self.root_monotonic > 0 and
            type(self.reservation_expires_at_milliseconds) is int and
            self.root_epoch_milliseconds < self.reservation_expires_at_milliseconds <=
            self.root_epoch_milliseconds + _PROFILE_LIMITS[self.profile][0],
            'reader_sequential_expiry_rejected')
        for name in ('reservation_receipt_sha256', 'allocation_sha256', 'device_selector_sha256',
                'device_unit_sha256', 'source_sha256', 'artifact_sha256', 'test_artifact_sha256',
                'certificate_sha256', 'watchdog_receipt_sha256'):
            require(type(getattr(self, name)) is str and HASH.fullmatch(getattr(self, name)),
                'reader_sequential_identity_rejected')
        if self.profile != SEVEN_MINUTES_PROFILE:
            require(all(value is None for value in (self.prior_terminal_ledger_path,
                self.prior_terminal_ledger_sha256, self.prior_whole_root_milliseconds,
                self.current_allocation_name_sha256)), 'reader_sequential_prior_rejected')
        else:
            self._admit_prior_terminal()

    def _admit_prior_terminal(self):
        require(type(self.prior_terminal_ledger_sha256) is str and HASH.fullmatch(self.prior_terminal_ledger_sha256) and
            type(self.current_allocation_name_sha256) is str and HASH.fullmatch(self.current_allocation_name_sha256) and
            type(self.prior_whole_root_milliseconds) is int and 0 < self.prior_whole_root_milliseconds <= 59000 and
            math.ceil((self.prior_whole_root_milliseconds + 1000) / 60000) == 1,
            'reader_sequential_prior_rejected')
        original = private_file(self.prior_terminal_ledger_path)
        require(sha256(original).hexdigest() == self.prior_terminal_ledger_sha256,
            'reader_sequential_prior_hash_rejected')
        prior = strict_json(original)
        require(type(prior) is dict and prior.get('scope') == 'selected075-authoritative-create-owned-watchdog' and
            prior.get('state') == 'FINISHED' and prior.get('terminalObserved') is True and
            type(prior.get('elapsedMillis')) is int and 0 < prior['elapsedMillis'] <= self.prior_whole_root_milliseconds and
            prior.get('onlySuccessfulPostResponseNameCancelled') is True and
            prior.get('absoluteCleanupActionProcessDeadline') is True and
            prior.get('providerTimesNotCleanupOwnership') is True and
            prior.get('cancelAtRootSeconds') == 450 and prior.get('cleanupBudgetMaximumSeconds') == 28 and
            type(prior.get('nameSha256')) is str and HASH.fullmatch(prior['nameSha256']) and
            prior['nameSha256'] != self.current_allocation_name_sha256,
            'reader_sequential_prior_rejected')
        with _TERMINAL_CONSUMPTION_LOCK:
            require(self.prior_terminal_ledger_sha256 not in _CONSUMED_TERMINAL_LEDGERS,
                'reader_sequential_prior_replayed')
            _CONSUMED_TERMINAL_LEDGERS.add(self.prior_terminal_ledger_sha256)

    @property
    def limits_receipt(self):
        prior_minutes = 1 if self.profile == SEVEN_MINUTES_PROFILE else 0
        current_only = self.profile in (NINE_MINUTES_PROFILE, TWELVE_MINUTES_PROFILE)
        expiry_ms, cancel, cleanup, new_minutes = _PROFILE_LIMITS[self.profile]
        return {'profile': self.profile, 'maximumProviderExpiryRootMilliseconds': expiry_ms,
            'cancelAtRootSeconds': cancel, 'hardCleanupRootSeconds': cleanup,
            'priorTerminalLedgerSha256': self.prior_terminal_ledger_sha256,
            'priorWholeRootMilliseconds': self.prior_whole_root_milliseconds,
            'priorNewAllocationBillableMinutesRetained': None if current_only else prior_minutes,
            'currentAllocationMaximumBillableMinutes': new_minutes,
            'totalNewAllocationBillableMinutesUpper': None if current_only else prior_minutes + new_minutes,
            'limitScope': 'current_allocation_only' if current_only else 'retained_profile_aggregate',
            'retainedModeledUpperUsd': None if current_only else RETAINED_MODELED_UPPER_USD,
            'historicalTenUsdScopeApplies': not current_only,
            'historicalDebitsAndReserveChanged': False, 'currentInvoice': False}

    @property
    def cancel_monotonic(self):
        return self.root_monotonic + _PROFILE_LIMITS[self.profile][1]

    @property
    def cleanup_monotonic(self):
        return self.root_monotonic + _PROFILE_LIMITS[self.profile][2]

    def _clock(self):
        now = time.monotonic()
        elapsed = now - self.root_monotonic
        require(0 <= elapsed and abs(time.time() * 1000 -
            (self.root_epoch_milliseconds + elapsed * 1000)) <= 2000,
            'reader_sequential_clock_rejected')
        return now

    def enter(self, batch, *, geometry_sha256=None):
        with self._state_lock:
            return self._enter(batch, geometry_sha256=geometry_sha256)

    def _enter(self, batch, *, geometry_sha256):
        now = self._clock()
        require(not self._closed and not self._failed and self._active is None and
            self._next < len(ORDER) and batch == ORDER[self._next],
            'reader_sequential_order_rejected')
        require(geometry_sha256 is None if self._next == 0 else
            geometry_sha256 is not None and geometry_sha256 == self._geometry_sha256,
            'reader_sequential_geometry_rejected')
        provider_expiry = self.root_monotonic + (
            self.reservation_expires_at_milliseconds - self.root_epoch_milliseconds) / 1000
        entry_limit = min(self.cancel_monotonic, provider_expiry - 15)
        require(self.cancel_monotonic - now >= 150 and entry_limit - now >= 150,
            'reader_sequential_entry_refused')
        lease = SequentialAndroidLease(self, batch, now, min(now + 180, entry_limit))
        object.__setattr__(self, '_active', lease)
        object.__setattr__(self, '_next', self._next + 1)  # Refusals consume their slot too.
        return lease

    def finish(self, lease, *, succeeded, geometry_sha256=None):
        with self._state_lock:
            return self._finish(lease, succeeded=succeeded, geometry_sha256=geometry_sha256)

    def _finish(self, lease, *, succeeded, geometry_sha256):
        require(type(lease) is SequentialAndroidLease and self._active is lease and
            type(succeeded) is bool, 'reader_sequential_lease_rejected')
        object.__setattr__(self, '_active', None)
        object.__setattr__(self, '_failed', True)
        timely = self._clock() <= lease.deadline
        if succeeded and lease.batch == ORDER[0]:
            require(type(geometry_sha256) is str and HASH.fullmatch(geometry_sha256),
                'reader_sequential_geometry_rejected')
            object.__setattr__(self, '_geometry_sha256', geometry_sha256)
        object.__setattr__(self, '_failed', not (succeeded and timely))
        require(timely, 'reader_sequential_completion_late')

    def validate_cleanup(self, *, allocation_sha256, terminal_observed):
        """Qualify an original owned cancellation result; never operate on another root."""
        with self._state_lock:
            return self._validate_cleanup(allocation_sha256=allocation_sha256,
                terminal_observed=terminal_observed)

    def _validate_cleanup(self, *, allocation_sha256, terminal_observed):
        require(allocation_sha256 == self.allocation_sha256 and terminal_observed is True and
            self._active is None and self._clock() <= self.cleanup_monotonic,
            'reader_sequential_cleanup_unqualified')
        object.__setattr__(self, '_closed', True)
        return {'allocationSha256': self.allocation_sha256, 'terminalObserved': True,
            'cancelAtRootSeconds': _PROFILE_LIMITS[self.profile][1],
            'hardCleanupRootSeconds': _PROFILE_LIMITS[self.profile][2],
            'automaticRetry': False}


@dataclass(frozen=True, eq=False, repr=False)
class SequentialAndroidLease:
    window: SequentialAndroidWindow
    batch: str
    entered_monotonic: float
    deadline: float

    def require_full_entry_budget(self):
        require(type(self.window) is SequentialAndroidWindow, 'reader_sequential_lease_rejected')
        with self.window._state_lock:
            self._require_full_entry_budget()

    def _require_full_entry_budget(self):
        require(type(self.window) is SequentialAndroidWindow and self.window._active is self and
            min(self.deadline, self.window.cancel_monotonic) - self.window._clock() >= 150,
            'reader_sequential_entry_refused')

    def validate(self, values, build, proof):
        """Called by both prerequisite passes, before any authenticated operation."""
        require(type(self.window) is SequentialAndroidWindow, 'reader_sequential_lease_rejected')
        with self.window._state_lock:
            self._validate(values, build, proof)

    def _validate(self, values, build, proof):
        window = self.window
        now = window._clock()
        require(type(window) is SequentialAndroidWindow and window._active is self and
            not window._closed and not window._failed and self.batch in ORDER and
            values.get('batchId') == self.batch and values.get('platform') == 'android' and
            values.get('mode') == 'release' and self.entered_monotonic <= now < self.deadline and
            0 < self.deadline - self.entered_monotonic <= 180 and
            self.deadline <= window.cancel_monotonic, 'reader_sequential_lease_rejected')
        self.require_full_entry_budget()
        require(proof.get('provider') == 'android_device_streaming' and proof.get('quotaVerified') is True and
            proof.get('deviceSelectorSha256') == window.device_selector_sha256 and
            proof.get('reservationReceiptSha256') == window.reservation_receipt_sha256 and
            type(proof.get('reservationExpiresAt')) is int and
            proof['reservationExpiresAt'] == window.reservation_expires_at_milliseconds and
            type(proof.get('deviceModel')) is str and
            proof.get('deviceModel', '').lower() in ('moto e20', 'motorola moto e20'),
            'reader_sequential_identity_rejected')
        require(build.get('sourceSha256') == window.source_sha256 and all(
            build.get(name) == proof.get(name) == expected for name, expected in (
                ('artifactSha256', window.artifact_sha256),
                ('testArtifactSha256', window.test_artifact_sha256),
                ('certificateSha256', window.certificate_sha256))),
            'reader_sequential_build_rejected')


def validate_lease(lease, values, build, proof):
    require(type(lease) is SequentialAndroidLease, 'reader_sequential_lease_rejected')
    lease.validate(values, build, proof)
