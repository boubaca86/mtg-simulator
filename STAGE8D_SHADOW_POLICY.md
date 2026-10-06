# Stage 8D — frozen shadow recommendations

Status: implemented and verified locally on all 820 captured proposals from the
32-game Stage 8C confirmation corpus. CI verification is pending.

The Stage 8C fresh-seed gate passed; see `STAGE8C_FRESH_SEED_RESULTS.md`.
This step packages that exact model into a reproducible checkpoint and adds a
read-only scorer that can consume Forge capture lines as they arrive. Forge
continues to select and execute every action. This component has no execution
interface, game mutation, model update or live-control option.

## Input and checkpoint boundary

`stage8d_shadow_policy.py export` trains once on only the pinned development
corpus, SHA-256 `1173a323a329460d69abfd2c8cab71816430759830b26c927874cc389ff42903`.
An altered corpus, including confirmation rows, is rejected before training.
The JSON checkpoint contains 9,060 weights, the frozen source hashes, training
seeds, configuration and content hash. Loading rejects content corruption,
source/configuration drift, obsolete targets and incompatible training provenance.
Loaded weights are immutable. No pickle or executable checkpoint format is used.

Prediction copies an explicit allowlist: legal public zones and their descriptors,
public counts/life/phase, actor name and typed candidate identities. Search scores,
the selected proposal, terminal outcomes, seed IDs and decision IDs cannot enter
the feature input. Hidden fields, missing public features, duplicate actions,
obsolete target arrays and incomplete three-world replay coverage are rejected.

The scorer returns all equally preferred actions using the frozen model's tie
tolerance. A tied prediction has no single recommendation. Forced proposals are
counted separately. Only after prediction does the observer compare its top set
with Forge's proposal. Exact recipe identities are retained for audit; they are
not issued as game commands.

## Use

With the pinned development artifact and audited source logs downloaded:

```bash
python stage8d_shadow_policy.py export development/stage8a-counterfactual.jsonl --output shadow-model.json
python stage8d_shadow_policy.py observe shadow-model.json < capture.log > shadow-recommendations.jsonl
python stage8d_shadow_replay.py shadow-model.json confirmation --output-dir shadow-results
```

`observe` reads standard input, emits each recommendation immediately, and needs
neither terminal results nor search-score labels to predict. Invalid capture
events produce rejected diagnostics without a recommendation. Its output remains
**provisional until whole-log audit**. Do not treat a partial, timed-out or failed
stream as accepted evidence.

`replay` first validates each entire source log for four completed games and the
existing timeout/exception/replay/fusion contracts. It matches every capture to a
unique Stage 7 observation, regenerates the confirmation corpus byte for byte,
and checks that the exported model exactly reproduces the frozen model's metrics.
It also verifies that all source logs remain unchanged.

## Verified behavior

Ten new regressions cover label/provenance independence, same-name target stats,
order-invariant ties, immutable inputs/weights, hidden/incomplete input rejection,
three-world audit checks, immediate streaming output, forced proposals, checkpoint
drift and rejection of unpinned training data.

Local replay scored **820/820 captured proposals** across all 32 source games:
286 forced, 520 with a unique model preference and 14 with tied preferences.
The 534 rankable decisions reproduce the entire frozen target-model metric
summary exactly. Forge's proposal belongs to the model's preferred set in 672
events; that count includes forced proposals and is not a playing-strength metric.

Model ID: `6c4dd27f364aaa6c9e9100f88eb323eaa419dab0a49845ee0f748b9998746288`.

## Remaining boundary

These are search proposals, including phase probes that Forge may defer.
Recommendations are not yet matched to verified executed moves or subsequent
continuations. The live-input interface is exercised by replay; an independently
running live sidecar and execution matching remain the next integration step.
Replaying this already observed corpus is correctness evidence, not a new test
of playing strength. `promotion_allowed=false` throughout.
