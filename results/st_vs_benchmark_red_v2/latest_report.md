# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 12,000
**S.T win rate:** 91.90%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.83% | 88.00% | +5.84 pp | 94.05% | 65.41% |
| Eradicator S.T | 93.67% | 88.70% | +4.97 pp | 95.27% | 58.17% |
| Pyroclast Squad S.T | 93.25% | 89.29% | +3.96 pp | 93.97% | 64.01% |
| Drop Pod S.T | 93.19% | 89.45% | +3.73 pp | 93.45% | 57.49% |
| Terminator S.T | 92.87% | 90.03% | +2.83 pp | 93.21% | 65.46% |
| Exterminatus S.T | 89.78% | 92.48% | -2.70 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.74% | 92.59% | -1.86 pp | 93.95% | 4.27% |
| Servo-Skull | 90.97% | 92.18% | -1.21 pp | 91.07% | 22.12% |
| Infernus S.T | 92.30% | 91.15% | +1.15 pp | 93.37% | 61.57% |
| Whirlwind S.T | 91.55% | 92.50% | -0.95 pp | 93.59% | 48.08% |
| Demolitionist S.T | 91.99% | 91.75% | +0.24 pp | 93.77% | 47.33% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
