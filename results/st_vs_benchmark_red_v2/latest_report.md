# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 19,500
**S.T win rate:** 91.64%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.75% | 87.44% | +6.30 pp | 93.99% | 65.12% |
| Eradicator S.T | 93.38% | 88.44% | +4.94 pp | 95.03% | 58.45% |
| Drop Pod S.T | 93.03% | 88.97% | +4.06 pp | 93.30% | 57.75% |
| Pyroclast Squad S.T | 92.87% | 89.29% | +3.58 pp | 93.65% | 63.66% |
| Terminator S.T | 92.70% | 89.59% | +3.11 pp | 93.10% | 65.32% |
| Exterminatus S.T | 89.55% | 92.21% | -2.65 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.46% | -2.20 pp | 92.32% | 4.14% |
| Infernus S.T | 92.01% | 90.93% | +1.07 pp | 93.20% | 61.46% |
| Servo-Skull | 90.83% | 91.88% | -1.05 pp | 90.91% | 22.17% |
| Whirlwind S.T | 91.35% | 92.14% | -0.80 pp | 93.44% | 48.29% |
| Demolitionist S.T | 91.86% | 91.23% | +0.63 pp | 93.54% | 47.64% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
