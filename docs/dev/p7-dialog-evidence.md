# P7 F.3 independent dialog evidence

Date: 2026-10-07. This records the existing F.3 roadmap item; no item is added,
split or discarded. F.3 is complete as a local development preview. F.4 remains the next implementation item.

## Delivered behavior and contract

`compose.dialog(nodeOrRender, options?)` returns the existing ComposeSession.
The options are type (`alert` by default or `bottomSheet`), cancelable (true by
default), and theme. An entire nullish options container uses defaults; invalid
field types and unknown fields fail with the existing typed errors. It is not
a Promise API and does not add a JSX node factory or widen the V1 catalog.

Dialogs are independent of the current page, XML containers and floating windows,
but share the eight-session limit. They accept normal reactive trees and F.2
AndroidView content, update through existing session/state/node operations, and
release resources on close, engine exit and plugin invalidation. User cancellation
honors back, outside-click and sheet drag; explicit close works when cancelable
is false. The presentation's scaffold is not an implicit Snackbar command target.

UI scripts use their actual engine-bound Activity, including before a page exists.
The writable global activity alias cannot change the mode or borrow another
engine's Activity. Non-UI scripts require the host's existing overlay permission,
retain a scoped keepalive and release it when the last owned work closes. They
also participate in legacy floaty.closeAll. There is no automatic permission UI.

The host owns only a transparent, non-interactive 1 px composition anchor. The
plugin creates one actual Compose Dialog window with a public window type/token
configuration. Alert uses a Material3 Surface; the modal sheet combines the
public Material3 BottomSheetScaffold, SheetState and scrim inside that Dialog.
This adaptation is explicit: locked Material3 1.4.0's original ModalBottomSheet
has no public pre-show overlay type parameter, and inspecting 1.5.0-alpha29 did
not justify an upgrade. No private reflection, hidden Android API, copied
internal implementation or new runtime/native dependency was introduced.

The optional `dialog-v1` feature advertises `dialogFactoryV1`; old hosts do not
resolve its shared types. The AAR adds three classes in api.dialog; all 206
retained V1/F.2 class files are byte-identical. Current release AAR SHA-256:
`e3a4f2003bb0336338e229cfc9220c2296a5de462957fd540a20c37d86996fe8`.
The frozen 30 components / 114 properties / 29 JSX tags and 51 shared dependencies
remain unchanged. See `compose-ui-dialog-v1.md` and
`p7-dialog-aar-compatibility.json` for the complete boundary.

Basic renderer compatibility stays host 5316. F.3 needs matching host 5321;
F.3 TSX additionally needs TypeScript Engine 0.6.6. Installing the renderer alone
does not add the host method or compiler declarations.

## Sources and companion workflow

| Repository | Local version | Commit |
| --- | --- | --- |
| Compose UI | 1.1.0 / 31 | This F.3 implementation/evidence commit |
| Dedicated host | 6.8.0 / 5321 | 8cba2ce0e3aad5835879b0b91604caec9cb719b1 |
| Declarations | 4.34.0 | 4512f0ef4a051c8286bd7a19cd3cd7f2d8cf50e6 |
| TypeScript Engine | 0.6.6 / 87 | c9612715b58612944ab9ae0d5b2ac0a1fbff8c43 |
| Isolated Ace | 1.28.0 / 141 | 0793f378de64d07c5e603396aa6e984a31b983f4 |
| Documentation | 6.8.0 / 94 | Content e830d58076fdc0a715f65efd787bcf907c07d5d2, counter 97d0fc429cfdd6a4f97d3a7179f5ee0856702947 |
| Offline Docs | 6.8.6 / 77 | d6cd51afb80ec901042f25a9945a38c96ca5b662 |

Host merge fbef5018f8 incorporated fixed mainline 2ab34ecfe5. Ace merge
19420767db4053956d607a696265b38b52989e0d incorporated fixed e75884b while preserving
F.2, current smart hints, overloads, colors/terminal declarations and both release
histories. Original host/Ace working directories were not edited or staged.

The actual local aj6dts -Publish workflow ran from the clean committed host above,
using generator 4.0.0 at 525bebdaa157087b1295d80cef0dae861152eda5 and the explicit
isolated Ace target. It selected 489/10834 host classes, 20/20 resources and
788/27242 dependency classes. Provenance records the exact source commit; the
clean working tree was independently checked. This was file generation/mirroring,
not npm publication.

The Engine preserves the public @sm003/autojs6-dts 2.1.3 tarball lock and separately
attributes its local 4.34.0 supplement. Generator/runtime identity remains revision
6 because the transform is unchanged; content fingerprints bind the new module.
Rhino has 53 declaration files / 959861 bytes; common + Rhino is 161 / 4744619,
with 118 manifest-owned files. Node and caller quotas are unchanged.

## Validation

| Check | Result |
| --- | --- |
| Host selected Compose/core/XML TSX/wizard/colors JVM | 214 pass |
| Shared API JVM | 23 pass; old 206 classes unchanged |
| Plugin JVM and source/packaging guards | 64 pass |
| Catalog exports/projections and Python regressions | V1/F.2 regions match; 19 pass |
| Plugin API 35 instrumentation | 49 pass, including nine F.3 cases |
| Plugin API 24 dialog instrumentation | All nine F.3 cases pass |
| Signed renderer, six real host dialog scenarios | Six pass on each of API 24 and API 35, with exact anchor and real IME checks |
| Denied overlay permission with a borrowed global Activity | One real negative case passes on each of API 24 and API 35 |
| Final host legacy/F.2/F.3 regression | 20 pass: six dialogs, six XML interop and eight floating-window cases |
| Old/new provider compatibility | New host + actual build30 provider: typed rejection with old page/callback preserved; old host5320 + final renderer: five examples, six F.2 cases and JNI/INFO, 13 pass |
| Actual compiler and Ace declarations | TS 5.1.3/6.0.3 against final published graph: F.1/F.2/F.3 positives and 25/31/22 expected errors, P6 strict/exact and declaration-body checks pass |
| Engine final release and actual TSX | Full Worker/C8/quota, 534 app JVM + four build-logic tests and five signed APKs pass; final signed provider + renderer run eight real TSX + five original JS examples, 13 pass |
| Ace final authority and signed releases | Final generate/full browser verifier, 171 JVM, debug/lint and five signed APKs/native alignment pass; 18 F.3 invalid categories per default/full native configuration |
| Documentation / Offline Docs | 156 modules, 6675 search entries / 2532671 bytes; 212 files / 12735581 bytes synchronized; validation and signed gate pass |

The host scenarios use real UI and ordinary Rhino engines, native accessibility
clicks, real IME windows and actual window lifecycle. They cover independent
updates, Activity binding despite activity=null, underlying-page replacement,
first and later opening in the same Activity, noncancelable sheet behavior,
immediate focus, dismissal during generation updates, exact anchor removal,
non-UI no-timer keepalive/automatic exit, closeAll/stop and creation rollback.

## Defects and test corrections retained in the record

The first dialog composition can run synchronously in View attachment. A real
BadToken test exposed that a coroutine exception handler alone could not catch
this path. F.3 now posts guarded initial setContent after attachment, catches its
failure, and clears pending initialization on detach/dispose. Asynchronous
composition failures use the coroutine hook. Both report a single reserved fatal
lifecycle event, which the host reports before closing; ordinary patch/native
node failures retain their existing rollback/error paths.

Empty Activities had not installed ComponentActivity ViewTree owners because no
setContentView had run. The host now calls the public initializer before adding
the anchor. It also lets the bound Activity Context supply the default application
token instead of passing a decor window token. Initial frame and focus commands
wait for the real dialog content; Focus additionally waits for actual window
focus through a removable public listener. The bounded FIFO never overtakes an
earlier command and revalidates nodes against the latest accepted tree.

The API 24 emulator first crashed in system_server's goldfish_dma_write using
swiftshader_indirect. The crash buffer is retained. Only that owned AVD was
restarted with supported swiftshader, keeping the SDK image, private stock
ramdisk and data. A real sheet swipe fixture now waits for the public enabled
Dismiss semantics before its unchanged 180 ms gesture; it never invokes that
semantics action as a substitute for dragging.

The focus FIFO fixture explicitly establishes and restores its input mode and
uses ordinary focusable nodes with a neutral default focus target, separating
window/command ordering from platform TextField/IME focus restoration. It keeps
strict event ordering and no-event-while-covered assertions. Pure JVM
lifecycle cases use the project's existing event-envelope fixture instead of
unmocked Android Bundle constructors; Android payload validation remains real.

Other fixture corrections preserve the public requirements: exact owned anchor
identity replaces a pre-layout decor child count, and Activity IME policy checks
compare STATE/ADJUST rather than the system's transient forward-navigation bit.
Immediate focus is verified before an actual editor click opens the IME, matching
the existing API's explicit statement that focus does not guarantee keyboard
visibility. Real IME appearance, Back hiding, policy, cancellation and cleanup
checks remain. No timeout was increased or platform skipped. Manual diagnostic
taps and failed attempts are not counted as successful gates.

## Artifacts, Q5 and cleanup

Final renderer candidate: `autojs6-plugin-compose-ui-v1.1.0-c8dddb90.apk`,
2545465 bytes, 7478 DEX classes / 46345 method references. These are below the
approved Q5 review lines of 2612576 bytes and 49194 method references. Signing,
CRC32, exact native inventory, class ownership and 16 KB alignment pass.
Renderer SHA-256:
`469314d90a9d85271e9278d5309130c75c433bda97fe5144e59294a3aaa7752d`.
Final companion APK identities:

- Engine universal `autojs6-plugin-typescript-engine-v0.6.6-universal-B72D8E6D.apk`,
  3422996 bytes, SHA-256
  `2f2f72b6a9773be43569956d676676a86b8cca63dd56dd97a2210cccfb7d42b0`.
  Installed x86_64 `...-v0.6.6-x86_64-A20EA5C8.apk`: 3373593 bytes, SHA-256
  `9c917ee44bda498ec963d88a1790b883a36af8b124bb9825ed83b620605774cc`.
- Ace universal `autojs6-plugin-ace-editor-v1.28.0-universal-88a3c5fb.apk`,
  18514128 bytes, SHA-256
  `3ece1338a6399321308911eb29814a3b3487fae81abfc9cebe2adbde191ad0a5`.
  The final asset refresh preserves Ace's prior package publishConfig after the
  declaration mirror operation. No Ace device installation is claimed.

Compose, Engine and Ace debug lint have zero errors and respectively four, seven
and 49 existing warnings. These are not zero-warning results. Normalized docs,
icons, AAR hashes, native inventories and relevant signed-artifact gates pass.

Offline inventory digest:
`2549c9fedcd5048e767271dcb0465e9ca492ecdc21b4216936d7abe1a741ae84`.
Signed Offline APK: `autojs6-plugin-offline-docs-v6.8.6-universal-52fa8d1d.apk`,
4263080 bytes, SHA-256
`6c5ad8f8afe1fdbecf33e5fa5dd8010a166fa6b7bdd04e04041105777089bec9`,
tied to content e830d58. The original 236 P5 observations and Q5
rules are preserved; no new comparable timing, cold-start/PSS or scroll baseline
is claimed.

All nine owned package installations were uninstalled: four on API 24 and five
on API 35. Both compose_f3_api24 and compose_f3_api35 were stopped after checking
their names and exact registered data paths within the dedicated host build tree.
SDK deletion completed and both registrations/data directories are absent; ignored
cleanup records are in the host app/build/compose-f3 directory. No physical device,
existing user AVD or shared SDK image was modified. Only local commits are made this turn;
there is no new push, npm publication, official-index entry or downloadable
Release. Full host JVM/lint, packaged-app F.3, the full physical-device matrix,
long-term/performance measurements and remote CI are not part of this gate.
