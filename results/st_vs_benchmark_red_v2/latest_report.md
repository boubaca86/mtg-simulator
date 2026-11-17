# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 376,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.67% | +5.76 pp | 93.71% | 64.99% |
| Eradicator S.T | 93.02% | 88.70% | +4.32 pp | 95.00% | 57.73% |
| Drop Pod S.T | 92.92% | 88.78% | +4.14 pp | 93.19% | 57.40% |
| Pyroclast Squad S.T | 92.77% | 89.01% | +3.76 pp | 93.59% | 64.27% |
| Terminator S.T | 92.75% | 89.04% | +3.72 pp | 93.25% | 65.52% |
| Exterminatus S.T | 88.93% | 92.20% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.19 pp | 92.35% | 4.25% |
| Infernus S.T | 91.91% | 90.70% | +1.21 pp | 93.06% | 61.38% |
| Whirlwind S.T | 91.14% | 92.10% | -0.97 pp | 93.55% | 47.79% |
| Servo-Skull | 91.04% | 91.62% | -0.59 pp | 91.06% | 22.04% |
| Demolitionist S.T | 91.54% | 91.40% | +0.15 pp | 93.24% | 47.02% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
