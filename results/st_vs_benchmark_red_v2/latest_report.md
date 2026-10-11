# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 85,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.61% | +5.91 pp | 93.78% | 64.81% |
| Eradicator S.T | 93.12% | 88.61% | +4.52 pp | 95.07% | 58.06% |
| Drop Pod S.T | 92.99% | 88.73% | +4.26 pp | 93.30% | 57.63% |
| Pyroclast Squad S.T | 92.83% | 89.00% | +3.83 pp | 93.64% | 64.24% |
| Terminator S.T | 92.75% | 89.14% | +3.61 pp | 93.26% | 65.59% |
| Exterminatus S.T | 89.11% | 92.20% | -3.09 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.34% | -2.15 pp | 92.75% | 4.24% |
| Whirlwind S.T | 91.14% | 92.21% | -1.08 pp | 93.41% | 47.90% |
| Infernus S.T | 91.88% | 90.88% | +1.00 pp | 93.03% | 61.35% |
| Servo-Skull | 91.27% | 91.60% | -0.33 pp | 91.36% | 21.96% |
| Demolitionist S.T | 91.61% | 91.37% | +0.24 pp | 93.32% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
