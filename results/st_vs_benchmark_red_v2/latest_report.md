# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 46,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.79% | +5.61 pp | 93.70% | 65.07% |
| Eradicator S.T | 93.21% | 88.46% | +4.75 pp | 95.03% | 58.03% |
| Drop Pod S.T | 93.08% | 88.57% | +4.51 pp | 93.40% | 57.48% |
| Pyroclast Squad S.T | 92.89% | 88.90% | +3.99 pp | 93.68% | 64.25% |
| Terminator S.T | 92.67% | 89.31% | +3.36 pp | 93.17% | 65.51% |
| Exterminatus S.T | 89.13% | 92.20% | -3.07 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.33% | -2.11 pp | 92.90% | 4.10% |
| Infernus S.T | 91.83% | 90.97% | +0.86 pp | 93.00% | 61.48% |
| Whirlwind S.T | 91.23% | 92.05% | -0.82 pp | 93.52% | 47.73% |
| Demolitionist S.T | 91.70% | 91.23% | +0.47 pp | 93.49% | 47.27% |
| Servo-Skull | 91.17% | 91.64% | -0.47 pp | 91.25% | 21.71% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
