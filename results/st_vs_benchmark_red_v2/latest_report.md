# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 290,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.63% | +5.79 pp | 93.71% | 64.94% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.97% | 57.76% |
| Drop Pod S.T | 92.91% | 88.77% | +4.15 pp | 93.16% | 57.40% |
| Terminator S.T | 92.78% | 88.96% | +3.82 pp | 93.27% | 65.48% |
| Pyroclast Squad S.T | 92.76% | 89.00% | +3.76 pp | 93.55% | 64.30% |
| Exterminatus S.T | 88.88% | 92.20% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.30% | -2.17 pp | 92.39% | 4.23% |
| Infernus S.T | 91.90% | 90.70% | +1.20 pp | 93.03% | 61.34% |
| Whirlwind S.T | 91.12% | 92.11% | -0.99 pp | 93.49% | 47.77% |
| Servo-Skull | 91.09% | 91.60% | -0.51 pp | 91.10% | 22.06% |
| Demolitionist S.T | 91.56% | 91.33% | +0.24 pp | 93.25% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
