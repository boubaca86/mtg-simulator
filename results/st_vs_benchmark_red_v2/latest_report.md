# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 43,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.68% | +5.74 pp | 93.72% | 65.13% |
| Eradicator S.T | 93.18% | 88.43% | +4.75 pp | 95.02% | 58.04% |
| Drop Pod S.T | 93.08% | 88.50% | +4.57 pp | 93.39% | 57.52% |
| Pyroclast Squad S.T | 92.88% | 88.83% | +4.05 pp | 93.68% | 64.27% |
| Terminator S.T | 92.60% | 89.36% | +3.25 pp | 93.12% | 65.53% |
| Exterminatus S.T | 89.07% | 92.17% | -3.10 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.33% | -2.20 pp | 92.77% | 4.10% |
| Whirlwind S.T | 91.19% | 92.04% | -0.85 pp | 93.49% | 47.70% |
| Infernus S.T | 91.76% | 91.02% | +0.75 pp | 92.95% | 61.43% |
| Demolitionist S.T | 91.67% | 91.21% | +0.46 pp | 93.49% | 47.12% |
| Servo-Skull | 91.17% | 91.60% | -0.44 pp | 91.25% | 21.71% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
