# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 87,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.60% | +5.94 pp | 93.80% | 64.81% |
| Eradicator S.T | 93.14% | 88.60% | +4.53 pp | 95.09% | 58.05% |
| Drop Pod S.T | 93.01% | 88.71% | +4.30 pp | 93.32% | 57.64% |
| Pyroclast Squad S.T | 92.84% | 89.00% | +3.83 pp | 93.65% | 64.28% |
| Terminator S.T | 92.76% | 89.15% | +3.61 pp | 93.27% | 65.56% |
| Exterminatus S.T | 89.09% | 92.22% | -3.13 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.35% | -2.14 pp | 92.73% | 4.24% |
| Whirlwind S.T | 91.14% | 92.22% | -1.08 pp | 93.42% | 47.91% |
| Infernus S.T | 91.89% | 90.88% | +1.01 pp | 93.05% | 61.36% |
| Servo-Skull | 91.27% | 91.62% | -0.35 pp | 91.35% | 21.95% |
| Demolitionist S.T | 91.61% | 91.40% | +0.22 pp | 93.32% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
