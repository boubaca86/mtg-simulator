# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 20,500
**S.T win rate:** 91.64%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.73% | 87.46% | +6.27 pp | 93.98% | 65.23% |
| Eradicator S.T | 93.36% | 88.49% | +4.88 pp | 95.07% | 58.36% |
| Drop Pod S.T | 93.04% | 88.97% | +4.07 pp | 93.27% | 57.66% |
| Pyroclast Squad S.T | 92.94% | 89.18% | +3.76 pp | 93.69% | 63.65% |
| Terminator S.T | 92.68% | 89.62% | +3.06 pp | 93.10% | 65.43% |
| Exterminatus S.T | 89.43% | 92.24% | -2.81 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.48% | -2.23 pp | 92.00% | 4.15% |
| Infernus S.T | 92.02% | 90.91% | +1.11 pp | 93.21% | 61.53% |
| Servo-Skull | 90.94% | 91.85% | -0.91 pp | 91.00% | 22.16% |
| Demolitionist S.T | 91.90% | 91.16% | +0.74 pp | 93.55% | 47.44% |
| Whirlwind S.T | 91.39% | 92.08% | -0.70 pp | 93.52% | 48.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
