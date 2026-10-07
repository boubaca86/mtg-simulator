# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 33,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.51% | +6.02 pp | 93.80% | 65.08% |
| Eradicator S.T | 93.22% | 88.38% | +4.85 pp | 94.99% | 58.37% |
| Drop Pod S.T | 93.07% | 88.54% | +4.53 pp | 93.40% | 57.71% |
| Pyroclast Squad S.T | 92.86% | 88.93% | +3.93 pp | 93.61% | 64.17% |
| Exterminatus S.T | 89.08% | 92.19% | -3.11 pp | 0.00% | 0.00% |
| Terminator S.T | 92.56% | 89.50% | +3.06 pp | 93.03% | 65.49% |
| Vindicator S.T | 90.20% | 92.31% | -2.11 pp | 92.78% | 4.09% |
| Infernus S.T | 91.85% | 90.90% | +0.95 pp | 93.02% | 61.52% |
| Whirlwind S.T | 91.28% | 91.93% | -0.65 pp | 93.57% | 47.79% |
| Demolitionist S.T | 91.71% | 91.18% | +0.53 pp | 93.46% | 47.26% |
| Servo-Skull | 91.24% | 91.60% | -0.36 pp | 91.31% | 21.97% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
