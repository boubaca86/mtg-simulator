# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 62,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.39% | 87.74% | +5.65 pp | 93.64% | 64.90% |
| Eradicator S.T | 93.17% | 88.40% | +4.77 pp | 95.06% | 58.12% |
| Drop Pod S.T | 92.97% | 88.66% | +4.31 pp | 93.29% | 57.55% |
| Pyroclast Squad S.T | 92.82% | 88.91% | +3.91 pp | 93.60% | 64.21% |
| Terminator S.T | 92.70% | 89.14% | +3.56 pp | 93.19% | 65.47% |
| Exterminatus S.T | 89.15% | 92.14% | -2.99 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.26% | -2.03 pp | 92.94% | 4.20% |
| Infernus S.T | 91.83% | 90.84% | +0.99 pp | 92.97% | 61.39% |
| Whirlwind S.T | 91.16% | 92.05% | -0.89 pp | 93.40% | 47.84% |
| Servo-Skull | 91.23% | 91.56% | -0.34 pp | 91.30% | 22.00% |
| Demolitionist S.T | 91.60% | 91.28% | +0.32 pp | 93.28% | 47.32% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
