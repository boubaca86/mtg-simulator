# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 82,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.69% | +5.85 pp | 93.79% | 64.88% |
| Eradicator S.T | 93.16% | 88.65% | +4.51 pp | 95.09% | 58.09% |
| Drop Pod S.T | 93.05% | 88.74% | +4.32 pp | 93.37% | 57.61% |
| Pyroclast Squad S.T | 92.85% | 89.07% | +3.78 pp | 93.67% | 64.22% |
| Terminator S.T | 92.79% | 89.17% | +3.62 pp | 93.31% | 65.60% |
| Exterminatus S.T | 89.20% | 92.22% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.38% | -2.14 pp | 92.76% | 4.24% |
| Whirlwind S.T | 91.18% | 92.25% | -1.07 pp | 93.46% | 47.91% |
| Infernus S.T | 91.94% | 90.87% | +1.06 pp | 93.08% | 61.33% |
| Servo-Skull | 91.31% | 91.64% | -0.33 pp | 91.39% | 21.89% |
| Demolitionist S.T | 91.65% | 91.42% | +0.23 pp | 93.33% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
