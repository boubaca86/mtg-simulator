# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 59,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.67% | +5.74 pp | 93.67% | 64.84% |
| Eradicator S.T | 93.16% | 88.40% | +4.76 pp | 95.05% | 58.09% |
| Drop Pod S.T | 92.94% | 88.69% | +4.25 pp | 93.26% | 57.57% |
| Pyroclast Squad S.T | 92.83% | 88.86% | +3.97 pp | 93.61% | 64.22% |
| Terminator S.T | 92.67% | 89.16% | +3.51 pp | 93.16% | 65.52% |
| Exterminatus S.T | 89.13% | 92.13% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.28% | -2.10 pp | 92.97% | 4.19% |
| Whirlwind S.T | 91.14% | 92.06% | -0.92 pp | 93.38% | 47.85% |
| Infernus S.T | 91.80% | 90.89% | +0.91 pp | 92.94% | 61.37% |
| Servo-Skull | 91.19% | 91.56% | -0.37 pp | 91.28% | 21.94% |
| Demolitionist S.T | 91.60% | 91.27% | +0.33 pp | 93.26% | 47.38% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
