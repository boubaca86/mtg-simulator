# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 245,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.62% | +5.81 pp | 93.72% | 64.95% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.98% | 57.81% |
| Drop Pod S.T | 92.91% | 88.78% | +4.13 pp | 93.16% | 57.42% |
| Terminator S.T | 92.79% | 88.94% | +3.85 pp | 93.28% | 65.47% |
| Pyroclast Squad S.T | 92.73% | 89.05% | +3.68 pp | 93.54% | 64.27% |
| Exterminatus S.T | 88.81% | 92.22% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.30% | -2.16 pp | 92.48% | 4.23% |
| Infernus S.T | 91.92% | 90.67% | +1.24 pp | 93.04% | 61.39% |
| Whirlwind S.T | 91.11% | 92.14% | -1.03 pp | 93.48% | 47.85% |
| Servo-Skull | 91.06% | 91.61% | -0.55 pp | 91.08% | 22.06% |
| Demolitionist S.T | 91.58% | 91.31% | +0.26 pp | 93.23% | 47.08% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
