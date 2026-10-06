# Stage 8C — fresh-seed replication protocol

Status: predeclared before capture. Offline only; no learned model controls Forge gameplay.

## Why a new fresh test is required

The original Stage 8 fresh-seed experiment on families 20261008–20261011 failed
its predeclared primary threshold. Those families are now observed development
data and must not be reused as an untouched confirmation set.

Stage 8C is a different representation. Its typed-target development comparison
on families 20261004–20261007 improved top-choice accuracy from 81.08% to 82.63%,
pairwise accuracy from 70.91% to 72.41%, and normalized regret from 0.09218 to
0.08209. This is development evidence only.

## Frozen development source

Train once on the Stage 8C counterfactual corpus from successful workflow run
`37399134353`, artifact `11385157491`, artifact digest
`sha256:114369fdb621ba6d05e1ee35b7166113122a924d2c67b38819bfb4634f38494c`.

The model/trainer and learner-boundary implementations are frozen to these Git
blob IDs before the fresh run:

- `stage8c_target_features.py`: `2252ab16bdac2776c78f8e434968eb520331681f`
- `stage8c_target_ranker.py`: `d278871dd8fa9467506bb82c8230d6453bbda9a8`
- `stage8_action_ranker.py`: `4343ff1757ffd4c7ad7e7ff602651bfd34d0e7de`
- `stage8_ranker_features.py`: `76187b1ac0e06842fcd87f33a7aada6107c3da45`
- `patch_forge_stage7_legal_extractor.py`: `d509910dea7a3b4589bb2c1d808e272e61f9cba6`
- `patch_forge_stage8_counterfactual_capture.py`: `25d05811eb64ef643626bce0ddeb5d5fdbb33efc`
- `stage8_serialize_counterfactual.py`: `88ed91a61d8727848444eece31ffb536af6466bd`
- `stage8_validate_counterfactual.py`: `35c1104255aa1637e067c081b09b8a784e4c1cc8`

Typed public targets remain `forge-public-targets-v2`. Forge 2.0.15 remains the
rules referee and fixed-root search still evaluates each complete executable
action across three information-set worlds.

## Untouched families

Capture exactly seeds **20261012, 20261013, 20261014 and 20261015**. For each
family run four complete games with ST first and four with Benchmark Red first:
32 games total. Keep the same decks, Forge 2.0.15, three information-set samples,
capture schema, fixed-root search, and 600-second slow-match cutoff.

Reject a source log rather than replacing a difficult seed if timeout, exception,
replay mismatch, strategy fusion, partial-world replay or target-boundary checks
fail.

## Primary comparison and fixed gate

Primary contrast: frozen Stage 8C target semantics versus the Stage 8B
`public_semantics` representation. Both train once on only families
20261004–20261007 and evaluate the identical candidates in the four untouched
families.

Primary metric: normalized regret averaged within represented games, then within
each seed family, then equally across the four families. Smaller is better.

Call the replication positive only if all three predeclared conditions hold:

1. target semantics reduce seed-macro normalized regret by at least **0.01000**;
2. target semantics have lower regret in at least **3 of 4** fresh families;
3. seed-macro game-macro top-choice accuracy is no worse by more than **2.00
   percentage points**.

Require at least one rankable game in every family and report proposal/game
coverage. Secondary metrics do not replace the primary contrast after results
are known.

## Interpretation

A positive replication justifies Stage 8D shadow-policy instrumentation: the
learned ranker may score Forge candidates and log its recommendation, but Forge
still chooses and executes every action. It does not authorize live learned
control.

A failed replication remains a real failed result. Do not relax thresholds or
reuse these families as untouched evidence. Diagnose the representation, treat
these seeds as development data, and predeclare another future confirmation set
after any model change.
