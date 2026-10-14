# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 119,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.69% | +5.88 pp | 93.85% | 64.97% |
| Eradicator S.T | 93.19% | 88.69% | +4.51 pp | 95.08% | 57.83% |
| Drop Pod S.T | 93.04% | 88.85% | +4.20 pp | 93.33% | 57.49% |
| Pyroclast Squad S.T | 92.86% | 89.12% | +3.74 pp | 93.65% | 64.47% |
| Terminator S.T | 92.85% | 89.15% | +3.69 pp | 93.33% | 65.57% |
| Exterminatus S.T | 89.07% | 92.30% | -3.23 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.36% | 92.35% | -1.99 pp | 92.70% | 4.25% |
| Infernus S.T | 92.03% | 90.78% | +1.25 pp | 93.16% | 61.55% |
| Whirlwind S.T | 91.20% | 92.28% | -1.09 pp | 93.44% | 47.83% |
| Servo-Skull | 91.23% | 91.70% | -0.48 pp | 91.29% | 22.00% |
| Demolitionist S.T | 91.65% | 91.50% | +0.15 pp | 93.29% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
