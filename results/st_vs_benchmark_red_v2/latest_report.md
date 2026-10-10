# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 72,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.70% | +5.78 pp | 93.72% | 64.94% |
| Eradicator S.T | 93.17% | 88.53% | +4.64 pp | 95.07% | 58.12% |
| Drop Pod S.T | 93.04% | 88.67% | +4.36 pp | 93.33% | 57.61% |
| Pyroclast Squad S.T | 92.85% | 88.99% | +3.86 pp | 93.68% | 64.15% |
| Terminator S.T | 92.75% | 89.16% | +3.59 pp | 93.28% | 65.53% |
| Exterminatus S.T | 89.27% | 92.16% | -2.90 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.33% | -2.09 pp | 93.04% | 4.20% |
| Whirlwind S.T | 91.15% | 92.20% | -1.04 pp | 93.43% | 47.81% |
| Infernus S.T | 91.87% | 90.91% | +0.95 pp | 93.03% | 61.38% |
| Demolitionist S.T | 91.66% | 91.31% | +0.35 pp | 93.34% | 47.29% |
| Servo-Skull | 91.28% | 91.61% | -0.33 pp | 91.36% | 21.95% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
