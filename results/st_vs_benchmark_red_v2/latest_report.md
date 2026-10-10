# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 68,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.69% | +5.78 pp | 93.72% | 64.91% |
| Eradicator S.T | 93.19% | 88.48% | +4.70 pp | 95.08% | 58.16% |
| Drop Pod S.T | 93.03% | 88.67% | +4.37 pp | 93.35% | 57.62% |
| Pyroclast Squad S.T | 92.85% | 88.99% | +3.86 pp | 93.65% | 64.17% |
| Terminator S.T | 92.74% | 89.19% | +3.55 pp | 93.25% | 65.49% |
| Exterminatus S.T | 89.20% | 92.18% | -2.98 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.33% | -2.10 pp | 92.94% | 4.21% |
| Infernus S.T | 91.88% | 90.89% | +0.99 pp | 93.04% | 61.41% |
| Whirlwind S.T | 91.19% | 92.12% | -0.93 pp | 93.44% | 47.84% |
| Servo-Skull | 91.27% | 91.61% | -0.35 pp | 91.35% | 21.97% |
| Demolitionist S.T | 91.62% | 91.38% | +0.24 pp | 93.31% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
