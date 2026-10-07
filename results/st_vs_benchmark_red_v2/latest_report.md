# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 31,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.69% | +5.86 pp | 93.81% | 65.11% |
| Eradicator S.T | 93.30% | 88.43% | +4.87 pp | 95.06% | 58.45% |
| Drop Pod S.T | 93.04% | 88.78% | +4.27 pp | 93.35% | 57.83% |
| Pyroclast Squad S.T | 92.91% | 89.03% | +3.88 pp | 93.66% | 64.10% |
| Terminator S.T | 92.64% | 89.55% | +3.08 pp | 93.11% | 65.45% |
| Exterminatus S.T | 89.22% | 92.24% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.41% | -2.19 pp | 92.52% | 4.11% |
| Infernus S.T | 91.91% | 90.98% | +0.93 pp | 93.07% | 61.51% |
| Whirlwind S.T | 91.33% | 92.03% | -0.70 pp | 93.61% | 47.72% |
| Demolitionist S.T | 91.79% | 91.22% | +0.57 pp | 93.49% | 47.36% |
| Servo-Skull | 91.29% | 91.68% | -0.39 pp | 91.34% | 22.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
