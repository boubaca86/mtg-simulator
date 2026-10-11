# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 81,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.68% | +5.87 pp | 93.80% | 64.87% |
| Eradicator S.T | 93.16% | 88.65% | +4.52 pp | 95.10% | 58.08% |
| Drop Pod S.T | 93.04% | 88.76% | +4.29 pp | 93.35% | 57.63% |
| Pyroclast Squad S.T | 92.84% | 89.11% | +3.72 pp | 93.66% | 64.21% |
| Terminator S.T | 92.79% | 89.19% | +3.60 pp | 93.30% | 65.62% |
| Exterminatus S.T | 89.22% | 92.22% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.40% | -2.17 pp | 92.76% | 4.24% |
| Whirlwind S.T | 91.17% | 92.26% | -1.09 pp | 93.46% | 47.92% |
| Infernus S.T | 91.94% | 90.89% | +1.05 pp | 93.08% | 61.33% |
| Servo-Skull | 91.30% | 91.65% | -0.35 pp | 91.38% | 21.88% |
| Demolitionist S.T | 91.66% | 91.42% | +0.24 pp | 93.35% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
