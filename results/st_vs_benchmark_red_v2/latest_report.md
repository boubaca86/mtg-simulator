# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 175,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.67% | +5.84 pp | 93.80% | 64.95% |
| Eradicator S.T | 93.13% | 88.67% | +4.46 pp | 95.04% | 57.86% |
| Drop Pod S.T | 92.99% | 88.84% | +4.15 pp | 93.24% | 57.49% |
| Pyroclast Squad S.T | 92.81% | 89.10% | +3.71 pp | 93.59% | 64.38% |
| Terminator S.T | 92.81% | 89.12% | +3.69 pp | 93.30% | 65.45% |
| Exterminatus S.T | 88.95% | 92.28% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.36% | -2.11 pp | 92.66% | 4.21% |
| Infernus S.T | 91.99% | 90.73% | +1.26 pp | 93.13% | 61.48% |
| Whirlwind S.T | 91.17% | 92.23% | -1.06 pp | 93.49% | 47.80% |
| Servo-Skull | 91.16% | 91.67% | -0.51 pp | 91.21% | 22.03% |
| Demolitionist S.T | 91.63% | 91.41% | +0.22 pp | 93.26% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
