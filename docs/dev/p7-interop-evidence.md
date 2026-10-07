# P7 F.2 XML and Compose interop evidence

Date: 2026-10-07. This is the existing F.2 roadmap item, implemented as a local
1.1.0 development preview. No roadmap item was added, split or discarded.
The next item is F.3, independent dialogs and bottom sheets.

## Delivered behavior

- The existing XML inflater recognizes an empty `<compose>` container, including
  XML TSX. `compose.attach(container, nodeOrRender, options?)` returns a normal
  session. Replacing one container leaves other containers and native controls
  intact. Page, floating and container sessions share the existing limit of eight.
- A container may be prepared before its XML tree enters a window. Initial focus
  and scroll commands wait for composition. A real detach, layout replacement,
  legacy floating-window close or engine exit disposes the owned session.
- `compose.AndroidView({view: nativeViewOrFactory})` borrows an actual View.
  Factories run synchronously on main using the host Context; `ui.inflate` can
  return legacy XML or compiled XML TSX controls with their original listeners.
  Factory errors return typed failures without exiting the whole script.
- The node getter returns the prepared View. Stable-node updates retain leases,
  including A -> B -> A replacements. A View cannot be claimed by two nodes or
  sessions, stolen from a foreign parent, or introduce an ancestor cycle.
  New mounts validate ownership before closing previously accepted content.
- Native attachment, layout, drawing and input failures have guarded boundaries.
  Borrowed layout callbacks run before tree/generation publication; rejection
  preserves the accepted generation for retry. A native attachment callback that
  closes the session cannot reattach old Views or resurrect released claims.
- Removing a native node or closing its session detaches the borrowed View.
  The renderer does not clear caller listeners or destroy caller-owned resources
  such as WebViews. Native XML listeners keep their existing event rules; only
  Compose callbacks use the Compose script dispatcher.
- XML and Compose remain separate tree domains outside the two explicit bridges.
  XML `<compose>` and Compose AndroidView are leaf nodes. Invalid mixed trees,
  nested mounts and failed factory/replacement transactions retain old content
  and working callbacks.

## Contract and compatibility

The frozen V1 contract version, 30-entry catalog, 114 canonical properties and
29-entry JSX region are unchanged. The optional feature `android-view-interop-v1`
advertises a separate factory class using the V1 capability map. Old hosts load
the ordinary renderer without linking any of the new shared extension types.
New hosts negotiate the extension before publishing AndroidView nodes.

The release AAR is built from host commit
`d54f21b1ea408dfcb66e58f663d71d2f10fade8a`. SHA-256:
`0aaff93a27d405e8db7a513172ba1a2ebe60a3b8a09d4f27d8c9eaa03544d4a3`.
All 197 retained V1 class files are byte-identical to the old artifact. Nine
interop classes were added; seven unused negative-version P0 spike classes,
outside frozen V1, were removed as already required by the repository rules.
See `p7-interop-aar-compatibility.json` and `compose-ui-interop-v1.md`.

Basic renderer compatibility remains host 5316. F.2 requires the matching host
6.8.0 / 5320, and F.2 TSX additionally requires TypeScript Engine 0.6.5. The
51-item shared dependency lock and four-ABI graphics-path inventory are unchanged.

## Source and version identities

| Repository | Local version | Commit |
| --- | --- | --- |
| Compose UI | 1.1.0 / 30 | This F.2 implementation and evidence commit |
| Dedicated host integration | 6.8.0 / 5320 | d54f21b1ea408dfcb66e58f663d71d2f10fade8a |
| TypeScript declarations | 4.33.0 | 26dcf6cd6d4d175c179b49474d9b9ffc28939690 |
| TypeScript Engine | 0.6.5 / 86 | c9cded817f2b13bd60bfee514c9578dad0f2355c |
| Isolated Ace integration | 1.26.0 / 137 | aab24b89b82de9cfdc42bd15f6efae94e5a744bb |
| Documentation | 6.8.0 / 93 | Content 95b65af625c6f8de31481bf3d6c32ed4d7132128, counter ab541a5 |
| Offline Docs | 6.8.6 / 76 | 11911a1 |

Host merge `e05218974a` incorporated fixed mainline commit `8b5bef91f3` and retained
both release histories and current colors/theme work. The original host checkout
was not modified or staged. Ace uses an isolated `compose-ui-f2` worktree based on
fixed upstream `c5d77a5f3decb40a44d1e64604638013bbb455a6`, retaining 1.25 colors,
terminal, smart hints and signature help. Its original checkout has concurrent
work and was not modified, staged or merged by this task.

The actual `aj6dts.bat -Publish` flow used Android d.ts Generator 4.0.0 at
`525bebdaa157087b1295d80cef0dae861152eda5`, the committed host above and the explicit
isolated Ace target. It selected 488/10769 host classes, 20/20 resources and
788/27239 dependency classes. The provenance records the exact host commit;
host cleanliness was checked independently. All published output hashes match
the source declarations and Ace mirrors. This command performed local file
generation and mirroring, not npm publication.

The Engine retains its public `@sm003/autojs6-dts@2.1.3` tarball lock and separately
attributes the local 4.33.0 Compose supplement. Generator revision 6 owns 118
distribution files; the Rhino layer is 53 files / 958981 bytes and common + Rhino
is 161 files / 4743739 bytes. Node remains unchanged. The compiler-owned XML
declarations add the empty compose container, supported textAllCaps aliases and
the real EventEmitter receiver of native click callbacks.

## Validation

| Check | Result |
| --- | --- |
| Host selected Compose, XML TSX, wizard and colors JVM tests | 197 passed |
| Shared API JVM and real catalog exports | 21 passed; frozen V1 projection identical |
| Plugin JVM | 64 passed |
| Catalog Python and V1/F.2 declaration/document/JSX projections | 19 passed; all projections match |
| Plugin instrumentation, owned API 35 x86_64 | 40 passed, including eight AndroidView cases |
| Final signed renderer with new host, API 24 x86 | Six interop scenarios passed |
| Final signed renderer with new host, API 35 x86_64 | Six interop scenarios passed |
| Old host 5319 with final signed renderer | Five real JavaScript examples plus JNI/resources and independent INFO: seven passed |
| New host 5320 with actual old renderer build 27 | One dedicated compatibility scenario passed |
| TypeScript 5.1.3 and 6.0.3 against final published declarations | F.1 positives and 25 expected errors, F.2 positives and 31 expected errors, P6 strict/exact and declaration-body checks pass |
| Ace language service and final artifact gate | Default/full native groups: 30 F.2 invalid categories and existing checks pass; JVM 171, final generate/verify/debug/lint/five signed APKs/native alignment pass |
| Engine real Worker, C8 layering, quota and JVM | Full Worker, six profile-isolation checks, 534 selected app JVM cases (529 + five explicit cases), four build-logic tests pass; debug/androidTest/lint/five signed APKs pass |
| Final signed Engine + renderer with host 5320, API 35 | All seven real TSX scenarios pass, including the XML/AndroidView bridge |
| Documentation and Offline Docs | 156 modules / 6673 search entries; full generation/check and offline synchronization/signature gate passed |
| Localized Compose docs and icons | 36 Markdown products and 15 system icon resources match their generators |

The device interop scenarios exercise multiple XML containers, shared state,
actual native/Compose clicks, factories/getters/replacements, off-window attach
and focus, detach/close/engine exit, and legacy raw/resizable windows. The old
renderer case verifies basic attach plus PLUGIN_INCOMPATIBLE rejection before
mount/attach/update publication, rolled-back native getters and both old click
paths remaining usable. The three final renderer regressions cover throwing
forceLayout, a throwing legacy parent requestLayout, and closing from a borrowed
View's onAttachedToWindow callback.

Plugin debug/androidTest/release assembly, signing, CRC32, native inventory and
16 KB alignment pass. Debug lint has zero errors and four existing system-icon
shape/duplicate warnings. Engine lint has zero errors and seven warnings in
existing query/dependency/API/handler/icon locations. These are not zero-warning
results. Ace debug lint has zero errors and 49 existing dependency/resource/Android
implementation warnings. Full host JVM and lint are outside this selected gate.

## Run corrections and reproducibility

The first device fixtures used invalid Button.text and assumed native Button
text would not be uppercased by the theme. They now use documented Text children
and explicit textAllCaps=false. Visibility, click, thread, focus and cleanup
assertions and timeouts were retained. The real compiler subsequently exposed
the missing legitimate textAllCaps declaration; the actual compiler-owned
profile was corrected, then the installed signed provider passed the F.2 case.

One API 35 startup ANR occurred before tests while the main thread was Runnable
in ScriptEngineService initialization, with high system CPU pressure. Two TSX
compiler waits timed out under load. Subsequent runs used unchanged timeouts;
these attempts are retained in ignored logs rather than counted as passes.
The first broad old-host selection also included a nonexistent helper test class
and a debug-only reflection assertion for an R8-renamed Compose class. The
correct signed-package gate passes all seven applicable tests.

A fresh Ace checkout exposed four generated language indices with CRLF despite
the generator's deterministic LF comparison. A scoped .gitattributes rule now
preserves LF for those outputs. Their content matches upstream exactly and the
strict freshness check remains enabled. A direct standalone Engine build-logic
invocation lacked root-managed plugin versions; the normal included-build task
passes without adding a version override. The final Engine JVM selection produced
529 cases; the five capability-promotion fixture cases were then explicitly
selected and passed, giving 534 distinct cases across the two final runs.

Ignored detailed logs use `compose-f2-*` under each repository's build directory.
The host companion report is `docs/dev/compose-ui-p7-interop-evidence.md`.

## Artifacts and Q5 review

- Compose signed APK: `releases/autojs6-plugin-compose-ui-v1.1.0-a1de352a.apk`,
  2472197 bytes, SHA-256
  `3dd4f45976568218335c879d67078dfe28a119d97603ac01bf5d4d8b1f45530d`.
  DEX: 7237 classes / 45285 method references. Both size and reference count are
  below the approved Q5 lines of 2612576 bytes and 49194 references, approximately
  +4.09% / +1.26% against release19. No new native library was introduced.
- Engine: five signed 0.6.5 / 86 APKs. Universal
  `autojs6-plugin-typescript-engine-v0.6.5-universal-CF0035CE.apk` is 3421536 bytes,
  SHA-256 `23fd4ab2a0a5540364ff121b4c1e8d56962462a4cd84d69296cfafd8bfb09398`.
  The installed x86_64 `...-v0.6.5-x86_64-6954E0DF.apk` is 3372133 bytes, SHA-256
  `450568244d9eefab91093e23b0f17e2f8e7b025d12951c3dfbd5e929abad6503`.
- Ace: five signed 1.26.0 / 137 APKs. Universal
  `autojs6-plugin-ace-editor-v1.26.0-universal-d0432323.apk` is 18482688 bytes,
  SHA-256 `56919e9672197516a0b3d850e83acb78a57211a91fe4bc02967fc5525bf0c9e4`.
  The language service was exercised on the actual generated assets; this turn
  did not install Ace on a device.
- Offline Docs: `autojs6-plugin-offline-docs-v6.8.6-universal-96b5073c.apk`,
  4257812 bytes, SHA-256
  `7a186cf321d906bc51df76f9f7066f428b81784845e76741df81a42755606430`.
  Content: 212 files / 12720268 bytes, inventory digest
  `741cf29d58e946da801eb2849c9fe3afb0fe86a47c26df794caad59fc9b7aab9`,
  pinned to documentation content commit 95b65af.

The actual build27 renderer and host5319 APKs were copied into the dedicated
host's ignored `app/build/compose-f2/baseline` before builds; compatibility tests
installed those exact files. Release collection can replace a same-version
digest filename, so this report does not claim every old release filename remains.
The original 236 P5 observations and all Q5 rules are unchanged. No fresh
comparable timing, cold-start/PSS or scrolling baseline is claimed.

## Boundaries and cleanup

TypeScript's single global JSX expression type retains its existing implicit any.
Attributes and ordinary factory calls remain precise; mixed-tree and cardinality
checks still run in the host. Native optional-group types reject Function/Promise
fallback without requiring default loading of all Android/Java declarations.

All eight owned package installations were uninstalled: three on API 24 and five
on API 35. Both AVDs, `compose_f2_api24` and `compose_f2_api35`, were stopped after
checking their names and exact registered data paths inside the dedicated host
build directory. SDK deletion completed and both registrations/data paths were
confirmed absent. Ignored cleanup records are in `app/build/compose-f2`. All
mutating device commands use explicit owned serials. The API 24 emulator used
the task-owned stock ramdisk copy; shared SDK images and user device state were
preserved. Existing AVDs and physical-device apps were not installed into or
stopped by this work.

This turn creates local commits only. The prior 2026-10-04 permission to publish
Compose plugin source is recorded in the roadmap; it is not a claim of a new
push, npm publication, official-index registration or downloadable Release here.
The full physical-device matrix, packaged-app F.2 execution, full host JVM/lint,
fresh long-term/performance baselines and remote CI were not run. Q6 still uses
the existing valid artwork. F.3-F.6 remain in their approved order.
