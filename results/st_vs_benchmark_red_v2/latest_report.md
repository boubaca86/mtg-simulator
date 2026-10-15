# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 132,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.69% | +5.86 pp | 93.82% | 65.01% |
| Eradicator S.T | 93.15% | 88.74% | +4.42 pp | 95.07% | 57.84% |
| Drop Pod S.T | 93.02% | 88.86% | +4.16 pp | 93.30% | 57.47% |
| Terminator S.T | 92.85% | 89.14% | +3.71 pp | 93.35% | 65.53% |
| Pyroclast Squad S.T | 92.84% | 89.15% | +3.69 pp | 93.64% | 64.42% |
| Exterminatus S.T | 89.00% | 92.31% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.36% | -2.03 pp | 92.70% | 4.24% |
| Infernus S.T | 92.02% | 90.77% | +1.25 pp | 93.16% | 61.57% |
| Whirlwind S.T | 91.20% | 92.26% | -1.05 pp | 93.49% | 47.88% |
| Servo-Skull | 91.24% | 91.69% | -0.45 pp | 91.29% | 21.93% |
| Demolitionist S.T | 91.65% | 91.47% | +0.18 pp | 93.29% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
