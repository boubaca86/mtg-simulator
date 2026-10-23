# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 230,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.60% | +5.84 pp | 93.74% | 64.94% |
| Eradicator S.T | 93.04% | 88.64% | +4.40 pp | 94.98% | 57.79% |
| Drop Pod S.T | 92.93% | 88.74% | +4.18 pp | 93.18% | 57.45% |
| Terminator S.T | 92.78% | 88.98% | +3.80 pp | 93.27% | 65.42% |
| Pyroclast Squad S.T | 92.74% | 89.03% | +3.71 pp | 93.55% | 64.29% |
| Exterminatus S.T | 88.81% | 92.22% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.30% | -2.15 pp | 92.52% | 4.23% |
| Infernus S.T | 91.92% | 90.67% | +1.24 pp | 93.05% | 61.42% |
| Whirlwind S.T | 91.11% | 92.13% | -1.01 pp | 93.48% | 47.83% |
| Servo-Skull | 91.03% | 91.62% | -0.59 pp | 91.06% | 22.04% |
| Demolitionist S.T | 91.58% | 91.31% | +0.27 pp | 93.23% | 47.11% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
