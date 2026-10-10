# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 70,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.70% | +5.77 pp | 93.71% | 64.94% |
| Eradicator S.T | 93.18% | 88.49% | +4.70 pp | 95.09% | 58.16% |
| Drop Pod S.T | 93.03% | 88.68% | +4.35 pp | 93.33% | 57.57% |
| Pyroclast Squad S.T | 92.85% | 88.97% | +3.88 pp | 93.68% | 64.17% |
| Terminator S.T | 92.75% | 89.15% | +3.61 pp | 93.27% | 65.50% |
| Exterminatus S.T | 89.22% | 92.17% | -2.96 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.31% | -2.05 pp | 93.05% | 4.21% |
| Whirlwind S.T | 91.18% | 92.14% | -0.97 pp | 93.44% | 47.82% |
| Infernus S.T | 91.86% | 90.91% | +0.96 pp | 93.03% | 61.42% |
| Servo-Skull | 91.28% | 91.60% | -0.32 pp | 91.37% | 21.98% |
| Demolitionist S.T | 91.62% | 91.37% | +0.25 pp | 93.32% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
