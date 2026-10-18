# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 167,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.73% | +5.77 pp | 93.78% | 65.01% |
| Eradicator S.T | 93.14% | 88.69% | +4.45 pp | 95.05% | 57.84% |
| Drop Pod S.T | 92.99% | 88.86% | +4.13 pp | 93.25% | 57.48% |
| Terminator S.T | 92.85% | 89.08% | +3.76 pp | 93.33% | 65.49% |
| Pyroclast Squad S.T | 92.82% | 89.12% | +3.69 pp | 93.61% | 64.42% |
| Exterminatus S.T | 88.99% | 92.28% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.35% | -2.05 pp | 92.73% | 4.20% |
| Infernus S.T | 92.00% | 90.76% | +1.24 pp | 93.14% | 61.45% |
| Whirlwind S.T | 91.18% | 92.25% | -1.07 pp | 93.49% | 47.79% |
| Servo-Skull | 91.18% | 91.68% | -0.50 pp | 91.23% | 22.04% |
| Demolitionist S.T | 91.65% | 91.42% | +0.23 pp | 93.30% | 47.21% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
