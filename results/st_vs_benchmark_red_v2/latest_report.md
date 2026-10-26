# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 261,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.64% | +5.79 pp | 93.73% | 64.96% |
| Eradicator S.T | 93.05% | 88.64% | +4.41 pp | 94.98% | 57.82% |
| Drop Pod S.T | 92.91% | 88.80% | +4.12 pp | 93.15% | 57.42% |
| Terminator S.T | 92.80% | 88.94% | +3.87 pp | 93.29% | 65.48% |
| Pyroclast Squad S.T | 92.74% | 89.06% | +3.68 pp | 93.54% | 64.27% |
| Exterminatus S.T | 88.88% | 92.21% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.19 pp | 92.40% | 4.24% |
| Infernus S.T | 91.92% | 90.69% | +1.23 pp | 93.04% | 61.36% |
| Whirlwind S.T | 91.11% | 92.16% | -1.06 pp | 93.49% | 47.81% |
| Servo-Skull | 91.07% | 91.62% | -0.54 pp | 91.10% | 22.05% |
| Demolitionist S.T | 91.58% | 91.32% | +0.26 pp | 93.25% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
