# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 289,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.63% | +5.80 pp | 93.71% | 64.95% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.97% | 57.76% |
| Drop Pod S.T | 92.91% | 88.77% | +4.15 pp | 93.16% | 57.41% |
| Terminator S.T | 92.78% | 88.96% | +3.82 pp | 93.27% | 65.47% |
| Pyroclast Squad S.T | 92.76% | 89.00% | +3.76 pp | 93.55% | 64.30% |
| Exterminatus S.T | 88.88% | 92.20% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.31% | -2.17 pp | 92.37% | 4.24% |
| Infernus S.T | 91.90% | 90.69% | +1.21 pp | 93.03% | 61.35% |
| Whirlwind S.T | 91.12% | 92.11% | -1.00 pp | 93.49% | 47.77% |
| Servo-Skull | 91.08% | 91.60% | -0.52 pp | 91.09% | 22.06% |
| Demolitionist S.T | 91.56% | 91.33% | +0.23 pp | 93.24% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
