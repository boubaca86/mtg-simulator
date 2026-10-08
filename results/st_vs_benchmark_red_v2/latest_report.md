# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 47,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.80% | +5.66 pp | 93.74% | 65.07% |
| Eradicator S.T | 93.25% | 88.50% | +4.76 pp | 95.05% | 58.01% |
| Drop Pod S.T | 93.11% | 88.63% | +4.48 pp | 93.42% | 57.47% |
| Pyroclast Squad S.T | 92.91% | 88.97% | +3.93 pp | 93.69% | 64.27% |
| Terminator S.T | 92.74% | 89.30% | +3.44 pp | 93.25% | 65.52% |
| Exterminatus S.T | 89.20% | 92.22% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.39% | -2.18 pp | 92.86% | 4.13% |
| Infernus S.T | 91.87% | 91.00% | +0.88 pp | 93.03% | 61.49% |
| Whirlwind S.T | 91.28% | 92.07% | -0.79 pp | 93.55% | 47.74% |
| Demolitionist S.T | 91.74% | 91.25% | +0.49 pp | 93.49% | 47.29% |
| Servo-Skull | 91.22% | 91.67% | -0.45 pp | 91.30% | 21.75% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
