# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 122,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.68% | +5.88 pp | 93.84% | 64.96% |
| Eradicator S.T | 93.18% | 88.69% | +4.50 pp | 95.07% | 57.84% |
| Drop Pod S.T | 93.03% | 88.86% | +4.17 pp | 93.32% | 57.48% |
| Pyroclast Squad S.T | 92.87% | 89.09% | +3.77 pp | 93.66% | 64.47% |
| Terminator S.T | 92.85% | 89.14% | +3.71 pp | 93.34% | 65.54% |
| Exterminatus S.T | 89.03% | 92.30% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.32% | 92.36% | -2.04 pp | 92.64% | 4.25% |
| Infernus S.T | 92.01% | 90.79% | +1.23 pp | 93.15% | 61.54% |
| Whirlwind S.T | 91.20% | 92.27% | -1.08 pp | 93.46% | 47.81% |
| Servo-Skull | 91.23% | 91.69% | -0.46 pp | 91.29% | 21.97% |
| Demolitionist S.T | 91.65% | 91.48% | +0.17 pp | 93.28% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
