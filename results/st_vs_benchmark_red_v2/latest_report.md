# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 23,500
**S.T win rate:** 91.60%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.65% | 87.52% | +6.14 pp | 93.93% | 65.24% |
| Eradicator S.T | 93.28% | 88.54% | +4.74 pp | 95.04% | 58.34% |
| Drop Pod S.T | 93.05% | 88.84% | +4.21 pp | 93.37% | 57.59% |
| Pyroclast Squad S.T | 93.01% | 88.94% | +4.07 pp | 93.74% | 63.67% |
| Terminator S.T | 92.52% | 89.83% | +2.69 pp | 92.96% | 65.44% |
| Exterminatus S.T | 89.49% | 92.18% | -2.69 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.06% | 92.53% | -2.47 pp | 92.19% | 4.14% |
| Infernus S.T | 91.95% | 90.96% | +0.98 pp | 93.11% | 61.49% |
| Demolitionist S.T | 91.87% | 91.13% | +0.73 pp | 93.53% | 47.39% |
| Servo-Skull | 91.09% | 91.76% | -0.66 pp | 91.15% | 22.12% |
| Whirlwind S.T | 91.37% | 92.02% | -0.65 pp | 93.57% | 48.12% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
