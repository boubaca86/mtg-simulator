# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 50,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.76% | +5.67 pp | 93.70% | 65.00% |
| Eradicator S.T | 93.22% | 88.41% | +4.81 pp | 95.09% | 58.01% |
| Drop Pod S.T | 93.06% | 88.59% | +4.48 pp | 93.38% | 57.53% |
| Pyroclast Squad S.T | 92.85% | 88.95% | +3.90 pp | 93.64% | 64.27% |
| Terminator S.T | 92.70% | 89.22% | +3.48 pp | 93.21% | 65.58% |
| Exterminatus S.T | 89.15% | 92.18% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.34% | -2.15 pp | 92.81% | 4.15% |
| Infernus S.T | 91.84% | 90.94% | +0.90 pp | 93.01% | 61.45% |
| Whirlwind S.T | 91.25% | 92.00% | -0.76 pp | 93.50% | 47.80% |
| Servo-Skull | 91.09% | 91.65% | -0.56 pp | 91.17% | 21.80% |
| Demolitionist S.T | 91.69% | 91.22% | +0.47 pp | 93.40% | 47.31% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
