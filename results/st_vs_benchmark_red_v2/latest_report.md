# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 67,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.71% | +5.76 pp | 93.71% | 64.90% |
| Eradicator S.T | 93.18% | 88.49% | +4.69 pp | 95.08% | 58.16% |
| Drop Pod S.T | 93.03% | 88.67% | +4.35 pp | 93.34% | 57.63% |
| Pyroclast Squad S.T | 92.84% | 88.98% | +3.86 pp | 93.65% | 64.19% |
| Terminator S.T | 92.74% | 89.18% | +3.56 pp | 93.25% | 65.45% |
| Exterminatus S.T | 89.17% | 92.18% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.33% | -2.10 pp | 92.88% | 4.21% |
| Infernus S.T | 91.88% | 90.87% | +1.01 pp | 93.04% | 61.40% |
| Whirlwind S.T | 91.18% | 92.12% | -0.94 pp | 93.44% | 47.85% |
| Servo-Skull | 91.29% | 91.60% | -0.31 pp | 91.37% | 21.97% |
| Demolitionist S.T | 91.61% | 91.38% | +0.23 pp | 93.32% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
