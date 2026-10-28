# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 282,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.64% | +5.80 pp | 93.73% | 64.97% |
| Eradicator S.T | 93.05% | 88.66% | +4.39 pp | 94.98% | 57.78% |
| Drop Pod S.T | 92.91% | 88.81% | +4.11 pp | 93.16% | 57.41% |
| Terminator S.T | 92.80% | 88.96% | +3.84 pp | 93.29% | 65.46% |
| Pyroclast Squad S.T | 92.76% | 89.04% | +3.72 pp | 93.56% | 64.29% |
| Exterminatus S.T | 88.92% | 92.21% | -3.29 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.33% | -2.21 pp | 92.35% | 4.24% |
| Infernus S.T | 91.92% | 90.69% | +1.23 pp | 93.05% | 61.34% |
| Whirlwind S.T | 91.13% | 92.13% | -1.00 pp | 93.50% | 47.79% |
| Servo-Skull | 91.08% | 91.62% | -0.54 pp | 91.09% | 22.05% |
| Demolitionist S.T | 91.58% | 91.34% | +0.24 pp | 93.24% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
