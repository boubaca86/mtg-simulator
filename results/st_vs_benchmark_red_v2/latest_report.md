# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 72,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.70% | +5.77 pp | 93.71% | 64.95% |
| Eradicator S.T | 93.17% | 88.50% | +4.67 pp | 95.08% | 58.14% |
| Drop Pod S.T | 93.03% | 88.66% | +4.38 pp | 93.33% | 57.61% |
| Pyroclast Squad S.T | 92.85% | 88.98% | +3.87 pp | 93.68% | 64.15% |
| Terminator S.T | 92.75% | 89.15% | +3.60 pp | 93.27% | 65.53% |
| Exterminatus S.T | 89.26% | 92.16% | -2.89 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.32% | -2.08 pp | 92.98% | 4.20% |
| Whirlwind S.T | 91.14% | 92.19% | -1.04 pp | 93.42% | 47.79% |
| Infernus S.T | 91.85% | 90.91% | +0.94 pp | 93.02% | 61.38% |
| Demolitionist S.T | 91.65% | 91.30% | +0.35 pp | 93.34% | 47.28% |
| Servo-Skull | 91.28% | 91.60% | -0.32 pp | 91.36% | 21.97% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
