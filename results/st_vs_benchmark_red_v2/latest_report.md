# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 140,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.70% | +5.83 pp | 93.80% | 64.99% |
| Eradicator S.T | 93.12% | 88.74% | +4.38 pp | 95.04% | 57.88% |
| Drop Pod S.T | 93.02% | 88.83% | +4.20 pp | 93.28% | 57.43% |
| Terminator S.T | 92.85% | 89.09% | +3.77 pp | 93.35% | 65.50% |
| Pyroclast Squad S.T | 92.83% | 89.12% | +3.71 pp | 93.64% | 64.41% |
| Exterminatus S.T | 88.93% | 92.31% | -3.38 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.36% | -2.07 pp | 92.81% | 4.23% |
| Infernus S.T | 92.00% | 90.77% | +1.23 pp | 93.15% | 61.52% |
| Whirlwind S.T | 91.19% | 92.25% | -1.06 pp | 93.49% | 47.85% |
| Servo-Skull | 91.18% | 91.69% | -0.51 pp | 91.22% | 21.95% |
| Demolitionist S.T | 91.65% | 91.43% | +0.22 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
