# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 44,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.66% | +5.75 pp | 93.70% | 65.13% |
| Eradicator S.T | 93.20% | 88.36% | +4.83 pp | 95.02% | 58.07% |
| Drop Pod S.T | 93.04% | 88.52% | +4.53 pp | 93.35% | 57.52% |
| Pyroclast Squad S.T | 92.87% | 88.81% | +4.06 pp | 93.67% | 64.21% |
| Terminator S.T | 92.59% | 89.33% | +3.26 pp | 93.10% | 65.51% |
| Exterminatus S.T | 89.11% | 92.14% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.11% | 92.32% | -2.20 pp | 92.66% | 4.12% |
| Whirlwind S.T | 91.18% | 92.02% | -0.84 pp | 93.47% | 47.73% |
| Infernus S.T | 91.76% | 90.98% | +0.77 pp | 92.93% | 61.42% |
| Demolitionist S.T | 91.68% | 91.13% | +0.56 pp | 93.49% | 47.12% |
| Servo-Skull | 91.12% | 91.59% | -0.47 pp | 91.20% | 21.73% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
