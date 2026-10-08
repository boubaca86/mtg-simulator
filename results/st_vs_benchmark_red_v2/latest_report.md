# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 41,000
**S.T win rate:** 91.45%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.37% | 87.62% | +5.75 pp | 93.67% | 65.04% |
| Eradicator S.T | 93.13% | 88.37% | +4.76 pp | 94.98% | 58.11% |
| Drop Pod S.T | 93.03% | 88.41% | +4.62 pp | 93.37% | 57.62% |
| Pyroclast Squad S.T | 92.83% | 88.76% | +4.07 pp | 93.64% | 64.22% |
| Terminator S.T | 92.52% | 89.36% | +3.16 pp | 93.04% | 65.46% |
| Exterminatus S.T | 89.05% | 92.10% | -3.05 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.25% | -2.13 pp | 92.67% | 4.10% |
| Infernus S.T | 91.72% | 90.93% | +0.80 pp | 92.93% | 61.35% |
| Whirlwind S.T | 91.16% | 91.94% | -0.78 pp | 93.50% | 47.69% |
| Demolitionist S.T | 91.61% | 91.14% | +0.47 pp | 93.45% | 47.15% |
| Servo-Skull | 91.10% | 91.55% | -0.45 pp | 91.17% | 21.82% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
