# Forge Expert AI — Stage 4 Information Boundary

Forge source tag: forge-2.0.15  
Games per arm: 10  
Seed: 20261004

Both players use Forge `USE_FULL_SIMULATION`. The only difference is whether search is allowed to inherit the real hidden hand/library assignment from the copied game.

| Arm | Hidden-state treatment | S.T wins | Benchmark wins | Draws | Timeouts | S.T win rate |
|---|---|---:|---:|---:|---:|---:|
| full_raw | raw copied hidden state | 7 | 3 | 0 | 0 | 70.0% |
| full_infoset | information-set determinization | 7 | 3 | 0 | 0 | 70.0% |

## What Stage 4 changes

With information-set determinization enabled, the search copy preserves public information, hand/library counts, and cards the acting player is allowed to look at. Unknown opponent hand cards and unknown future library cards are pooled and resampled using a local deterministic RNG derived from public/count information only. The real game's RNG stream is not consumed by this resampling.

This prevents a search line from relying on the opponent's actual unknown hand identity or the actual future library order. It is still not the final expert planner because one sampled hidden world can be misleading. Stage 5 will evaluate candidate actions across multiple plausible hidden worlds and aggregate their values.
