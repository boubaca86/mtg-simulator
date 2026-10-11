# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 84,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.52% | 87.67% | +5.85 pp | 93.78% | 64.82% |
| Eradicator S.T | 93.14% | 88.62% | +4.52 pp | 95.09% | 58.08% |
| Drop Pod S.T | 93.02% | 88.75% | +4.27 pp | 93.34% | 57.62% |
| Pyroclast Squad S.T | 92.82% | 89.08% | +3.74 pp | 93.63% | 64.25% |
| Terminator S.T | 92.78% | 89.13% | +3.65 pp | 93.30% | 65.59% |
| Exterminatus S.T | 89.18% | 92.21% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.36% | -2.13 pp | 92.75% | 4.24% |
| Whirlwind S.T | 91.16% | 92.23% | -1.08 pp | 93.43% | 47.90% |
| Infernus S.T | 91.91% | 90.87% | +1.03 pp | 93.06% | 61.34% |
| Servo-Skull | 91.28% | 91.63% | -0.34 pp | 91.36% | 21.95% |
| Demolitionist S.T | 91.63% | 91.40% | +0.23 pp | 93.32% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
