# Landing-time corrections

2026-10-08. The final report avoids two overbroad digest statements identified by the fresh verifier:
- precacheImage removes its listener immediately on error; only successful first-image availability defers removal to frame end.
- ImageCache clear prose and upstream pending-listener removal differ; automatic later reinsertion is not a guaranteed behavior. Cache bookkeeping also does not prove network cancellation.

See verification.md V3/V5. The report did not assert either universal behavior. Geometry tables use raw fractional physical extents; integer decode dimensions are rounded upward. Single-publisher API findings remain explicitly unverified by an independent publisher,even when the primary contract is clear.
