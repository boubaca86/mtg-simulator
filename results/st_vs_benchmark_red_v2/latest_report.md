# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 86,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.60% | +5.93 pp | 93.78% | 64.81% |
| Eradicator S.T | 93.13% | 88.60% | +4.54 pp | 95.08% | 58.05% |
| Drop Pod S.T | 93.00% | 88.73% | +4.27 pp | 93.31% | 57.64% |
| Pyroclast Squad S.T | 92.83% | 89.00% | +3.84 pp | 93.64% | 64.24% |
| Terminator S.T | 92.75% | 89.14% | +3.61 pp | 93.26% | 65.56% |
| Exterminatus S.T | 89.09% | 92.21% | -3.12 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.35% | -2.15 pp | 92.72% | 4.25% |
| Whirlwind S.T | 91.14% | 92.21% | -1.07 pp | 93.41% | 47.93% |
| Infernus S.T | 91.88% | 90.88% | +1.00 pp | 93.03% | 61.37% |
| Servo-Skull | 91.25% | 91.61% | -0.36 pp | 91.34% | 21.96% |
| Demolitionist S.T | 91.61% | 91.39% | +0.22 pp | 93.31% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
