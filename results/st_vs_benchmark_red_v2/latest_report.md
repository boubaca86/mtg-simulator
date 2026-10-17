# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 157,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.74% | +5.76 pp | 93.78% | 64.97% |
| Eradicator S.T | 93.12% | 88.73% | +4.39 pp | 95.02% | 57.86% |
| Drop Pod S.T | 93.00% | 88.84% | +4.15 pp | 93.25% | 57.47% |
| Terminator S.T | 92.85% | 89.07% | +3.77 pp | 93.34% | 65.48% |
| Pyroclast Squad S.T | 92.83% | 89.09% | +3.74 pp | 93.63% | 64.42% |
| Exterminatus S.T | 88.95% | 92.29% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.36% | -2.11 pp | 92.81% | 4.22% |
| Infernus S.T | 92.00% | 90.75% | +1.25 pp | 93.13% | 61.55% |
| Whirlwind S.T | 91.16% | 92.28% | -1.12 pp | 93.48% | 47.81% |
| Servo-Skull | 91.17% | 91.68% | -0.51 pp | 91.21% | 22.03% |
| Demolitionist S.T | 91.67% | 91.37% | +0.30 pp | 93.31% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
