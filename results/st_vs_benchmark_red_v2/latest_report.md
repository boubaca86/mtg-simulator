# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 97,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.57% | +6.00 pp | 93.83% | 64.85% |
| Eradicator S.T | 93.16% | 88.59% | +4.57 pp | 95.09% | 57.99% |
| Drop Pod S.T | 93.02% | 88.74% | +4.29 pp | 93.32% | 57.51% |
| Pyroclast Squad S.T | 92.86% | 89.00% | +3.86 pp | 93.66% | 64.30% |
| Terminator S.T | 92.80% | 89.10% | +3.70 pp | 93.30% | 65.52% |
| Exterminatus S.T | 89.04% | 92.24% | -3.20 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.34% | -2.10 pp | 92.56% | 4.23% |
| Whirlwind S.T | 91.14% | 92.25% | -1.11 pp | 93.40% | 47.90% |
| Infernus S.T | 91.93% | 90.82% | +1.11 pp | 93.09% | 61.44% |
| Servo-Skull | 91.19% | 91.65% | -0.46 pp | 91.25% | 21.96% |
| Demolitionist S.T | 91.61% | 91.42% | +0.20 pp | 93.25% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
