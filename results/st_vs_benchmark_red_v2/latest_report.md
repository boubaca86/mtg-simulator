# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 35,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.64% | +5.80 pp | 93.72% | 64.97% |
| Eradicator S.T | 93.19% | 88.36% | +4.83 pp | 95.02% | 58.19% |
| Drop Pod S.T | 93.09% | 88.43% | +4.66 pp | 93.41% | 57.73% |
| Pyroclast Squad S.T | 92.85% | 88.86% | +3.99 pp | 93.63% | 64.14% |
| Exterminatus S.T | 89.11% | 92.15% | -3.04 pp | 0.00% | 0.00% |
| Terminator S.T | 92.52% | 89.48% | +3.04 pp | 93.00% | 65.56% |
| Vindicator S.T | 90.21% | 92.27% | -2.06 pp | 92.77% | 4.17% |
| Infernus S.T | 91.86% | 90.79% | +1.08 pp | 93.07% | 61.42% |
| Whirlwind S.T | 91.25% | 91.90% | -0.65 pp | 93.60% | 47.68% |
| Demolitionist S.T | 91.69% | 91.12% | +0.57 pp | 93.47% | 47.25% |
| Servo-Skull | 91.16% | 91.59% | -0.43 pp | 91.23% | 21.97% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
