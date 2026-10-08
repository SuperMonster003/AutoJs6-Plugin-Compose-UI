# P7 F.6 fine-grained update evidence

Date: 2026-10-08. This records the existing F.6 roadmap item; no item is added,
split or discarded. F.6 was conditional on performance evidence. The evidence was
collected first, it justified the change, and F.6 is now complete as a local
development preview. F.5 remains unscheduled.

## Decision input

P5.3 measured a 1000-row first frame and real scrolling, but not a state change while
a large render-layer tree stays mounted, so it could not show whether whole-tree
rebuilds dominate updates. The host performance harness gained an opt-in
`list-updates` section (host commit `51b7760c49`): the same tree of a label, a swatch,
a button and 1000 unchanged LazyColumn rows, updated once per click either by a
render-layer state write or directly through handles.

Owned API35 x86_64 google_apis AVD, isolated host 6.8.0 / 5322, Compose UI
1.1.0 / 36 release, 2 warmups and 10 samples per layer. Cells are median / p90 ms to
screenshot-confirmed visibility (the P5.3 instrumented upper bound, not pure render).

| Measure | Render layer | Handle layer |
| --- | --- | --- |
| State write to pixel confirmation | 722.50 / 732.29 | 156.44 / 178.53 |
| Render callback construction | 483.45 / 497.05 | not applicable |

Every click caused exactly one render. About 483 ms of the 566 ms difference is
JavaScript reconstruction of unchanged rows, on the Android main thread in UI mode.
The existing host snapshot comparison already suppresses unchanged native patches,
so the remaining cost had to be removed on the script side. F.6 was therefore
implemented as planned in appendix F.6 with `compose.memo(fn, deps)`.

## Delivered behavior

`compose.memo(fn, deps, options?)` (host commit `1adc2b8639`, build 5323) is valid
only while a session's render function runs. A slot is scoped to the render or the
enclosing memo fragment, matched by `options.key` or call order and by the function
literal. The previous node is reused while every dependency is unchanged, no
`compose.state` read by the fragment has been written since, and the node is still
in the previous tree; a reused fragment keeps the render subscribed to those states.
The per-session cache commits only with a published render, survives failed renders,
resets when the renderer rejects a frame and closes with the session. Limits are 256
dependencies per call and 5000 fragments per render; duplicate keys in one scope are
`DUPLICATE_KEY`.

The renderer contract, plugin APK code and the minimum host of this plugin (5322)
are unchanged. Scripts need the host build 5323 for `compose.memo`; TSX scripts need
TypeScript Engine 0.6.8, whose compiler rejects unknown members under `noEmitOnError`.

## Result

Same AVD and plugin, with the memo implementation (APK versionCode 5322 built before
the version bump), adding keyed `compose.memo` rows as a third workload:

| Measure | Render layer | Memo rows | Handle layer |
| --- | --- | --- | --- |
| State write to pixel confirmation | 752.78 / 791.42 | 307.22 / 322.22 | 155.81 / 173.66 |
| State write to semantics text | 692.60 / 729.37 | 247.74 / 255.48 | 99.28 / 112.53 |
| Render callback construction | 489.53 / 554.89 | 99.94 / 107.55 | not applicable |

Keyed memo rows reduce the visible update by about 59% and render construction by
about 80% in the same run. These are single-AVD observations for this decision;
they do not replace or extend the Q5 review baselines, and the 236 P5 observations
are unchanged.

## Verification

| Scope | Result |
| --- | --- |
| Host selected Compose JVM / contract API JVM | 208 / 29, zero failures or skips, including 5 memo cases |
| Host `ComposeScriptUiDeviceTest`, API35 AVD | 8 / 8, including 300 keyed memo rows: an unrelated render rebuilds none, a reverse plus one edit rebuilds exactly one and keeps handles |
| Host TSX + JavaScript examples, signed Engine 0.6.8, host 5323 | 10 TSX scenarios including the memo fixture and 5 examples: 15 / 15 in 61.873 s |
| Declarations 4.36.0 `5102e03` | TS 5.1.3 / 6.0.3: F1-F4 corpora, P6 strict/exact smoke with 5 new memo errors, declaration body: 0 unexpected diagnostics |
| Ace 1.30.0 / 144 `6748217` | Browser verifier with memo completion, valid keyed rows and 4 rejected categories in both configurations; JVM 171; lint 0 errors; 5 signed APKs |
| TypeScript Engine 0.6.8 / 90 `27e6100` | Worker compiler gate with memo compile/execute and 6 invalid inputs; JVM 534 + build-logic 21; lint 0 errors; 5 signed APKs |
| Documentation 6.8.0 / 97 `a8f7972`, `b882abc`; Offline Docs 6.8.6 / 82 `918e881` | 156 modules, 6704 search entries; offline sync, plugin tests and release APK checks pass (212 assets, 12838225 bytes) |
| Plugin JVM | 70 / 70 |

The actual `aj6dts -Publish` workflow ran from the clean host commit and published to
the isolated Ace worktree. It selected 494/10845 main classes; resources and
libraries are byte-identical to F.4, and main adds the host's memo cache types
through the existing exported internal members of the Compose module.

## Plugin artifact

No renderer code, contract or dependency changed. The bundled instruction and
changelog documents gained the host requirement note and the Hong Kong term fix,
and the retired P0 keep rule was removed.

| Item | Value |
| --- | --- |
| File | `autojs6-plugin-compose-ui-v1.1.0-2740f0ee.apk`, 1.1.0 / build38 |
| Size | 3023736 bytes, +1584 bytes from the F.4 final build33 |
| DEX | 9031 classes / 53842 method references, unchanged |
| SHA-256 | `3eddd777cfc38ac10c2514eea137442405783c4d4fe0f145e651d1ee7d977584` |

Both values remain above the original Q5 review limits, as accepted for the F.4
growth. The +1584 bytes are text assets only; `p7-f6-size-review.json` records this
candidate for maintainer review without presenting it as re-approved.

## Not executed

The six-device matrix, API24 runs, packaged-app (inrt) runs with `compose.memo`,
host full JVM and lint, new comparable Q5 timing or memory baselines and remote CI
were not run in this round. Host Compose work stays on the `spike/compose-ui-p0`
branch; nothing was pushed, published to npm, registered in the official index or
released publicly.
