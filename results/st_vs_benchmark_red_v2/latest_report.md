# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 160,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.76% | +5.73 pp | 93.78% | 65.00% |
| Eradicator S.T | 93.14% | 88.72% | +4.42 pp | 95.04% | 57.84% |
| Drop Pod S.T | 93.00% | 88.85% | +4.15 pp | 93.26% | 57.48% |
| Terminator S.T | 92.85% | 89.08% | +3.78 pp | 93.35% | 65.50% |
| Pyroclast Squad S.T | 92.84% | 89.10% | +3.74 pp | 93.63% | 64.41% |
| Exterminatus S.T | 88.95% | 92.30% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.38% | -2.12 pp | 92.86% | 4.21% |
| Infernus S.T | 92.01% | 90.75% | +1.25 pp | 93.14% | 61.52% |
| Whirlwind S.T | 91.17% | 92.27% | -1.11 pp | 93.50% | 47.79% |
| Servo-Skull | 91.19% | 91.69% | -0.50 pp | 91.24% | 22.04% |
| Demolitionist S.T | 91.66% | 91.40% | +0.26 pp | 93.32% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
