# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 368,500
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
| Eradicator S.T | 93.02% | 88.69% | +4.33 pp | 95.00% | 57.72% |
| Drop Pod S.T | 92.92% | 88.76% | +4.16 pp | 93.19% | 57.40% |
| Pyroclast Squad S.T | 92.76% | 89.00% | +3.76 pp | 93.57% | 64.29% |
| Terminator S.T | 92.75% | 89.01% | +3.74 pp | 93.25% | 65.52% |
| Exterminatus S.T | 88.93% | 92.19% | -3.26 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.32% | -2.19 pp | 92.38% | 4.24% |
| Infernus S.T | 91.91% | 90.68% | +1.23 pp | 93.06% | 61.40% |
| Whirlwind S.T | 91.13% | 92.10% | -0.97 pp | 93.53% | 47.80% |
| Servo-Skull | 91.02% | 91.62% | -0.60 pp | 91.05% | 22.03% |
| Demolitionist S.T | 91.54% | 91.38% | +0.16 pp | 93.24% | 47.04% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
