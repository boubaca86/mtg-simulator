# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 53,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.74% | +5.66 pp | 93.66% | 64.92% |
| Eradicator S.T | 93.18% | 88.40% | +4.78 pp | 95.08% | 58.16% |
| Drop Pod S.T | 93.00% | 88.65% | +4.35 pp | 93.31% | 57.53% |
| Pyroclast Squad S.T | 92.86% | 88.87% | +3.99 pp | 93.65% | 64.26% |
| Terminator S.T | 92.69% | 89.19% | +3.49 pp | 93.20% | 65.46% |
| Exterminatus S.T | 89.21% | 92.14% | -2.92 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.29% | -2.09 pp | 92.95% | 4.16% |
| Infernus S.T | 91.77% | 91.00% | +0.76 pp | 92.94% | 61.36% |
| Whirlwind S.T | 91.23% | 91.97% | -0.73 pp | 93.46% | 47.87% |
| Servo-Skull | 91.10% | 91.62% | -0.52 pp | 91.19% | 21.82% |
| Demolitionist S.T | 91.65% | 91.23% | +0.42 pp | 93.36% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
