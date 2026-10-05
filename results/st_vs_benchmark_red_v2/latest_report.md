# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 6,500
**S.T win rate:** 92.23%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.13% | 88.78% | +5.35 pp | 95.69% | 58.20% |
| Servitor | 93.98% | 88.70% | +5.28 pp | 94.20% | 65.46% |
| Pyroclast Squad S.T | 93.62% | 89.46% | +4.17 pp | 94.38% | 64.55% |
| Exterminatus S.T | 89.12% | 93.09% | -3.97 pp | 0.00% | 0.00% |
| Drop Pod S.T | 93.28% | 90.24% | +3.05 pp | 93.55% | 57.22% |
| Terminator S.T | 93.04% | 90.68% | +2.37 pp | 93.38% | 65.34% |
| Servo-Skull | 90.61% | 92.72% | -2.11 pp | 90.62% | 22.48% |
| Vindicator S.T | 91.32% | 92.76% | -1.44 pp | 92.91% | 4.34% |
| Infernus S.T | 92.61% | 91.52% | +1.10 pp | 93.55% | 61.34% |
| Demolitionist S.T | 92.45% | 91.84% | +0.61 pp | 94.10% | 47.48% |
| Whirlwind S.T | 92.09% | 92.46% | -0.37 pp | 93.72% | 47.75% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
