# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 274,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.63% | +5.80 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.05% | 88.64% | +4.41 pp | 94.98% | 57.82% |
| Drop Pod S.T | 92.91% | 88.79% | +4.12 pp | 93.16% | 57.40% |
| Terminator S.T | 92.80% | 88.94% | +3.86 pp | 93.29% | 65.45% |
| Pyroclast Squad S.T | 92.74% | 89.05% | +3.68 pp | 93.54% | 64.28% |
| Exterminatus S.T | 88.92% | 92.20% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.32% | -2.20 pp | 92.36% | 4.24% |
| Infernus S.T | 91.91% | 90.69% | +1.22 pp | 93.03% | 61.35% |
| Whirlwind S.T | 91.11% | 92.14% | -1.03 pp | 93.49% | 47.81% |
| Servo-Skull | 91.08% | 91.61% | -0.53 pp | 91.10% | 22.03% |
| Demolitionist S.T | 91.57% | 91.34% | +0.23 pp | 93.23% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
