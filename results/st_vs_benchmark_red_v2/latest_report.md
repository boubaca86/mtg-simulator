# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 36,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.68% | +5.75 pp | 93.71% | 64.97% |
| Eradicator S.T | 93.21% | 88.35% | +4.87 pp | 95.06% | 58.20% |
| Drop Pod S.T | 93.11% | 88.42% | +4.68 pp | 93.42% | 57.73% |
| Pyroclast Squad S.T | 92.85% | 88.89% | +3.95 pp | 93.65% | 64.12% |
| Terminator S.T | 92.54% | 89.46% | +3.08 pp | 93.04% | 65.56% |
| Exterminatus S.T | 89.15% | 92.15% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.27% | -2.05 pp | 92.81% | 4.17% |
| Infernus S.T | 91.89% | 90.78% | +1.11 pp | 93.10% | 61.36% |
| Whirlwind S.T | 91.26% | 91.92% | -0.66 pp | 93.60% | 47.65% |
| Demolitionist S.T | 91.69% | 91.16% | +0.53 pp | 93.49% | 47.21% |
| Servo-Skull | 91.14% | 91.61% | -0.47 pp | 91.22% | 21.99% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
