# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 117,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.68% | +5.88 pp | 93.84% | 64.96% |
| Eradicator S.T | 93.19% | 88.68% | +4.50 pp | 95.08% | 57.84% |
| Drop Pod S.T | 93.04% | 88.83% | +4.21 pp | 93.34% | 57.51% |
| Pyroclast Squad S.T | 92.85% | 89.12% | +3.73 pp | 93.65% | 64.47% |
| Terminator S.T | 92.85% | 89.12% | +3.73 pp | 93.34% | 65.57% |
| Exterminatus S.T | 89.06% | 92.29% | -3.23 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.35% | -2.01 pp | 92.72% | 4.24% |
| Infernus S.T | 92.00% | 90.80% | +1.20 pp | 93.15% | 61.51% |
| Whirlwind S.T | 91.19% | 92.28% | -1.09 pp | 93.44% | 47.82% |
| Servo-Skull | 91.22% | 91.70% | -0.48 pp | 91.29% | 22.01% |
| Demolitionist S.T | 91.64% | 91.48% | +0.16 pp | 93.30% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
