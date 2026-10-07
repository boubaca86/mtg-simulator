# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 30,000
**S.T win rate:** 91.65%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.59% | 87.76% | +5.83 pp | 93.85% | 65.24% |
| Eradicator S.T | 93.35% | 88.52% | +4.83 pp | 95.10% | 58.45% |
| Drop Pod S.T | 93.06% | 88.93% | +4.14 pp | 93.36% | 57.78% |
| Pyroclast Squad S.T | 92.99% | 89.08% | +3.91 pp | 93.72% | 64.04% |
| Terminator S.T | 92.70% | 89.63% | +3.06 pp | 93.13% | 65.35% |
| Exterminatus S.T | 89.35% | 92.28% | -2.93 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.47% | -2.18 pp | 92.43% | 4.14% |
| Infernus S.T | 91.95% | 91.09% | +0.86 pp | 93.08% | 61.55% |
| Whirlwind S.T | 91.43% | 92.03% | -0.59 pp | 93.69% | 47.70% |
| Demolitionist S.T | 91.82% | 91.35% | +0.46 pp | 93.54% | 47.34% |
| Servo-Skull | 91.30% | 91.76% | -0.46 pp | 91.32% | 22.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
