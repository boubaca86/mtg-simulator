# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 68,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.70% | +5.76 pp | 93.71% | 64.94% |
| Eradicator S.T | 93.19% | 88.47% | +4.72 pp | 95.09% | 58.19% |
| Drop Pod S.T | 93.03% | 88.67% | +4.36 pp | 93.34% | 57.59% |
| Pyroclast Squad S.T | 92.85% | 88.97% | +3.88 pp | 93.67% | 64.15% |
| Terminator S.T | 92.74% | 89.17% | +3.57 pp | 93.26% | 65.49% |
| Exterminatus S.T | 89.20% | 92.18% | -2.98 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.33% | -2.10 pp | 92.91% | 4.22% |
| Infernus S.T | 91.88% | 90.87% | +1.00 pp | 93.04% | 61.42% |
| Whirlwind S.T | 91.19% | 92.12% | -0.93 pp | 93.45% | 47.84% |
| Servo-Skull | 91.28% | 91.60% | -0.33 pp | 91.36% | 21.97% |
| Demolitionist S.T | 91.63% | 91.35% | +0.28 pp | 93.32% | 47.20% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
