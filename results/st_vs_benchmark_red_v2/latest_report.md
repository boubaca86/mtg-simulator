# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 192,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.67% | +5.81 pp | 93.76% | 64.91% |
| Eradicator S.T | 93.09% | 88.67% | +4.42 pp | 95.01% | 57.85% |
| Drop Pod S.T | 92.96% | 88.82% | +4.14 pp | 93.22% | 57.44% |
| Pyroclast Squad S.T | 92.79% | 89.06% | +3.73 pp | 93.59% | 64.37% |
| Terminator S.T | 92.79% | 89.08% | +3.72 pp | 93.29% | 65.45% |
| Exterminatus S.T | 88.85% | 92.27% | -3.42 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.36% | -2.18 pp | 92.55% | 4.22% |
| Infernus S.T | 91.95% | 90.74% | +1.22 pp | 93.09% | 61.44% |
| Whirlwind S.T | 91.18% | 92.15% | -0.97 pp | 93.52% | 47.84% |
| Servo-Skull | 91.09% | 91.66% | -0.57 pp | 91.12% | 22.02% |
| Demolitionist S.T | 91.62% | 91.36% | +0.26 pp | 93.23% | 47.17% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
