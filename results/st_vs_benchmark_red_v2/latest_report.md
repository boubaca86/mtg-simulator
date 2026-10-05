# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 6,000
**S.T win rate:** 92.33%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.15% | 88.99% | +5.16 pp | 95.74% | 58.35% |
| Servitor | 93.93% | 89.10% | +4.83 pp | 94.17% | 65.50% |
| Exterminatus S.T | 89.11% | 93.21% | -4.10 pp | 0.00% | 0.00% |
| Pyroclast Squad S.T | 93.66% | 89.66% | +4.00 pp | 94.46% | 64.67% |
| Drop Pod S.T | 93.38% | 90.36% | +3.01 pp | 93.58% | 57.15% |
| Terminator S.T | 93.21% | 90.68% | +2.53 pp | 93.56% | 65.18% |
| Servo-Skull | 90.78% | 92.80% | -2.02 pp | 90.82% | 22.33% |
| Vindicator S.T | 91.27% | 92.95% | -1.68 pp | 92.80% | 4.40% |
| Infernus S.T | 92.81% | 91.44% | +1.37 pp | 93.68% | 61.42% |
| Demolitionist S.T | 92.59% | 91.87% | +0.72 pp | 94.23% | 47.37% |
| Whirlwind S.T | 92.18% | 92.59% | -0.40 pp | 93.84% | 47.92% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
