# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 69,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.69% | +5.77 pp | 93.70% | 64.94% |
| Eradicator S.T | 93.18% | 88.46% | +4.72 pp | 95.08% | 58.17% |
| Drop Pod S.T | 93.02% | 88.67% | +4.36 pp | 93.33% | 57.56% |
| Pyroclast Squad S.T | 92.85% | 88.96% | +3.89 pp | 93.67% | 64.18% |
| Terminator S.T | 92.73% | 89.16% | +3.57 pp | 93.25% | 65.49% |
| Exterminatus S.T | 89.20% | 92.17% | -2.96 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.30% | -2.05 pp | 92.95% | 4.21% |
| Infernus S.T | 91.87% | 90.87% | +1.00 pp | 93.03% | 61.44% |
| Whirlwind S.T | 91.18% | 92.12% | -0.94 pp | 93.44% | 47.85% |
| Servo-Skull | 91.27% | 91.60% | -0.33 pp | 91.35% | 21.97% |
| Demolitionist S.T | 91.61% | 91.36% | +0.24 pp | 93.31% | 47.19% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
