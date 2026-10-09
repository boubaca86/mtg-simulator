# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 63,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.39% | 87.74% | +5.65 pp | 93.64% | 64.91% |
| Eradicator S.T | 93.16% | 88.42% | +4.74 pp | 95.06% | 58.16% |
| Drop Pod S.T | 92.98% | 88.66% | +4.33 pp | 93.30% | 57.55% |
| Pyroclast Squad S.T | 92.82% | 88.93% | +3.90 pp | 93.61% | 64.19% |
| Terminator S.T | 92.70% | 89.15% | +3.55 pp | 93.20% | 65.49% |
| Exterminatus S.T | 89.16% | 92.14% | -2.98 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.27% | -2.04 pp | 92.93% | 4.20% |
| Infernus S.T | 91.83% | 90.86% | +0.97 pp | 92.98% | 61.37% |
| Whirlwind S.T | 91.18% | 92.04% | -0.86 pp | 93.43% | 47.84% |
| Servo-Skull | 91.25% | 91.56% | -0.31 pp | 91.33% | 21.98% |
| Demolitionist S.T | 91.59% | 91.32% | +0.27 pp | 93.28% | 47.30% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
