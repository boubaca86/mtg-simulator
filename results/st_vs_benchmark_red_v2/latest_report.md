# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 15,000
**S.T win rate:** 91.71%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.78% | 87.59% | +6.20 pp | 94.00% | 65.16% |
| Eradicator S.T | 93.59% | 88.30% | +5.29 pp | 95.18% | 58.27% |
| Drop Pod S.T | 93.03% | 89.19% | +3.84 pp | 93.36% | 57.66% |
| Pyroclast Squad S.T | 93.00% | 89.28% | +3.72 pp | 93.77% | 63.65% |
| Terminator S.T | 92.66% | 89.90% | +2.76 pp | 93.06% | 65.23% |
| Exterminatus S.T | 89.55% | 92.30% | -2.75 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.41% | 92.49% | -2.08 pp | 92.97% | 4.17% |
| Servo-Skull | 90.83% | 91.97% | -1.14 pp | 90.97% | 22.01% |
| Infernus S.T | 92.06% | 91.04% | +1.02 pp | 93.18% | 61.87% |
| Whirlwind S.T | 91.43% | 92.21% | -0.78 pp | 93.55% | 48.25% |
| Demolitionist S.T | 91.87% | 91.44% | +0.43 pp | 93.64% | 47.51% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
