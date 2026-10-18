# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 163,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.75% | +5.75 pp | 93.79% | 65.00% |
| Eradicator S.T | 93.15% | 88.71% | +4.44 pp | 95.05% | 57.83% |
| Drop Pod S.T | 93.01% | 88.86% | +4.15 pp | 93.26% | 57.49% |
| Terminator S.T | 92.85% | 89.08% | +3.77 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.84% | 89.10% | +3.75 pp | 93.63% | 64.42% |
| Exterminatus S.T | 88.99% | 92.29% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.37% | -2.11 pp | 92.78% | 4.21% |
| Infernus S.T | 92.01% | 90.76% | +1.24 pp | 93.14% | 61.49% |
| Whirlwind S.T | 91.17% | 92.28% | -1.10 pp | 93.49% | 47.77% |
| Servo-Skull | 91.18% | 91.69% | -0.51 pp | 91.23% | 22.04% |
| Demolitionist S.T | 91.66% | 91.42% | +0.24 pp | 93.31% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
