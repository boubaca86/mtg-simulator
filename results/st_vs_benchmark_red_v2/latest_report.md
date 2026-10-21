# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 200,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.64% | +5.84 pp | 93.76% | 64.91% |
| Eradicator S.T | 93.08% | 88.66% | +4.41 pp | 95.00% | 57.79% |
| Drop Pod S.T | 92.94% | 88.82% | +4.12 pp | 93.20% | 57.41% |
| Terminator S.T | 92.80% | 89.02% | +3.78 pp | 93.30% | 65.45% |
| Pyroclast Squad S.T | 92.77% | 89.07% | +3.70 pp | 93.58% | 64.34% |
| Exterminatus S.T | 88.85% | 92.25% | -3.40 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.34% | -2.18 pp | 92.58% | 4.22% |
| Infernus S.T | 91.94% | 90.72% | +1.22 pp | 93.07% | 61.43% |
| Whirlwind S.T | 91.16% | 92.14% | -0.98 pp | 93.52% | 47.86% |
| Servo-Skull | 91.10% | 91.64% | -0.53 pp | 91.14% | 22.00% |
| Demolitionist S.T | 91.61% | 91.34% | +0.27 pp | 93.23% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
