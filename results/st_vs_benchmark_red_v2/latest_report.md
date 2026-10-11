# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 84,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.67% | +5.86 pp | 93.78% | 64.81% |
| Eradicator S.T | 93.14% | 88.64% | +4.50 pp | 95.09% | 58.08% |
| Drop Pod S.T | 93.01% | 88.76% | +4.26 pp | 93.33% | 57.60% |
| Pyroclast Squad S.T | 92.84% | 89.06% | +3.78 pp | 93.65% | 64.23% |
| Terminator S.T | 92.78% | 89.16% | +3.62 pp | 93.29% | 65.58% |
| Exterminatus S.T | 89.17% | 92.21% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.36% | -2.14 pp | 92.74% | 4.24% |
| Whirlwind S.T | 91.15% | 92.25% | -1.10 pp | 93.44% | 47.92% |
| Infernus S.T | 91.91% | 90.88% | +1.03 pp | 93.06% | 61.35% |
| Servo-Skull | 91.27% | 91.63% | -0.36 pp | 91.35% | 21.97% |
| Demolitionist S.T | 91.64% | 91.40% | +0.24 pp | 93.33% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
