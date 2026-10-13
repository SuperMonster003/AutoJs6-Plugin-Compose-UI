# Staged AAR artifacts

This directory contains the exact, hash-locked binary artifacts consumed by the plugin. Gradle never
resolves them from sibling repositories or from `mavenLocal()`.

## Host protocol AARs (`libs/`, lock `../locks/host-api-aars.lock`)

Stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`,
  `PluginCapabilityKeys`), consumed with `implementation`.
- `typescript-parser-api.aar` (host module `plugin-api/typescript-parser-api`, P9.2): the TypeScript parser
  contract, consumed with `implementation`; see the section below.
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

## 2026-10-13 rename artifact (3-Block Compose)

The current compose-ui-api.aar is the host release module at host commit
`f12665ffde` (`refactor(plugin): track the renamed 3-Block Compose plugin`),
assembled with `:plugin-api:compose-ui-api:assembleRelease` on the host master
branch. SHA-256: `557b7efc8fd7d17c2032f0a6e0ccb5f64b42d0a2f3dceabacd06c9ce89b352a6`.
Only `ComposeUiIds.PLUGIN_PACKAGE_NAME`
(`io.github.supermonster003.autojs6.plugin.three.block.compose`) and
`ComposeUiIds.PLUGIN_ID` (`three-block-compose`, also the INFO category) change
together with the reviewed V1 snapshot; the module name, the
`org.autojs.plugin.compose.api` package, every class name, `ENGINE`,
`CONTRACT_VERSION = 2` and the shared dependency lock are unchanged. The host
discovers the plugin by `PLUGIN_PACKAGE_NAME`, so the renamed plugin needs a host
build that contains this commit; `REQUIRED_HOST_VERSION` stays 5322 because an
older host never evaluates it for the new package.

## TypeScript parser contract (`typescript-parser-api.aar`, roadmap P9.2 / D42)

`typescript-parser-api.aar` is the release AAR of the host module `plugin-api/typescript-parser-api` at host
commit `fce22c83a6` (`feat(plugin-api): add the TypeScript parser contract`), assembled with
`:plugin-api:typescript-parser-api:assembleRelease`; a rebuild from the same commit is byte-identical.
SHA-256 `c5651a0c779903bde8832af185bc63038d1706d32f3e67069fbcf0e536658519`, MPL 2.0. It holds only the
`ITypeScriptParserPlugin` AIDL stubs and `TypeScriptParserContract` (package
`org.autojs.plugin.typescript.parser`, contract version 1, tree schema revision 1); it has no dependency.
The host defines the contract but does not package the module, so the plugin consumes it with
`implementation` in every process. The provider is the TypeScript Engine plugin 0.7.0 and later (action
`org.autojs.plugin.typescript.PARSER`, PLUGIN permission); the designer binds it to read `.tsx` projects.
The protocol is described in the host document `docs/dev/typescript-parser-plugin-protocol-v1.md`.

## Rhino parser jar (`libs/`, lock `../locks/rhino-jar.lock`, roadmap P8.1)

`org-mozilla-rhino-2_0_0-SNAPSHOT-slim.jar` is the output of the host task `:app:slimRhinoJar`
at host commit `f12665ffde4205d444a11f517b4ce1fa747f2724`: the host's own Rhino 2.0.0-SNAPSHOT build without JLine, the optimizer and
the JFR components, exactly what the host packages and runs. SHA-256 `cebd05fabce9aa78faefd202bfab30521a2bd5b227786cf0619e34eada2d505e`. It is MPL 2.0.
The plugin consumes it with `implementation`; only the designer's `designer/parse` package may
import `org.mozilla.javascript` (`RhinoImportGuardTest`), the parser is used through
`CompilerEnvirons` + `Parser` only and never enters a Rhino `Context`, and R8 keeps nothing of
it, so a release APK carries only the Parser / AST closure the designer reaches. Inside the host
the renderer never references it; the identically named host classes would win parent-first
anyway. Re-stage it only from the host commit recorded in the lock.
