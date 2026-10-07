# P7 F.4 wide components and contract V2

Work dates: 2026-10-07 through 2026-10-08. This implements the existing F.4 item
without adding, splitting or dropping roadmap items. The final renderer and host
device matrices, complete companion artifact checks and owned-device cleanup
pass. F.4 is complete as a local development preview. The implementation and
this evidence are delivered together as the build33 F.4 source commit.

## Contract and delivered behavior

All 19 requested families and six supporting item nodes are implemented:

| Requested components | Supporting item nodes |
| --- | --- |
| NavigationBar, NavigationRail, NavigationDrawer | NavigationBarItem, NavigationRailItem, NavigationDrawerItem |
| TabRow, DropdownMenu, SegmentedButton | Tab, DropdownMenuItem, SegmentedButtonItem |
| ModalBottomSheet, DatePicker, TimePicker | None |
| HorizontalPager, LazyVerticalGrid | None |
| AssistChip, FilterChip, InputChip, Badge, FloatingActionButton | None |
| SearchBar, Tooltip, PullToRefresh | None |

The base catalog has 55 entries: 54 nodes and command-only Snackbar. Negotiated
AndroidView makes 56 entries and 55 node factories. New vocabulary and catalog
entries live in `org.autojs.plugin.compose.api.v2.ComposeUiV2`. V1 retains its
30-entry catalog, model constructors, parcel tags, limits, modifiers and optional
F.2/F.3 interfaces. The authoritative scalar, slot, event and command rules are
documented in [the V2 contract](compose-ui-contract-v2.md).

The V2 renderer requires host 6.8.0 / 5322. The host supports contract versions
1 and 2 and checks the actual provider's component set. Submitting a new node to
an actual old V1 provider fails with UNKNOWN_COMPONENT before replacing the old
page. An older host rejects the V2 package with PLUGIN_INCOMPATIBLE before loading
its new classes. V2 is never advertised as V1.

The release API AAR has 215 classes. Of the 209 prior classes, 208 are
byte-identical; only ComposeUiContract changes, raising the maximum version to 2
while supporting both versions. Six V2 vocabulary/catalog classes are appended,
with no removed class. SHA-256 is
`3ebe1f887b7e3a9a98277bd0436e83e8e808b380cbf050d37da42975ea125351`.
The inventory and final host source commit are recorded in
[p7-wide-aar-compatibility.json](p7-wide-aar-compatibility.json).

Both sides validate complete prospective trees before publication. Required
slots, matching primary item parents, single selection, page/tab indices,
nonnegative padding and bounded grid columns are enforced. Rejected changes
retain the accepted tree, generation, callback ownership and borrowed native
views. New grid and pager nodes retain the frozen UiNode maximum of 2000 children,
and the live tree remains limited to 5000 nodes. No wire limit is raised.

Selection, open, query and refreshing properties remain controlled by the script.
Native handlers enqueue requests through the existing event sink and owning
script dispatcher. They do not invoke JS during composition. Pager.page is an
explicit navigation request when changed; gestures and scrollTo report actual
settled positions through pageChange. Unrelated updates preserve keyed items.
ScrollTo offsets remain physical pixels. Eleven native interactive nodes expose
the existing focus/blur commands, with the original mounted-node and window
readiness checks.

Dates use nullable integral UTC-midnight milliseconds. Supported years are
1583..9999, defaulting to 1900..2100. Locked Material3 uses a legacy calendar on
API 24/25 and a proleptic calendar on newer Android versions; 1583 is the first
complete year after their Gregorian cutover difference. A null selection with a
custom range starts within that range. Time values remain hour 0..23 and minute
0..59. Script callbacks receive positional values, including script null for an
absent date selection.

## Runtime dependencies, build tools and icons

Compose BOM 2026.09.00, runtime/ui/foundation/animation 1.12.1, Material3 1.4.0,
icons-core 1.7.8 and the 51-entry host shared dependency lock are retained.
Dependency insight confirmed activity-compose 1.8.2 was already a Material3
runtime dependency. Its explicit implementation entry exposes the same public
BackHandler to compilation without changing the resolved runtime version. Debug
lint reports that intentional older-version declaration as one warning, alongside
the four existing icon warnings.

A concurrent shared-platform upgrade to 1.9.0 changed the plugin's AGP from 9.3.2
to 9.3.3 and the host's AGP from 9.4.0 to 9.4.1. Kotlin, R8 and the renderer runtime
versions remained the same. These build-tool changes are recorded separately from
the component dependency decision; they are not described as a Compose upgrade.

The F.4/Q2 evaluation retains core icons and caller-owned ImageWrapper/Bitmap
content in component slots. All requested families work with that existing input
surface. The official [Compose resources guide](https://developer.android.com/develop/ui/compose/resources)
also identifies APK size and development build-time costs for
material-icons-extended. This candidate does not add that artifact or copy its
icon sources. Package measurements below are actual signed APK measurements,
without a speculative extended-icon dependency. The four existing graphics-path
libraries remain the complete native library set, with their alignment checks.

## Final renderer artifact and verification

| Property | Final value |
| --- | --- |
| Renderer identity | Compose UI 1.1.0 / build33 |
| Signed APK | autojs6-plugin-compose-ui-v1.1.0-ffee2730.apk |
| APK bytes | 3022152 |
| DEX class definitions | 9031 |
| DEX method references | 53842 |
| APK SHA-256 | 3ece8a8885b0a2b9ab7edf2e1eeaf5aeeeebc3b6a0b3f4936b868da5ae0ab755 |
| Host identity and source | AutoJs6 6.8.0 / 5322, 6a166c38415e7299ba034cd062dcc4c51a4b543a |

The renderer's 70 JVM tests, debug/androidTest builds, lint and signed release gate
pass. Lint has zero errors and five warnings as described above. Signature, CRC32,
shared classpath ownership and the complete native library/alignment checks pass.
The host's 224 selected Compose/core/XML/registration/colors JVM tests and the
shared API's 29 JVM tests pass. Host debug and androidTest APKs assemble.

| Device | Final build33 plugin instrumentation | Host with final signed renderer |
| --- | --- | --- |
| API 24 x86 owned AVD | 64/64 pass, 89.410 s | 29/29 pass, 80.052 s |
| API 35 x86_64 owned AVD | 64/64 pass, 156.174 s | 29/29 pass, 122.827 s |

The final plugin matrix contains 49 retained cases and 15 F.4 renderer cases.
The final host matrix contains nine F.4 scenarios plus 20 retained
interop/dialog/floaty cases. Each final 29-case host result is a single complete
run with the exact artifact above, after the intermediate fixture and behavior
corrections. It is not a union of selected retries. Logs are retained in the
dedicated host worktree under:

- `app/build/compose-f4/plugin-build33-api24.log`
- `app/build/compose-f4/plugin-build33-api35.log`
- `app/build/compose-f4/host-shipped-api24.log`
- `app/build/compose-f4/host-shipped-api35.log`

The committed host evidence predates those final full runs; this plugin evidence
record contains the final artifact's matrix results. Separate compatibility
checks used the actual archived host5321 and renderer31: the old host rejected the
new renderer, while the new host retained the old provider's page/callbacks when
rejecting a V2 node and ran all five unchanged examples successfully.

Catalog projections preserve the V1/F.2 regions and add 25 nodes with 62
properties, for 176 properties in the V2 base projection. The Python catalog
checker has 22 passing regressions. Real TypeScript 5.1.3 and 6.0.3 each pass the
F.1/F.2/F.3/F.4 corpus with 25/31/22/42 expected negative cases and no extra
diagnostics. P6 strict/exact-optional checks and targeted declaration-body checks
with skipLibCheck=false also have zero diagnostics.

## Corrections covered by the final tests

The host getter preserves explicit/default UiValue.Null as script null and an
absent property as undefined. This also repairs the corresponding V1 progress
and color getters without changing their wire models.

DatePicker synchronizes its accepted selected date and displayed month before
publishing a changed backing mode. This prevents a newly visible calendar from
starting with an old lazy-month position. The native input-to-picker cross-month
case passes on both APIs. TimePicker retains bounded pending hour/minute pairs,
so fast edits and older acknowledgements do not lose the other edited field.

Pager normalizes signed and large pixel offsets using Long arithmetic. Before
the first real measure, a pixel command waits for a usable page size; a newer
request or disposal cancels that wait. Grid/pager tests cover accepted same-turn
growth/shrink, keyed moves, real gestures and rendered command positions.

ModalBottomSheet combines public Compose Dialog and Material3
BottomSheetScaffold/SheetState/scrim APIs, retaining the F.3 overlay approach. A
private renderer window context carries the outer application/overlay type across
popup subpanels. Real host tests cover Activity, overlay, nested dialog and nested
popup sheets. Raw and legacy floaty containers receive a public Back dispatcher
owner and a route for real Back events while preserving explicit native focus,
caller-owned views and listener lifetime. Independent dialog windows use their
own real owner.

Drawer confirmation callbacks also occur during native anchor corrections and
cancelled programmatic animations. The renderer distinguishes those from user
requests and converges interrupted model transitions to the accepted target.
Closing the drawer and immediately expanding SearchBar no longer reopens it.
SearchBar's common click observer preserves the inner editor's focus and native
input behavior.

Native SearchBar text is checked before constructing an event envelope. The
65536 UTF-16-unit boundary is accepted; 65537 units produce a node-specific
LIMIT_EXCEEDED system event with callback0 and prop=query, preserving the accepted
query. The payload contains only code/message/prop, without input text. Disabled
stale native callbacks are ignored. API35 exposed a separate native rollback echo
of the already accepted query. Only unchanged queryChange echoes are suppressed;
search submissions of the same accepted value still run. Final tests cover these
cases and subsequent normal editing/search on both APIs.

Fixture corrections retain the original assertions and 5000 ms bounds: native
popup and search animations are awaited through actual visibility, DatePicker
days are selected through their complete localized date semantics, disabled
SearchBar inputs are checked without requiring a removed SetText action, and
long-press timeouts use the renderer's real Android clock. Callback diagnostics
record bounded lengths and equality flags, not text bodies. An early lint run
stalled in Kotlin PSI comment traversal; only that owned build was stopped.
Named controlled-state classes and fresh complete lint runs pass without
disabling checks.

## Source history and completed companion refreshes

The dedicated host worktree is clean at
`6a166c38415e7299ba034cd062dcc4c51a4b543a`. A concurrent platform-upgrade commit,
`ea74b937a5`, included 67 then-staged F.4 files. That history is preserved; the
implementation is not incorrectly attributed only to the final follow-up commit.
The Compose plugin's existing platform baseline is
`c7be49c54844a3d53665c3c2e31a39579c9dd125`, before the build33 F.4 implementation/evidence commit.
Build33 is aligned with that final local commit sequence.

Original host and Ace workspaces were not edited or staged by this implementation.
Concurrent external commits occurred; the original host was subsequently observed
clean at `7990a40366`. Its earlier dirty status is not presented as the final state.
Integration and generation used the explicit dedicated host and Ace worktrees.

| Repository | Version / source | Recorded state |
| --- | --- | --- |
| TypeScript Declarations | 4.35.0, de51858b59ec88f75c56d94bcf4af600c54ff34f | Clean source, actual generation and both compiler checks complete |
| android-dts-generator | 525bebdaa157087b1295d80cef0dae861152eda5 | Actual local aj6dts workflow executed |
| Documentation | 6.8.0 / 96, c5a96580cb65c195d27f2208eb16f2a2f4cb6fe7 | Clean; content source 479d8f1651bcf4f6b8f82b3fbe35838451c6dbfc |
| Offline Docs | 6.8.6 / build81, f1bfc004d189750949df48b3a9673418eebfc1fe | Clean; final package and content gates pass |
| TypeScript Engine | 0.6.7 / build89 | 527d8921397b94ae6c2b4e9714c8b6ee83897602 |
| Isolated Ace Editor | 1.29.0 / build143 | 5b1b0e979cadb0aab4fddc4590430f7fcd5bb722 |

Actual aj6dts generation used clean host commit `6a166c38415e7299ba034cd062dcc4c51a4b543a`,
with the generator source above. It selected 489/10839 main declarations, 20/20
resources and 789/27248 library declarations. The local `-Publish` operation
mirrored the prepared declarations into the explicit isolated Ace target; it was
not an npm publication. The final declaration repository is the committed source
for downstream provenance.

Documentation contains 156 modules and 6702 search entries / 2562288 search bytes.
The actual BAT dry-run and verification synchronized Offline Docs. Its concurrent
platform commit `42defff` is preserved before final commit `f1bfc004d189750949df48b3a9673418eebfc1fe`.
The 212 assets total 12828821 bytes, with canonical content SHA-256
`5d25acbe9843240538c5e46a3816634a3cdf49ed2182a66ee052291a88893ce4`.
The signed `autojs6-plugin-offline-docs-v6.8.6-universal-48eff18f.apk` is 4278108 bytes,
SHA-256 `24a7af1731e5119ca03aff48672950c4d05c99cf0d97592d0a7126688cf8af12`.
Python four cases, JVM two cases, content inventory and signed-package checks pass.
A prior synchronization attempt encountered a Windows APK handle retained by an
idle Gradle owner; releasing that handle allowed the actual sync and checks to
finish. Final package data above supersedes the intermediate build79 artifact.

## Q5 acceptance and exact final measurements

The existing review limits remain 2612576 APK bytes and 49194 DEX method
references. The original P5 release19 baseline remains 2375069 bytes / 44721
references; the immediate F.3 predecessor was 2545465 bytes / 46345 references.

| Candidate | APK bytes | DEX method references | Approval relationship |
| --- | --- | --- | --- |
| Reviewed build32, d4f266d2 | 3022148 | 53838 | Maintainer explicitly accepted this F.4 growth on 2026-10-07 |
| Final build33, ffee2730 | 3022152 | 53842 | Final recorded result, +4 bytes / +4 references after the reviewed candidate |

The reviewed candidate's SHA-256 is
`957ae7e988d18d5116c39e23986ae442f4813528f3e9dc9c6794977e9a05da35`.
The original acceptance applies to that identified candidate and F.4 functional
growth. The final delta records the input-boundary/disabled-callback/no-op-echo
fix and metadata/count alignment; it is not represented as a second maintainer
confirmation of the exact new numbers. No additional component or runtime
dependency was added after review.

The growth is attributable to the requested component implementations retained
by shrinking, using the existing Material3/foundation runtime. The exact baseline,
reviewed candidate, final candidate, hashes and approval reference are in
[p7-wide-size-review.json](p7-wide-size-review.json). No review limit is raised.
The original 236 P5 observations are unchanged. No new timing, cold-start, PSS or
scroll stability baseline is claimed.

## Final companion gates and cleanup

Engine 0.6.7/build89 keeps the public npm source lock and attributes the local
4.35.0 supplement to the exact committed source above. Generator/runtime revision
7 adds the V2 JSX region; Rhino is 53 files / 991341 bytes, common + Rhino is
161 / 4776099, with 118 manifest-owned files. Node and caller quotas are unchanged.
The final gate passes 534 app JVM cases, four build-logic cases, full real Worker
compilation, C8 Program/Worker layering, quota checks, debug/androidTest/lint and
all five signed/native-aligned APKs. Lint has zero errors and seven existing
warnings. Two actual debug declaration-inventory device cases also pass.

The final signed x86_64 Engine and renderer pass all nine real TSX cases and five
unchanged JS examples on API35, 14/14 in 60.797 s, including the new V2 mixed
Dialog/AndroidView/navigation/pager/chip case. Log:
`app/build/compose-f4/final-signed-tsx-and-examples.log` in the host. The exact
final renderer also passes the actual old-host5321 rejection test in
`old-host-final-renderer-gate.log`, after which the current host is restored.

| Final companion artifact | Bytes | SHA-256 |
| --- | --- | --- |
| Engine app/releases/0.6.7/autojs6-plugin-typescript-engine-v0.6.7-universal-E06D46A9.apk | 3427008 | b25a65ff3111387235259cb976c8468a9a7c18e05e5e6e6862ce697c946d6605 |
| Engine installed x86_64, 2CC81742 | 3377605 | 67a7e8501a1c4cfcf342aa5a8026f692a0c6492f0bc7029d2ef2ede71041a1d4 |
| Ace autojs6-plugin-ace-editor-v1.29.0-universal-d7bd8a25.apk | 18520872 | f2162f8d8dac5e6c4541ebb571e8e7054dadfa85fff43a476984fb99a89afd1d |

Ace uses the actual generator outputs and the committed handwritten declarations,
with BUNDLED provenance pointing to de51858. Its prior publishConfig ignore list,
curated index, two editor-owned lib files and TypeScript runtime are preserved.
Complete browser verification passes both default and optional-native groups:
25 V2 components, 34 invalid categories, property/member completion and all prior
language/smart-hint/overload/colors/terminal/F.1/F.2/F.3 checks. All 171 JVM cases,
debug/lint and five signed/native-aligned artifacts pass. Lint has zero errors
and 49 existing warnings; an Ace Android device installation is not claimed.

All ten owned package installations are removed: four on API24 and six on API35.
The AVD names, original ports, process ownership, registered paths and absolute
workspace data paths were checked before shutdown and SDK deletion. Both
compose_f4_api24 and compose_f4_api35 registrations/data are absent. API24's ADB
transport disappeared while its verified QEMU process remained alive; only its
owned localhost:5589 transport was reconnected, without restarting the global
ADB server. That connection was disconnected after its graceful shutdown.
Cleanup records are `app/build/compose-f4/cleanup-api24.json` and
`cleanup-api35.json` in the host. Physical devices, the user's other AVD and the
shared SDK images were not changed. API24 used the existing verified private
stock ramdisk and the supported swiftshader backend.

All affected source changes are locally committed by logical repository scope.
The final plugin/Engine/Ace/Offline build numbers are 33/89/143/81 and match their
reachable Git counts. The final roadmap entry records completion without changing
its section structure. Original worktrees and concurrent platform history remain
intact; no source history was rewritten.

F.5 and F.6 were not implemented in this work. Full host JVM/lint, a new six-device
matrix, packaged inrt F.4 validation, new P5 timing/memory baselines and remote CI
were not run. No Git push, npm publication, official index registration or public
Release is part of this local implementation.
