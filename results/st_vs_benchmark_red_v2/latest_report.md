# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 61,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.70% | +5.72 pp | 93.67% | 64.90% |
| Eradicator S.T | 93.17% | 88.41% | +4.75 pp | 95.07% | 58.07% |
| Drop Pod S.T | 92.97% | 88.67% | +4.30 pp | 93.29% | 57.55% |
| Pyroclast Squad S.T | 92.84% | 88.89% | +3.94 pp | 93.61% | 64.21% |
| Terminator S.T | 92.69% | 89.17% | +3.52 pp | 93.18% | 65.52% |
| Exterminatus S.T | 89.19% | 92.13% | -2.94 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.27% | -2.04 pp | 92.99% | 4.18% |
| Whirlwind S.T | 91.15% | 92.09% | -0.94 pp | 93.37% | 47.85% |
| Infernus S.T | 91.82% | 90.88% | +0.94 pp | 92.96% | 61.35% |
| Servo-Skull | 91.22% | 91.57% | -0.36 pp | 91.30% | 21.93% |
| Demolitionist S.T | 91.60% | 91.30% | +0.30 pp | 93.26% | 47.36% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
