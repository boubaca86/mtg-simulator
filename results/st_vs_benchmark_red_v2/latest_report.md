# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 109,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.59% | +5.96 pp | 93.81% | 64.90% |
| Eradicator S.T | 93.15% | 88.62% | +4.53 pp | 95.06% | 57.89% |
| Drop Pod S.T | 92.99% | 88.79% | +4.21 pp | 93.30% | 57.49% |
| Terminator S.T | 92.83% | 89.03% | +3.80 pp | 93.33% | 65.54% |
| Pyroclast Squad S.T | 92.83% | 89.03% | +3.80 pp | 93.64% | 64.42% |
| Exterminatus S.T | 89.05% | 92.23% | -3.18 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.30% | -1.99 pp | 92.67% | 4.24% |
| Infernus S.T | 91.95% | 90.78% | +1.17 pp | 93.10% | 61.51% |
| Whirlwind S.T | 91.11% | 92.29% | -1.17 pp | 93.37% | 47.85% |
| Servo-Skull | 91.20% | 91.64% | -0.45 pp | 91.26% | 22.01% |
| Demolitionist S.T | 91.58% | 91.47% | +0.11 pp | 93.25% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
