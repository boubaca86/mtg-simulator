# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 78,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.69% | +5.84 pp | 93.77% | 64.95% |
| Eradicator S.T | 93.17% | 88.62% | +4.55 pp | 95.09% | 58.07% |
| Drop Pod S.T | 93.06% | 88.71% | +4.35 pp | 93.36% | 57.61% |
| Pyroclast Squad S.T | 92.86% | 89.06% | +3.80 pp | 93.68% | 64.18% |
| Terminator S.T | 92.80% | 89.15% | +3.65 pp | 93.31% | 65.65% |
| Exterminatus S.T | 89.25% | 92.21% | -2.96 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.39% | -2.17 pp | 92.76% | 4.21% |
| Whirlwind S.T | 91.17% | 92.26% | -1.10 pp | 93.44% | 47.89% |
| Infernus S.T | 91.92% | 90.91% | +1.00 pp | 93.06% | 61.34% |
| Servo-Skull | 91.27% | 91.65% | -0.38 pp | 91.34% | 21.92% |
| Demolitionist S.T | 91.67% | 91.38% | +0.29 pp | 93.34% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
