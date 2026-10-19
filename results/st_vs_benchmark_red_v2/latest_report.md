# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 181,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.67% | +5.81 pp | 93.77% | 64.95% |
| Eradicator S.T | 93.12% | 88.63% | +4.49 pp | 95.03% | 57.84% |
| Drop Pod S.T | 92.97% | 88.81% | +4.16 pp | 93.22% | 57.48% |
| Terminator S.T | 92.80% | 89.07% | +3.73 pp | 93.29% | 65.45% |
| Pyroclast Squad S.T | 92.79% | 89.09% | +3.70 pp | 93.58% | 64.38% |
| Exterminatus S.T | 88.93% | 92.26% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.36% | -2.17 pp | 92.62% | 4.21% |
| Infernus S.T | 91.98% | 90.70% | +1.28 pp | 93.12% | 61.46% |
| Whirlwind S.T | 91.16% | 92.18% | -1.02 pp | 93.49% | 47.80% |
| Servo-Skull | 91.11% | 91.66% | -0.55 pp | 91.14% | 22.02% |
| Demolitionist S.T | 91.62% | 91.38% | +0.24 pp | 93.23% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
