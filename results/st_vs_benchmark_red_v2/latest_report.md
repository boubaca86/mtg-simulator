# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 71,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.67% | +5.81 pp | 93.72% | 64.97% |
| Eradicator S.T | 93.19% | 88.48% | +4.70 pp | 95.09% | 58.14% |
| Drop Pod S.T | 93.03% | 88.68% | +4.35 pp | 93.33% | 57.61% |
| Pyroclast Squad S.T | 92.85% | 88.97% | +3.88 pp | 93.68% | 64.15% |
| Terminator S.T | 92.76% | 89.14% | +3.62 pp | 93.28% | 65.52% |
| Exterminatus S.T | 89.26% | 92.16% | -2.90 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.31% | -2.06 pp | 93.04% | 4.20% |
| Whirlwind S.T | 91.15% | 92.18% | -1.03 pp | 93.41% | 47.80% |
| Infernus S.T | 91.86% | 90.91% | +0.95 pp | 93.01% | 61.37% |
| Servo-Skull | 91.28% | 91.61% | -0.33 pp | 91.36% | 21.97% |
| Demolitionist S.T | 91.64% | 91.33% | +0.31 pp | 93.32% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
