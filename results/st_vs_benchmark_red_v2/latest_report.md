# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 183,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.67% | +5.80 pp | 93.76% | 64.95% |
| Eradicator S.T | 93.11% | 88.65% | +4.46 pp | 95.01% | 57.84% |
| Drop Pod S.T | 92.96% | 88.81% | +4.15 pp | 93.22% | 57.47% |
| Terminator S.T | 92.80% | 89.07% | +3.73 pp | 93.29% | 65.44% |
| Pyroclast Squad S.T | 92.78% | 89.08% | +3.70 pp | 93.58% | 64.39% |
| Exterminatus S.T | 88.91% | 92.25% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.35% | -2.17 pp | 92.62% | 4.21% |
| Infernus S.T | 91.97% | 90.70% | +1.27 pp | 93.11% | 61.46% |
| Whirlwind S.T | 91.15% | 92.18% | -1.03 pp | 93.49% | 47.81% |
| Servo-Skull | 91.09% | 91.66% | -0.58 pp | 91.12% | 22.03% |
| Demolitionist S.T | 91.62% | 91.36% | +0.26 pp | 93.23% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
