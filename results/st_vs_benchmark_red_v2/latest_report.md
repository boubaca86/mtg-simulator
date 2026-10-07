# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 39,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.68% | +5.76 pp | 93.73% | 65.03% |
| Eradicator S.T | 93.18% | 88.42% | +4.76 pp | 95.04% | 58.22% |
| Drop Pod S.T | 93.13% | 88.40% | +4.73 pp | 93.47% | 57.67% |
| Pyroclast Squad S.T | 92.89% | 88.83% | +4.05 pp | 93.69% | 64.16% |
| Exterminatus S.T | 89.10% | 92.17% | -3.07 pp | 0.00% | 0.00% |
| Terminator S.T | 92.54% | 89.49% | +3.05 pp | 93.05% | 65.51% |
| Vindicator S.T | 90.24% | 92.27% | -2.04 pp | 92.67% | 4.13% |
| Infernus S.T | 91.80% | 90.95% | +0.86 pp | 93.01% | 61.39% |
| Whirlwind S.T | 91.24% | 91.96% | -0.72 pp | 93.62% | 47.67% |
| Servo-Skull | 91.06% | 91.64% | -0.58 pp | 91.12% | 21.84% |
| Demolitionist S.T | 91.67% | 91.22% | +0.45 pp | 93.53% | 47.14% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
