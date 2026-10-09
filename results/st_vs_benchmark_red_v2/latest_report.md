# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 52,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.72% | +5.70 pp | 93.69% | 64.95% |
| Eradicator S.T | 93.21% | 88.38% | +4.83 pp | 95.09% | 58.08% |
| Drop Pod S.T | 93.01% | 88.64% | +4.38 pp | 93.34% | 57.52% |
| Pyroclast Squad S.T | 92.87% | 88.87% | +4.01 pp | 93.66% | 64.26% |
| Terminator S.T | 92.70% | 89.19% | +3.50 pp | 93.21% | 65.53% |
| Exterminatus S.T | 89.16% | 92.16% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.32% | -2.13 pp | 92.78% | 4.15% |
| Infernus S.T | 91.79% | 90.98% | +0.81 pp | 92.97% | 61.40% |
| Whirlwind S.T | 91.26% | 91.94% | -0.68 pp | 93.49% | 47.84% |
| Servo-Skull | 91.11% | 91.63% | -0.52 pp | 91.20% | 21.76% |
| Demolitionist S.T | 91.68% | 91.20% | +0.47 pp | 93.39% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
