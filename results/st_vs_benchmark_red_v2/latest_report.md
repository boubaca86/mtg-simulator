# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 96,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.57% | +6.00 pp | 93.83% | 64.84% |
| Eradicator S.T | 93.16% | 88.60% | +4.56 pp | 95.09% | 58.01% |
| Drop Pod S.T | 93.03% | 88.74% | +4.29 pp | 93.33% | 57.52% |
| Pyroclast Squad S.T | 92.85% | 89.01% | +3.84 pp | 93.66% | 64.30% |
| Terminator S.T | 92.80% | 89.11% | +3.69 pp | 93.30% | 65.51% |
| Exterminatus S.T | 89.04% | 92.25% | -3.20 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.34% | -2.08 pp | 92.60% | 4.24% |
| Whirlwind S.T | 91.14% | 92.25% | -1.11 pp | 93.40% | 47.92% |
| Infernus S.T | 91.93% | 90.83% | +1.10 pp | 93.09% | 61.41% |
| Servo-Skull | 91.21% | 91.65% | -0.44 pp | 91.28% | 21.96% |
| Demolitionist S.T | 91.62% | 91.42% | +0.20 pp | 93.26% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
