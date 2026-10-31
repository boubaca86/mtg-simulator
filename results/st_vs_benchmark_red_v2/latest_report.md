# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 317,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.65% | +5.76 pp | 93.70% | 64.96% |
| Eradicator S.T | 93.03% | 88.67% | +4.35 pp | 94.98% | 57.74% |
| Drop Pod S.T | 92.92% | 88.77% | +4.15 pp | 93.18% | 57.42% |
| Terminator S.T | 92.77% | 88.99% | +3.78 pp | 93.27% | 65.49% |
| Pyroclast Squad S.T | 92.76% | 89.00% | +3.75 pp | 93.57% | 64.28% |
| Exterminatus S.T | 88.87% | 92.21% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.31% | -2.18 pp | 92.46% | 4.25% |
| Infernus S.T | 91.89% | 90.72% | +1.17 pp | 93.03% | 61.35% |
| Whirlwind S.T | 91.12% | 92.11% | -0.99 pp | 93.51% | 47.75% |
| Servo-Skull | 91.03% | 91.62% | -0.59 pp | 91.05% | 22.04% |
| Demolitionist S.T | 91.56% | 91.33% | +0.23 pp | 93.26% | 47.04% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
