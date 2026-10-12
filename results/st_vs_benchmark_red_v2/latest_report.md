# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 94,500
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.58% | 87.60% | +5.97 pp | 93.84% | 64.83% |
| Eradicator S.T | 93.16% | 88.64% | +4.52 pp | 95.10% | 58.04% |
| Drop Pod S.T | 93.05% | 88.74% | +4.31 pp | 93.35% | 57.57% |
| Pyroclast Squad S.T | 92.87% | 89.02% | +3.85 pp | 93.68% | 64.30% |
| Terminator S.T | 92.80% | 89.16% | +3.64 pp | 93.30% | 65.53% |
| Exterminatus S.T | 89.05% | 92.26% | -3.22 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.35% | -2.06 pp | 92.81% | 4.24% |
| Whirlwind S.T | 91.17% | 92.24% | -1.07 pp | 93.44% | 47.90% |
| Infernus S.T | 91.93% | 90.88% | +1.05 pp | 93.08% | 61.42% |
| Servo-Skull | 91.26% | 91.66% | -0.40 pp | 91.33% | 21.96% |
| Demolitionist S.T | 91.63% | 91.44% | +0.19 pp | 93.29% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
