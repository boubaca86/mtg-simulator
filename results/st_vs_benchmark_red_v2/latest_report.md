# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 81,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.69% | +5.84 pp | 93.78% | 64.85% |
| Eradicator S.T | 93.16% | 88.65% | +4.51 pp | 95.09% | 58.07% |
| Drop Pod S.T | 93.05% | 88.74% | +4.31 pp | 93.36% | 57.60% |
| Pyroclast Squad S.T | 92.83% | 89.11% | +3.72 pp | 93.66% | 64.21% |
| Terminator S.T | 92.79% | 89.18% | +3.61 pp | 93.30% | 65.63% |
| Exterminatus S.T | 89.23% | 92.22% | -2.99 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.39% | -2.17 pp | 92.74% | 4.23% |
| Whirlwind S.T | 91.17% | 92.26% | -1.09 pp | 93.45% | 47.94% |
| Infernus S.T | 91.93% | 90.90% | +1.03 pp | 93.08% | 61.33% |
| Servo-Skull | 91.30% | 91.65% | -0.35 pp | 91.38% | 21.87% |
| Demolitionist S.T | 91.66% | 91.41% | +0.25 pp | 93.35% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
