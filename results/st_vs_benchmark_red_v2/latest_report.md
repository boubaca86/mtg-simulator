# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 120,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.67% | +5.89 pp | 93.84% | 64.96% |
| Eradicator S.T | 93.19% | 88.67% | +4.52 pp | 95.08% | 57.83% |
| Drop Pod S.T | 93.02% | 88.85% | +4.17 pp | 93.31% | 57.48% |
| Pyroclast Squad S.T | 92.85% | 89.11% | +3.74 pp | 93.64% | 64.46% |
| Terminator S.T | 92.84% | 89.13% | +3.72 pp | 93.33% | 65.57% |
| Exterminatus S.T | 89.05% | 92.29% | -3.24 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.35% | -2.00 pp | 92.66% | 4.26% |
| Infernus S.T | 92.02% | 90.77% | +1.25 pp | 93.15% | 61.53% |
| Whirlwind S.T | 91.19% | 92.27% | -1.08 pp | 93.43% | 47.84% |
| Servo-Skull | 91.22% | 91.69% | -0.48 pp | 91.28% | 22.00% |
| Demolitionist S.T | 91.63% | 91.50% | +0.12 pp | 93.27% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
