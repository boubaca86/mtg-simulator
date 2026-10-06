# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 22,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.74% | 87.31% | +6.42 pp | 93.98% | 65.24% |
| Eradicator S.T | 93.32% | 88.42% | +4.90 pp | 95.01% | 58.35% |
| Drop Pod S.T | 93.04% | 88.83% | +4.21 pp | 93.33% | 57.61% |
| Pyroclast Squad S.T | 92.95% | 89.04% | +3.91 pp | 93.69% | 63.55% |
| Terminator S.T | 92.58% | 89.68% | +2.90 pp | 92.98% | 65.47% |
| Exterminatus S.T | 89.51% | 92.16% | -2.64 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.46% | -2.32 pp | 92.11% | 4.15% |
| Infernus S.T | 91.97% | 90.88% | +1.08 pp | 93.14% | 61.59% |
| Servo-Skull | 90.99% | 91.77% | -0.78 pp | 91.08% | 22.17% |
| Whirlwind S.T | 91.33% | 92.05% | -0.71 pp | 93.52% | 48.25% |
| Demolitionist S.T | 91.83% | 91.16% | +0.67 pp | 93.51% | 47.39% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
