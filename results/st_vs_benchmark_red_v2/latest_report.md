# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 16,000
**S.T win rate:** 91.70%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.74% | 87.61% | +6.13 pp | 93.98% | 65.25% |
| Eradicator S.T | 93.56% | 88.32% | +5.24 pp | 95.16% | 58.35% |
| Drop Pod S.T | 92.98% | 89.28% | +3.70 pp | 93.29% | 57.51% |
| Pyroclast Squad S.T | 92.95% | 89.31% | +3.64 pp | 93.71% | 63.76% |
| Terminator S.T | 92.68% | 89.82% | +2.86 pp | 93.06% | 65.21% |
| Exterminatus S.T | 89.62% | 92.27% | -2.65 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.43% | 92.46% | -2.03 pp | 92.78% | 4.16% |
| Infernus S.T | 92.04% | 91.06% | +0.98 pp | 93.17% | 61.77% |
| Whirlwind S.T | 91.36% | 92.29% | -0.93 pp | 93.52% | 48.21% |
| Servo-Skull | 90.99% | 91.91% | -0.92 pp | 91.11% | 21.99% |
| Demolitionist S.T | 91.87% | 91.39% | +0.48 pp | 93.62% | 47.54% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
