# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 19,000
**S.T win rate:** 91.66%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.78% | 87.42% | +6.37 pp | 94.02% | 65.22% |
| Eradicator S.T | 93.36% | 88.53% | +4.83 pp | 95.01% | 58.38% |
| Drop Pod S.T | 93.00% | 89.09% | +3.90 pp | 93.29% | 57.71% |
| Pyroclast Squad S.T | 92.90% | 89.29% | +3.60 pp | 93.66% | 63.80% |
| Terminator S.T | 92.72% | 89.62% | +3.10 pp | 93.12% | 65.34% |
| Exterminatus S.T | 89.64% | 92.21% | -2.56 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.36% | 92.44% | -2.08 pp | 92.49% | 4.14% |
| Infernus S.T | 92.00% | 91.01% | +0.99 pp | 93.21% | 61.40% |
| Servo-Skull | 90.90% | 91.88% | -0.98 pp | 91.00% | 22.10% |
| Whirlwind S.T | 91.36% | 92.17% | -0.81 pp | 93.46% | 48.18% |
| Demolitionist S.T | 91.89% | 91.24% | +0.65 pp | 93.57% | 47.57% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
