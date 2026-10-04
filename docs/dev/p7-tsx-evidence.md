# P7 F.1 TSX evidence

Date: 2026-10-04. F.1 is complete as a local 1.1.0 development preview.
F.2-F.6 remain in their existing roadmap order; no roadmap section was added,
split or discarded. D7 remains in force: no Git push, npm publication, official
index registration or remote release.

## Delivered behavior

- The host's existing classic TSX ABI accepts `<compose.Name>`, aliases of the
  current runtime's real factory functions and `<compose:Name>`. Exact catalog
  strings route to the same graph construction path as `compose.createElement`.
  All 29 node factories are supported; Snackbar remains a session command.
- Neutral fragments preserve runtime ownership, original order and scalar
  semantics. Compose roots require one expanded node, slots accept zero or one
  subject to required-slot validation, and containers retain catalog rules.
  Cycles, depth, source-array size and accumulated input cost remain bounded.
  The frozen lazy-child limit is 10000, distinct from the 5000-node limit.
- Lowercase XML TSX remains available. Mixed XML/Compose trees and unregistered
  function components are rejected before replacing the current page. Rejected
  reactive frames retain the last accepted nodes and their working callbacks.
- A device-discovered identity bug is fixed: Rhino's `NativeObject` also implements
  `Map`, so generic unwrapping had copied node handles into ordinary objects. The
  TSX-specific unwrap preserves identity; the general-purpose conversion is
  unchanged. Real return-bridge tests cover the regression.
- TypeScript declarations and Ace use a generated 29-entry JSX map. Component
  attributes, required props and callback receivers retain existing precise
  factory types. The compiler's actual bundled Rhino layer now includes the same
  Compose module and JSX map, plus `compose`, `$compose` and `ComposeError`.

TSX needs the matching AutoJs6 6.8.0 / 5319 host and TypeScript Engine 0.6.4.
The renderer's ordinary JavaScript compatibility minimum stays 5316. Frozen V1
wire/AAR hashes, 51 shared dependencies and the renderer implementation do not
change. Installing the renderer alone does not add host TSX support.

## Source and version identities

| Repository | Local version | Commit |
| --- | --- | --- |
| Compose UI | 1.1.0 / 27 | This F.1 evidence commit |
| Dedicated host integration | 6.8.0 / 5319 | b67d668f485494f246d557c51c7f06833fb0b53c |
| TypeScript declarations | 4.31.0 | 3213fb08f9ee6e26aa3f9f283c61a49f286684ae |
| TypeScript Engine | 0.6.4 / 84 | be699233545bcd8a23f9f17d96afaba081664612 |
| Ace Editor | 1.23.0 / 125 | a1b7045 |
| Documentation | 6.8.0 / 91 | Content 4c3e292, synchronization counter 55c033f |
| Offline Docs | 6.8.6 / 72 | cbb7183 |

The required `aj6dts.bat -Publish` flow was local generation/mirroring only, using
Android d.ts Generator 4.0.0 at 525bebdaa157087b1295d80cef0dae861152eda5. Its clean
host input is b67d668f48. The export selected 487/10100 host classes, 20/20 resource
classes and 786/27236 dependency classes. Host build/time did not auto-increment.
The handwritten JSX mirrors match; Ace keeps its curated index and its own
package publishing options. No generator repository changes were necessary.

The Engine keeps the public npm `@sm003/autojs6-dts@2.1.3` tarball lock unchanged.
The separate local 4.31.0 source is explicitly attributed, stored as repository
resources with LF normalization and SHA-256 checks, and bound into the ordinary
layer/profile/distribution fingerprints. It is not advertised as an npm release.
Generator revision 5 owns 118 packaged files, with 53 Rhino declaration files /
956181 bytes. Common + Rhino is 161 files / 4740939 bytes; Node is unchanged.
This verification also exposed stale C8 generator and T2 transport source-text
expectations. Their guards now match existing production authorities; no real
compiler quota, profile option or wire capability was changed.

## Validation

| Check | Result |
| --- | --- |
| Host Compose loader/session/runtime and XML TSX JVM selection | 173 passed |
| Frozen Compose API JVM | 16 passed |
| Compose plugin JVM | 63 passed |
| Catalog Python tests and all three projections | 16 passed; 30 components / 114 props / 29 JSX tags |
| TypeScript 5.1.3 and 6.0.3 against actual declarations | Both pass positive TSX and 25 expected errors, actual classic emit, ordinary and exact-nullish regressions |
| Ace actual Rhino language-service profiles | Default and all optional groups pass; 17 invalid-input categories and 36 legacy XML tags checked |
| Ace authority after final host declaration publication | Generate/verify, debug APK and native alignment pass; complete JVM run has 171 passing tests |
| Engine real Worker desktop suite | Pass, using actual generated common/Rhino declarations, including the global ComposeError reference |
| Engine declaration layering / current quota guards | Pass; six profile-isolation checks; 534 relevant app JVM tests and 4 build-logic tests pass |
| Engine debug/androidTest, lint and five signed release APKs | Pass; debug lint has 0 errors and 5 warnings in existing code/dependency locations |
| Engine installed declaration inventory, API 35 | 2 passed on the debug provider |
| Final signed Engine + signed renderer, owned API 35 x86_64 AVD | 6 TSX scenarios and 5 actual bundled JavaScript examples pass |
| Documentation and Offline Docs | 152 modules / 6548 search entries; full generation/check, 208-file sync, JVM/Python and debug/release/signature gates pass |
| Compose localized Markdown and icons | 36 artifacts and 2 icons match their generators |

The six TSX cases compile real source files through the installed TypeScript
provider and normal launch path. They cover member/alias/namespace tags, nested
slots, single-root fragments, native clicks, state/ref/key reuse, mixed-frame
recovery, three rejected XML replacement paths, invalid roots/slots/functions,
the non-UI mount error and an actual non-UI floating window that updates/closes.
No Compose test declarations or handwritten lowered-JS substitute are used.
The same final packages also run all five unchanged JavaScript example assets.

Detailed ignored logs include the host's `app/build/compose-f1-verified.log`,
`compose-f1-device-fixed.log` and `compose-f1-final-device.log`, the Engine's
`app/build/compose-f1-final-release.log` and desktop/quota logs, the Ace final
publication log, and this repository's `build/f1-*` generation/sync logs.
Companion evidence is in the host's `docs/dev/compose-ui-p7-tsx-evidence.md`,
Declarations' `docs/compose-tsx-acceptance.md` and Ace's
`docs/development/compose-tsx-declarations-2026-10-04.md`.

## Final local artifacts and Q5 review

- Compose: `releases/autojs6-plugin-compose-ui-v1.1.0-0a1cb1c0.apk`, 2381737 bytes,
  SHA-256 `f03d899aaf5a32ed5f26d06ccf9ae48e4e8977596947249442f9ad8bca9e2aca`.
  DEX: 7143 classes / 44721 method references. APK size is below 2612576 bytes
  and references below 49194, the approved Q5 review lines. Size is about 0.28%
  above release19; reference count is unchanged. Signing, CRC32, exact four-ABI
  graphics-path inventory, compression and 16 KB alignment checks pass.
- TypeScript Engine: five signed 0.6.4 / 84 artifacts; universal
  `autojs6-plugin-typescript-engine-v0.6.4-universal-CEA36571.apk` is 3178208 bytes.
  The final x86_64 `...-5C99FB70.apk` was used for the 11-case host device run.
- Offline Docs: `autojs6-plugin-offline-docs-v6.8.6-universal-aba99023.apk`,
  4009921 bytes. Its content inventory is 208 files / 12415848 bytes, digest
  `df7c35f9fd87a9b48e51f732070c040e67c76f754fd445f52c22472d88512105`,
  pinned to documentation content commit 4c3e292.

Prior release artifacts are retained. The original 236 P5 observations and
review rules are unchanged. This work did not collect a fresh comparable timing,
cold-start/PSS or scrolling baseline; the new AVD is not substituted for the
approved baseline devices.

## Boundaries and cleanup

TypeScript has one global JSX expression type. The existing implicit `any`
result is preserved instead of misclassifying XML as ComposeNode. Runtime checks
therefore remain necessary for mixed trees and fragment cardinality. Normal
strict mode can also admit a single JSX child through an optional-never leaf
property; exact optional mode catches it, and the runtime rejects it in either
case. These limits are recorded and tested without widening ordinary factories.
Recommend `<compose.Name>` for property completion; TypeScript 6.0.3 namespace-tag
completion is limited even though diagnostics and emit are correct.

All five packages installed in the owned AVD were uninstalled. The owned
`compose_f1_api35` AVD was stopped, its registered path was verified inside the
dedicated host build directory, and its registration/data were deleted. Existing
AVDs, physical-device applications and shared SDK images were preserved.
The original host checkout has concurrent codegen work; it was not staged,
modified or cleaned. Ace's pre-existing untracked `releases/` was also preserved.

Full host JVM/lint, the full device matrix, packaged-app TSX execution, fresh
long-term/performance measurements and remote CI were not run. F.2 embedding,
independent `compose.dialog`, wider components and later P7 items remain pending.
Q6 still uses the existing valid light/dark mipmaps with temporary artwork.
