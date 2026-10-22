# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 213,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.67% | +5.79 pp | 93.75% | 64.91% |
| Eradicator S.T | 93.08% | 88.69% | +4.39 pp | 94.99% | 57.82% |
| Drop Pod S.T | 92.96% | 88.79% | +4.18 pp | 93.21% | 57.46% |
| Terminator S.T | 92.82% | 89.01% | +3.81 pp | 93.31% | 65.46% |
| Pyroclast Squad S.T | 92.78% | 89.08% | +3.70 pp | 93.60% | 64.31% |
| Exterminatus S.T | 88.84% | 92.26% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.35% | -2.18 pp | 92.56% | 4.23% |
| Infernus S.T | 91.96% | 90.71% | +1.25 pp | 93.09% | 61.42% |
| Whirlwind S.T | 91.17% | 92.14% | -0.97 pp | 93.52% | 47.85% |
| Servo-Skull | 91.08% | 91.65% | -0.57 pp | 91.11% | 22.00% |
| Demolitionist S.T | 91.63% | 91.33% | +0.30 pp | 93.26% | 47.17% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
