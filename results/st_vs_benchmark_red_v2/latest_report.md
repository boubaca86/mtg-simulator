# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 17,500
**S.T win rate:** 91.69%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.75% | 87.55% | +6.20 pp | 94.01% | 65.22% |
| Eradicator S.T | 93.43% | 88.50% | +4.93 pp | 95.10% | 58.32% |
| Drop Pod S.T | 93.00% | 89.17% | +3.83 pp | 93.34% | 57.72% |
| Pyroclast Squad S.T | 92.90% | 89.36% | +3.54 pp | 93.66% | 63.84% |
| Terminator S.T | 92.68% | 89.77% | +2.91 pp | 93.08% | 65.29% |
| Exterminatus S.T | 89.45% | 92.29% | -2.84 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.44% | 92.43% | -1.99 pp | 92.87% | 4.17% |
| Infernus S.T | 92.08% | 90.93% | +1.16 pp | 93.24% | 61.61% |
| Whirlwind S.T | 91.30% | 92.35% | -1.05 pp | 93.51% | 48.13% |
| Servo-Skull | 90.91% | 91.92% | -1.00 pp | 91.01% | 22.19% |
| Demolitionist S.T | 91.87% | 91.36% | +0.51 pp | 93.59% | 47.61% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
