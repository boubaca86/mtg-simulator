# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 109,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.59% | +5.97 pp | 93.83% | 64.90% |
| Eradicator S.T | 93.15% | 88.64% | +4.51 pp | 95.06% | 57.88% |
| Drop Pod S.T | 93.01% | 88.79% | +4.22 pp | 93.31% | 57.51% |
| Pyroclast Squad S.T | 92.85% | 89.02% | +3.82 pp | 93.65% | 64.43% |
| Terminator S.T | 92.84% | 89.03% | +3.81 pp | 93.34% | 65.54% |
| Exterminatus S.T | 89.06% | 92.24% | -3.18 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.31% | -2.00 pp | 92.67% | 4.25% |
| Infernus S.T | 91.96% | 90.78% | +1.18 pp | 93.11% | 61.52% |
| Whirlwind S.T | 91.13% | 92.29% | -1.17 pp | 93.38% | 47.83% |
| Servo-Skull | 91.20% | 91.66% | -0.46 pp | 91.26% | 22.01% |
| Demolitionist S.T | 91.59% | 91.48% | +0.11 pp | 93.25% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
