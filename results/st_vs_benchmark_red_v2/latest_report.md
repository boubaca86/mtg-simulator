# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 198,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.64% | +5.83 pp | 93.76% | 64.92% |
| Eradicator S.T | 93.08% | 88.66% | +4.42 pp | 95.00% | 57.81% |
| Drop Pod S.T | 92.93% | 88.83% | +4.11 pp | 93.19% | 57.41% |
| Terminator S.T | 92.79% | 89.04% | +3.76 pp | 93.29% | 65.45% |
| Pyroclast Squad S.T | 92.78% | 89.05% | +3.72 pp | 93.58% | 64.35% |
| Exterminatus S.T | 88.85% | 92.25% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.35% | -2.19 pp | 92.56% | 4.22% |
| Infernus S.T | 91.93% | 90.73% | +1.20 pp | 93.07% | 61.41% |
| Whirlwind S.T | 91.16% | 92.13% | -0.97 pp | 93.52% | 47.85% |
| Servo-Skull | 91.08% | 91.64% | -0.56 pp | 91.11% | 22.01% |
| Demolitionist S.T | 91.62% | 91.33% | +0.28 pp | 93.23% | 47.12% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
