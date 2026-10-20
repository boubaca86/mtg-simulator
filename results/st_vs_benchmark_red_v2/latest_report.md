# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 193,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.66% | +5.82 pp | 93.76% | 64.90% |
| Eradicator S.T | 93.09% | 88.66% | +4.43 pp | 95.00% | 57.85% |
| Drop Pod S.T | 92.96% | 88.81% | +4.15 pp | 93.21% | 57.43% |
| Pyroclast Squad S.T | 92.78% | 89.06% | +3.72 pp | 93.59% | 64.37% |
| Terminator S.T | 92.78% | 89.08% | +3.70 pp | 93.28% | 65.44% |
| Exterminatus S.T | 88.85% | 92.26% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.34% | -2.15 pp | 92.57% | 4.22% |
| Infernus S.T | 91.94% | 90.73% | +1.21 pp | 93.09% | 61.43% |
| Whirlwind S.T | 91.17% | 92.14% | -0.97 pp | 93.52% | 47.82% |
| Servo-Skull | 91.08% | 91.65% | -0.57 pp | 91.11% | 22.00% |
| Demolitionist S.T | 91.62% | 91.34% | +0.28 pp | 93.23% | 47.16% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
