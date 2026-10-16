# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 144,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.71% | +5.82 pp | 93.80% | 64.97% |
| Eradicator S.T | 93.14% | 88.71% | +4.43 pp | 95.06% | 57.87% |
| Drop Pod S.T | 93.03% | 88.82% | +4.21 pp | 93.28% | 57.46% |
| Terminator S.T | 92.84% | 89.10% | +3.75 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.82% | 89.13% | +3.70 pp | 93.64% | 64.41% |
| Exterminatus S.T | 88.94% | 92.30% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.37% | -2.09 pp | 92.80% | 4.21% |
| Infernus S.T | 91.99% | 90.79% | +1.20 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.19% | 92.24% | -1.05 pp | 93.50% | 47.85% |
| Servo-Skull | 91.21% | 91.68% | -0.48 pp | 91.25% | 21.98% |
| Demolitionist S.T | 91.67% | 91.40% | +0.28 pp | 93.33% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
