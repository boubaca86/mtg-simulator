# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 140,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.70% | +5.82 pp | 93.80% | 65.00% |
| Eradicator S.T | 93.11% | 88.74% | +4.37 pp | 95.04% | 57.88% |
| Drop Pod S.T | 93.02% | 88.83% | +4.19 pp | 93.27% | 57.43% |
| Terminator S.T | 92.85% | 89.07% | +3.78 pp | 93.35% | 65.50% |
| Pyroclast Squad S.T | 92.82% | 89.11% | +3.71 pp | 93.63% | 64.42% |
| Exterminatus S.T | 88.94% | 92.30% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.36% | -2.07 pp | 92.78% | 4.23% |
| Infernus S.T | 91.99% | 90.77% | +1.22 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.18% | 92.25% | -1.06 pp | 93.49% | 47.83% |
| Servo-Skull | 91.18% | 91.68% | -0.50 pp | 91.23% | 21.96% |
| Demolitionist S.T | 91.64% | 91.43% | +0.21 pp | 93.30% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
