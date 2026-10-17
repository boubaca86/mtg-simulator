# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 154,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.74% | +5.77 pp | 93.80% | 64.97% |
| Eradicator S.T | 93.12% | 88.75% | +4.37 pp | 95.03% | 57.85% |
| Drop Pod S.T | 93.01% | 88.85% | +4.16 pp | 93.27% | 57.47% |
| Terminator S.T | 92.85% | 89.09% | +3.76 pp | 93.35% | 65.50% |
| Pyroclast Squad S.T | 92.84% | 89.11% | +3.73 pp | 93.64% | 64.42% |
| Exterminatus S.T | 88.96% | 92.30% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.28% | 92.37% | -2.09 pp | 92.81% | 4.23% |
| Infernus S.T | 92.00% | 90.77% | +1.23 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.17% | 92.28% | -1.10 pp | 93.50% | 47.82% |
| Servo-Skull | 91.21% | 91.68% | -0.48 pp | 91.26% | 22.03% |
| Demolitionist S.T | 91.67% | 91.40% | +0.28 pp | 93.31% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
