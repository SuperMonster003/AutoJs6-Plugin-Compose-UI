# Staged AAR artifacts

This directory contains the exact, hash-locked binary artifacts consumed by the plugin. Gradle never
resolves them from sibling repositories or from `mavenLocal()`.

## Host protocol AARs (`libs/`, lock `../locks/host-api-aars.lock`)

Stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`,
  `PluginCapabilityKeys`), consumed with `implementation`.
- `compose-ui-api.aar` (host module `plugin-api/compose-ui-api`): frozen V1 loading interfaces,
  immutable Parcelable models, the 30-entry catalog and centralized vocabulary. Consumed with
  `compileOnly`, and `testImplementation` / `androidTestImplementation` for tests. The host supplies
  the classes. The negative-version `api.spike` regression fixture is outside the frozen V1 surface.

Current provenance: `common-plugin-api.aar` is the release AAR of the AutoJs6 6.8.0 snapshot `77b5a3b0c5` (build 5307,
2026-10-02); the module is unchanged since host commit `9c3ba2e520` (2026-09-15), so the file is byte-identical to the
artifact staged by the other official plugins.

`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock entries and digest
mismatches during configuration. Do not commit locally assembled debug AARs or rename debug outputs to bypass this
policy. Record the lowercase SHA-256 of every staged artifact in the lock file; licenses are listed in
`../THIRD_PARTY_NOTICES.md`.

V1 `compose-ui-api.aar`: built with `:plugin-api:compose-ui-api:assembleRelease` on host branch
`spike/compose-ui-p0` (6.8.0 / 5316, P1.3 integration commit recorded in `docs/dev/p13-p2-evidence.md`).
SHA-256 `3ba7c215262e889034eef61e6ba0d5414839712a03284e3d009a96696cce5266`.
The host now uses `implementation(project(":plugin-api:compose-ui-api"))` in every variant. The
shared dependency fingerprint is unchanged. P1.3 changes only the API deployment-floor metadata,
with the new staged artifact digest recorded above. P1.2 now uses the V1 loading surface
and the implemented P2 basic/interactive renderer. Legacy api.spike types have no remaining implementation
references and will be removed at the next artifact maintenance. The final minimum host version is 5316;
the P2 renderer and P3 UI/floating script APIs require the matching local host build.
See `docs/dev/compose-ui-plugin-protocol-v1.md` for the frozen surface and BitmapRef transport limits.
