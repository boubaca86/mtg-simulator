# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 54,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.74% | +5.68 pp | 93.69% | 64.95% |
| Eradicator S.T | 93.20% | 88.43% | +4.77 pp | 95.09% | 58.15% |
| Drop Pod S.T | 93.01% | 88.68% | +4.32 pp | 93.32% | 57.51% |
| Pyroclast Squad S.T | 92.88% | 88.88% | +3.99 pp | 93.67% | 64.26% |
| Terminator S.T | 92.70% | 89.22% | +3.48 pp | 93.21% | 65.46% |
| Exterminatus S.T | 89.25% | 92.15% | -2.90 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.31% | -2.08 pp | 92.93% | 4.17% |
| Infernus S.T | 91.78% | 91.03% | +0.75 pp | 92.95% | 61.35% |
| Whirlwind S.T | 91.25% | 91.99% | -0.74 pp | 93.47% | 47.87% |
| Servo-Skull | 91.11% | 91.64% | -0.53 pp | 91.20% | 21.83% |
| Demolitionist S.T | 91.66% | 91.26% | +0.40 pp | 93.35% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
