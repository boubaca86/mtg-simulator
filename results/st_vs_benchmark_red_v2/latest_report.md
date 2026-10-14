# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 116,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.68% | +5.88 pp | 93.83% | 64.95% |
| Eradicator S.T | 93.18% | 88.68% | +4.50 pp | 95.08% | 57.83% |
| Drop Pod S.T | 93.04% | 88.82% | +4.21 pp | 93.34% | 57.53% |
| Terminator S.T | 92.86% | 89.08% | +3.78 pp | 93.35% | 65.55% |
| Pyroclast Squad S.T | 92.86% | 89.09% | +3.76 pp | 93.65% | 64.44% |
| Exterminatus S.T | 89.07% | 92.28% | -3.22 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.35% | -2.02 pp | 92.70% | 4.24% |
| Infernus S.T | 92.00% | 90.80% | +1.20 pp | 93.14% | 61.52% |
| Whirlwind S.T | 91.18% | 92.27% | -1.09 pp | 93.43% | 47.82% |
| Servo-Skull | 91.22% | 91.69% | -0.47 pp | 91.29% | 22.01% |
| Demolitionist S.T | 91.64% | 91.48% | +0.16 pp | 93.29% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
