# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 46,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.80% | +5.62 pp | 93.71% | 65.07% |
| Eradicator S.T | 93.24% | 88.45% | +4.78 pp | 95.05% | 58.03% |
| Drop Pod S.T | 93.10% | 88.57% | +4.53 pp | 93.42% | 57.46% |
| Pyroclast Squad S.T | 92.89% | 88.93% | +3.96 pp | 93.68% | 64.26% |
| Terminator S.T | 92.70% | 89.30% | +3.40 pp | 93.20% | 65.51% |
| Exterminatus S.T | 89.17% | 92.20% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.35% | -2.14 pp | 92.93% | 4.11% |
| Infernus S.T | 91.85% | 90.96% | +0.89 pp | 93.02% | 61.48% |
| Whirlwind S.T | 91.24% | 92.08% | -0.84 pp | 93.52% | 47.73% |
| Servo-Skull | 91.16% | 91.66% | -0.49 pp | 91.24% | 21.72% |
| Demolitionist S.T | 91.71% | 91.23% | +0.48 pp | 93.49% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
