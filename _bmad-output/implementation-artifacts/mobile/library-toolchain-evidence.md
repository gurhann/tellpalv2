# Story 1.1 toolchain probe

Date: 2026-10-02. Status: successful isolated dependency/generator/analyzer probe; native platform compilation and application acceptance pending.

The local SDK is Flutter3.41.1 stable, Dart3.11.0. The repository remains on its current SDK constraint. No SDK upgrade or application dependency change was made while planning.

A throwaway package at /private/tmp/tellpal-library-toolchain.ABxFf6 resolved the required architecture dependencies together with the existing just_audio dependency. The retained toolchain-probe/pubspec.lock is the exact resolution, not a promise that every future range resolves identically.

| Package | Resolved version |
| --- | --- |
| flutter_bloc / bloc | 9.1.1 / 9.2.1 |
| auto_route / auto_route_generator | 10.3.0 / 10.3.1 |
| get_it | 8.3.0 |
| injectable / injectable_generator | 2.7.1+4 / 2.12.1 |
| freezed / freezed_annotation | 3.2.5 / 3.1.0 |
| json_serializable / json_annotation | 6.13.0 / 4.11.0 |
| build_runner / analyzer | 2.15.1 / 9.0.0 |
| dartz / equatable | 0.10.1 / 2.1.0 |
| cached_network_image / flutter_svg | 3.4.1 / 2.3.0 |
| just_audio | 0.10.6 |

The probe exercised JSON serialization, sealed Freezed state, an injectable Cubit/composition root and auto_route generation. Dependency resolution passed; seven generated outputs were written; dart analyze reported no issues. Retain the small source and generated files alongside the lock for repeatability. Verify the versions against that lock when installing; the lock is authoritative over this summary table.

The local tool used the SDK's direct Dart binary with FLUTTER_ROOT set because the Flutter wrapper attempted to write engine.stamp outside the planning sandbox. This does not establish native build success. build_runner2.15.1 no longer requires/supports the old --delete-conflicting-outputs workflow flag; plain build succeeded. json_annotation must satisfy the tested ≥4.11.0 generator requirement.

During implementation, merge required dependencies into app/mobile/pubspec.yaml and commit the resulting application lock. Regenerate and analyze in the actual app, retain existing PoC tests, and then prove native AndroidAPI24/iOS15 builds and virtual-device/backend smoke. Do not copy the probe's package name into the product.
