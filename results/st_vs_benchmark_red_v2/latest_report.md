# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 41,500
**S.T win rate:** 91.46%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.37% | 87.64% | +5.73 pp | 93.67% | 65.08% |
| Eradicator S.T | 93.14% | 88.36% | +4.78 pp | 94.99% | 58.11% |
| Drop Pod S.T | 93.04% | 88.43% | +4.62 pp | 93.37% | 57.58% |
| Pyroclast Squad S.T | 92.85% | 88.76% | +4.09 pp | 93.65% | 64.21% |
| Terminator S.T | 92.53% | 89.37% | +3.17 pp | 93.04% | 65.48% |
| Exterminatus S.T | 89.09% | 92.10% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.09% | 92.28% | -2.19 pp | 92.63% | 4.08% |
| Infernus S.T | 91.74% | 90.92% | +0.82 pp | 92.94% | 61.39% |
| Whirlwind S.T | 91.16% | 91.97% | -0.81 pp | 93.48% | 47.72% |
| Demolitionist S.T | 91.62% | 91.16% | +0.47 pp | 93.44% | 47.17% |
| Servo-Skull | 91.10% | 91.56% | -0.46 pp | 91.17% | 21.80% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
