# Staged AAR artifacts

This directory contains the exact, hash-locked binary artifacts consumed by the plugin. Gradle never
resolves them from sibling repositories or from `mavenLocal()`.

## Host protocol AARs (`libs/`, lock `../locks/host-api-aars.lock`)

Stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`,
  `PluginCapabilityKeys`), consumed with `implementation`.
- `compose-ui-api.aar` (host module `plugin-api/compose-ui-api`, roadmap D9): the Compose UI contract (loading interfaces,
  data model, component catalog, constants). P0.2 stages an explicitly experimental `api.spike` draft (version -1), P1.1 will replace it with V1. It is
  consumed with `compileOnly` because the host provides the classes at run time (roadmap D10); `testImplementation` for JVM tests.

Current provenance: `common-plugin-api.aar` is the release AAR of the AutoJs6 6.8.0 snapshot `77b5a3b0c5` (build 5307,
2026-10-02); the module is unchanged since host commit `9c3ba2e520` (2026-09-15), so the file is byte-identical to the
artifact staged by the other official plugins.

`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock entries and digest
mismatches during configuration. Do not commit locally assembled debug AARs or rename debug outputs to bypass this
policy. Record the lowercase SHA-256 of every staged artifact in the lock file; licenses are listed in
`../THIRD_PARTY_NOTICES.md`.

Draft `compose-ui-api.aar`: host branch `spike/compose-ui-p0`, source commit `21dff98f26`, based on `e86186920d` (6.8.0 / 5309),
release task `:plugin-api:compose-ui-api:assembleRelease`, SHA-256 `c39b0cc59834d1b9e98d7b2fdde5b8184eae7ff9fe8ff2d32a843f53a44ca437`.
This AAR contains no Compose dependency. The matching debug host dependency snapshot is
`locks/host-shared-deps.lock`; it is not a released host compatibility guarantee.
