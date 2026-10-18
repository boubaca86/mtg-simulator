# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 161,500
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.76% | +5.73 pp | 93.78% | 65.00% |
| Eradicator S.T | 93.13% | 88.73% | +4.40 pp | 95.04% | 57.84% |
| Drop Pod S.T | 93.01% | 88.85% | +4.16 pp | 93.27% | 57.49% |
| Terminator S.T | 92.85% | 89.09% | +3.76 pp | 93.34% | 65.50% |
| Pyroclast Squad S.T | 92.84% | 89.10% | +3.74 pp | 93.64% | 64.41% |
| Exterminatus S.T | 88.98% | 92.29% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.37% | -2.11 pp | 92.85% | 4.21% |
| Infernus S.T | 92.01% | 90.76% | +1.25 pp | 93.15% | 61.52% |
| Whirlwind S.T | 91.17% | 92.28% | -1.11 pp | 93.49% | 47.78% |
| Servo-Skull | 91.16% | 91.70% | -0.53 pp | 91.22% | 22.03% |
| Demolitionist S.T | 91.66% | 91.42% | +0.25 pp | 93.32% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
