"""Stage 6 reference model: aggregate complete actions, never per-world optima.

This is deliberately independent of Forge internals.  It is an executable
regression oracle for the imperfect-information invariant that the Java patch
must preserve before its results can be used as expert labels.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, Mapping, Sequence


@dataclass(frozen=True, order=True)
class CompleteAction:
    ability: str
    modes: tuple[str, ...] = ()
    x: int | None = None
    targets: tuple[str, ...] = ()
    choices: tuple[str, ...] = ()


def aggregate_complete_actions(
    worlds: Sequence[Mapping[CompleteAction, float]], *, downside_weight: float = 0.15
) -> tuple[CompleteAction, dict[CompleteAction, float]]:
    """Return one action chosen from legal-information-consistent candidates.

    Only actions present in every sampled world are eligible.  This prevents
    hidden-world illegality from becoming an information channel.  A candidate
    is scored by mean value minus a documented downside-risk penalty measured
    as mean-minus-worst-case.  Tie breaking is deterministic.
    """
    if not worlds:
        raise ValueError("at least one hidden-world sample is required")
    common = set(worlds[0])
    for world in worlds[1:]:
        common.intersection_update(world)
    if not common:
        raise ValueError("no complete action is legal across the information set")

    scores: dict[CompleteAction, float] = {}
    for action in sorted(common):
        values = [world[action] for world in worlds]
        mean = sum(values) / len(values)
        downside = mean - min(values)
        scores[action] = mean - downside_weight * downside

    winner = max(sorted(common), key=lambda action: scores[action])
    return winner, scores


def strategy_fusion_upper_bound(worlds: Sequence[Mapping[CompleteAction, float]]) -> float:
    """Diagnostic only: illegal per-world optimization used to expose leakage."""
    return sum(max(world.values()) for world in worlds) / len(worlds)
