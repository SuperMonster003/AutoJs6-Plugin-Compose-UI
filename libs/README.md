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
  the classes. The F.2 `api.interop` package is negotiated separately from frozen V1.

Current provenance: `common-plugin-api.aar` is the release AAR of the AutoJs6 6.8.0 snapshot `77b5a3b0c5` (build 5307,
2026-10-02); the module is unchanged since host commit `9c3ba2e520` (2026-09-15), so the file is byte-identical to the
artifact staged by the other official plugins.

`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock entries and digest
mismatches during configuration. Do not commit locally assembled debug AARs or rename debug outputs to bypass this
policy. Record the lowercase SHA-256 of every staged artifact in the lock file; licenses are listed in
`../THIRD_PARTY_NOTICES.md`.

Prior F.2 `compose-ui-api.aar`: built with `:plugin-api:compose-ui-api:assembleRelease` on host branch
`spike/compose-ui-p0` for F.2 integration (6.8.0 / 5320), source commit
`d54f21b1ea408dfcb66e58f663d71d2f10fade8a`. SHA-256
`0aaff93a27d405e8db7a513172ba1a2ebe60a3b8a09d4f27d8c9eaa03544d4a3`.
Compared with the P1.3 artifact `3ba7c215262e889034eef61e6ba0d5414839712a03284e3d009a96696cce5266`,
all 197 retained V1 class files are byte-identical. Nine classes add the optional AndroidView
factory/renderer, component catalog, process-local bindings and shared weak ownership policy.
The seven unused negative-version `api.spike` fixture classes have been removed as scheduled.
No V1 Parcelable field, ValueKind, loading interface or component catalog entry changes.

The host uses `implementation(project(":plugin-api:compose-ui-api"))` in every variant. The
51-item shared dependency fingerprint is unchanged. Basic renderer compatibility remains host
5316; F.2 script APIs require the matching 5320 integration. The base factory keeps V1 capabilities
and only offers the extension by feature identifier and class-name string, so old hosts do not
need to load the new shared types. The exact implementation commit and compatibility evidence
are recorded in `../docs/dev/p7-interop-evidence.md`.
See `docs/dev/compose-ui-plugin-protocol-v1.md` for the frozen surface and BitmapRef transport limits.

## Prior F.3 dialog artifact

The prior F.3 release artifact added the separately negotiated `dialog-v1` factory and
options in `api.dialog`, from the matching 6.8.0 / 5321 host integration at commit
`8cba2ce0e3aad5835879b0b91604caec9cb719b1`. SHA-256
`e3a4f2003bb0336338e229cfc9220c2296a5de462957fd540a20c37d86996fe8`. All 206 prior V1 and F.2 class files remain byte-identical;
three dialog classes are added, without changing the shared dependency lock or base
minimum host version. See `../docs/dev/p7-dialog-aar-compatibility.json` for the exact
class inventory and `../docs/dev/p7-dialog-evidence.md` for final source identity.

## F.4 V2 release artifact

The current compose-ui-api.aar is the host release module for 6.8.0 / 5322.
SHA-256: `3ebe1f887b7e3a9a98277bd0436e83e8e808b380cbf050d37da42975ea125351`. API source is the clean host commit
`6a166c38415e7299ba034cd062dcc4c51a4b543a`; the API module was assembled with AGP
9.4.0 before the concurrent platform upgrade. Its sources and the staged AAR
were unchanged by that upgrade. Final host generation uses the same source at
AGP 9.4.1. See `docs/dev/p7-wide-evidence.md` at the repository root. The MPL-2.0
license and the 51 shared dependency entries are unchanged. The artifact adds
25 component definitions and shared V2 validation. All 208 previous model, catalog,
loading and optional extension class files are byte-identical; only
ComposeUiContract changes its maximum supported version to 2. The V1 minimum
is still 1, and its frozen catalog remains 30 entries. See
`docs/dev/p7-wide-aar-compatibility.json` for the complete class comparison.
