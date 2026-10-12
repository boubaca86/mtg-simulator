# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 89,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.57% | +5.96 pp | 93.79% | 64.83% |
| Eradicator S.T | 93.13% | 88.58% | +4.55 pp | 95.09% | 58.04% |
| Drop Pod S.T | 93.01% | 88.69% | +4.32 pp | 93.32% | 57.61% |
| Pyroclast Squad S.T | 92.85% | 88.95% | +3.90 pp | 93.66% | 64.26% |
| Terminator S.T | 92.74% | 89.14% | +3.61 pp | 93.25% | 65.56% |
| Exterminatus S.T | 89.06% | 92.21% | -3.14 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.34% | -2.15 pp | 92.70% | 4.24% |
| Whirlwind S.T | 91.13% | 92.21% | -1.08 pp | 93.41% | 47.88% |
| Infernus S.T | 91.89% | 90.84% | +1.05 pp | 93.04% | 61.38% |
| Servo-Skull | 91.25% | 91.60% | -0.36 pp | 91.32% | 21.97% |
| Demolitionist S.T | 91.60% | 91.39% | +0.21 pp | 93.29% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
