# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 82,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.67% | +5.87 pp | 93.79% | 64.87% |
| Eradicator S.T | 93.16% | 88.64% | +4.52 pp | 95.09% | 58.08% |
| Drop Pod S.T | 93.04% | 88.75% | +4.30 pp | 93.35% | 57.62% |
| Pyroclast Squad S.T | 92.84% | 89.09% | +3.75 pp | 93.66% | 64.23% |
| Terminator S.T | 92.79% | 89.16% | +3.63 pp | 93.31% | 65.60% |
| Exterminatus S.T | 89.21% | 92.22% | -3.01 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.38% | -2.14 pp | 92.76% | 4.23% |
| Whirlwind S.T | 91.17% | 92.24% | -1.07 pp | 93.46% | 47.91% |
| Infernus S.T | 91.93% | 90.88% | +1.06 pp | 93.08% | 61.35% |
| Servo-Skull | 91.29% | 91.65% | -0.36 pp | 91.37% | 21.88% |
| Demolitionist S.T | 91.65% | 91.41% | +0.24 pp | 93.33% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
