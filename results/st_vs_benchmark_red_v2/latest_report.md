# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 356,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.65% | +5.77 pp | 93.70% | 64.96% |
| Eradicator S.T | 93.00% | 88.72% | +4.28 pp | 94.97% | 57.72% |
| Drop Pod S.T | 92.92% | 88.77% | +4.15 pp | 93.18% | 57.40% |
| Pyroclast Squad S.T | 92.76% | 89.01% | +3.75 pp | 93.57% | 64.28% |
| Terminator S.T | 92.76% | 89.01% | +3.75 pp | 93.26% | 65.53% |
| Exterminatus S.T | 88.93% | 92.20% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.31% | -2.17 pp | 92.46% | 4.24% |
| Infernus S.T | 91.91% | 90.69% | +1.22 pp | 93.05% | 61.40% |
| Whirlwind S.T | 91.13% | 92.11% | -0.98 pp | 93.53% | 47.79% |
| Servo-Skull | 91.01% | 91.62% | -0.61 pp | 91.03% | 22.03% |
| Demolitionist S.T | 91.54% | 91.38% | +0.16 pp | 93.25% | 47.02% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
