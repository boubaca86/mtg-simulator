# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 79,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.68% | +5.85 pp | 93.78% | 64.93% |
| Eradicator S.T | 93.16% | 88.64% | +4.53 pp | 95.09% | 58.07% |
| Drop Pod S.T | 93.06% | 88.71% | +4.35 pp | 93.37% | 57.62% |
| Pyroclast Squad S.T | 92.86% | 89.06% | +3.79 pp | 93.68% | 64.19% |
| Terminator S.T | 92.80% | 89.16% | +3.64 pp | 93.31% | 65.66% |
| Exterminatus S.T | 89.25% | 92.21% | -2.96 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.39% | -2.16 pp | 92.81% | 4.21% |
| Whirlwind S.T | 91.17% | 92.25% | -1.08 pp | 93.45% | 47.90% |
| Infernus S.T | 91.91% | 90.92% | +1.00 pp | 93.06% | 61.34% |
| Servo-Skull | 91.24% | 91.66% | -0.42 pp | 91.32% | 21.91% |
| Demolitionist S.T | 91.66% | 91.39% | +0.27 pp | 93.35% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
