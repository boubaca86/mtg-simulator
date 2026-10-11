# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 79,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.68% | +5.86 pp | 93.79% | 64.92% |
| Eradicator S.T | 93.16% | 88.65% | +4.51 pp | 95.09% | 58.07% |
| Drop Pod S.T | 93.07% | 88.71% | +4.36 pp | 93.37% | 57.62% |
| Pyroclast Squad S.T | 92.85% | 89.09% | +3.76 pp | 93.67% | 64.19% |
| Terminator S.T | 92.79% | 89.18% | +3.62 pp | 93.30% | 65.65% |
| Exterminatus S.T | 89.25% | 92.22% | -2.97 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.39% | -2.16 pp | 92.84% | 4.21% |
| Whirlwind S.T | 91.17% | 92.27% | -1.10 pp | 93.44% | 47.91% |
| Infernus S.T | 91.93% | 90.91% | +1.02 pp | 93.07% | 61.34% |
| Servo-Skull | 91.26% | 91.66% | -0.40 pp | 91.34% | 21.90% |
| Demolitionist S.T | 91.66% | 91.40% | +0.26 pp | 93.36% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
