# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 25,000
**S.T win rate:** 91.60%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.67% | 87.44% | +6.23 pp | 93.94% | 65.28% |
| Eradicator S.T | 93.23% | 88.60% | +4.63 pp | 95.01% | 58.31% |
| Drop Pod S.T | 93.02% | 88.89% | +4.13 pp | 93.36% | 57.55% |
| Pyroclast Squad S.T | 92.91% | 89.10% | +3.81 pp | 93.64% | 63.82% |
| Terminator S.T | 92.57% | 89.73% | +2.84 pp | 93.01% | 65.42% |
| Exterminatus S.T | 89.49% | 92.17% | -2.68 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.09% | 92.51% | -2.42 pp | 92.06% | 4.18% |
| Infernus S.T | 91.89% | 91.06% | +0.83 pp | 93.04% | 61.59% |
| Demolitionist S.T | 91.83% | 91.19% | +0.64 pp | 93.58% | 47.31% |
| Whirlwind S.T | 91.37% | 91.99% | -0.62 pp | 93.63% | 47.96% |
| Servo-Skull | 91.14% | 91.74% | -0.60 pp | 91.19% | 22.11% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
