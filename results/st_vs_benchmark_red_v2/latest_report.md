# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 116,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.67% | +5.88 pp | 93.83% | 64.95% |
| Eradicator S.T | 93.18% | 88.66% | +4.51 pp | 95.07% | 57.83% |
| Drop Pod S.T | 93.03% | 88.81% | +4.22 pp | 93.33% | 57.53% |
| Terminator S.T | 92.87% | 89.06% | +3.81 pp | 93.35% | 65.54% |
| Pyroclast Squad S.T | 92.85% | 89.08% | +3.77 pp | 93.65% | 64.44% |
| Exterminatus S.T | 89.06% | 92.27% | -3.21 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.32% | 92.34% | -2.02 pp | 92.70% | 4.24% |
| Infernus S.T | 92.00% | 90.78% | +1.22 pp | 93.13% | 61.53% |
| Whirlwind S.T | 91.17% | 92.27% | -1.10 pp | 93.41% | 47.82% |
| Servo-Skull | 91.21% | 91.68% | -0.48 pp | 91.27% | 22.02% |
| Demolitionist S.T | 91.63% | 91.48% | +0.15 pp | 93.28% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
