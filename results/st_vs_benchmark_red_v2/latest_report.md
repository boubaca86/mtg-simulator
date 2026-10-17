# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 154,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.75% | +5.75 pp | 93.79% | 64.96% |
| Eradicator S.T | 93.12% | 88.74% | +4.37 pp | 95.02% | 57.85% |
| Drop Pod S.T | 93.01% | 88.84% | +4.17 pp | 93.26% | 57.47% |
| Terminator S.T | 92.85% | 89.08% | +3.77 pp | 93.35% | 65.50% |
| Pyroclast Squad S.T | 92.83% | 89.10% | +3.73 pp | 93.64% | 64.41% |
| Exterminatus S.T | 88.96% | 92.29% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.36% | -2.09 pp | 92.82% | 4.23% |
| Infernus S.T | 92.00% | 90.76% | +1.24 pp | 93.14% | 61.54% |
| Whirlwind S.T | 91.17% | 92.27% | -1.11 pp | 93.49% | 47.82% |
| Servo-Skull | 91.21% | 91.68% | -0.47 pp | 91.26% | 22.03% |
| Demolitionist S.T | 91.67% | 91.40% | +0.27 pp | 93.31% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
