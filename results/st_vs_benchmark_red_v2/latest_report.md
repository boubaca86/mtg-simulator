# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 64,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.72% | +5.69 pp | 93.65% | 64.90% |
| Eradicator S.T | 93.15% | 88.44% | +4.71 pp | 95.05% | 58.14% |
| Drop Pod S.T | 92.99% | 88.65% | +4.34 pp | 93.30% | 57.59% |
| Pyroclast Squad S.T | 92.80% | 88.95% | +3.85 pp | 93.61% | 64.18% |
| Terminator S.T | 92.68% | 89.19% | +3.49 pp | 93.18% | 65.44% |
| Exterminatus S.T | 89.12% | 92.15% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.30% | -2.12 pp | 93.10% | 4.20% |
| Infernus S.T | 91.84% | 90.84% | +0.99 pp | 93.00% | 61.35% |
| Whirlwind S.T | 91.16% | 92.06% | -0.90 pp | 93.42% | 47.86% |
| Servo-Skull | 91.27% | 91.56% | -0.29 pp | 91.33% | 22.00% |
| Demolitionist S.T | 91.58% | 91.33% | +0.25 pp | 93.27% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
