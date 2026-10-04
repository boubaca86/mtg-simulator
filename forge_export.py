from __future__ import annotations

import argparse
import json
from pathlib import Path

FORGE_RELEASE = "2.0.15"
SET_CODE = "STF"

CARD_SCRIPTS = {
    "d/drop_pod_s_t.txt": """Name:Drop Pod S.T
ManaCost:2
Types:Artifact Creature Vehicle
PT:0/3
K:Defender
S:Mode$ ReduceCost | ValidCard$ Creature.Red | Type$ Spell | Activator$ You | Amount$ 1 | Description$ Red creature spells you cast cost {1} less to cast.
T:Mode$ AttackerBlockedByCreature | ValidCard$ Creature | ValidBlocker$ Card.Self | Execute$ TrigDamageAttacker | TriggerZones$ Battlefield | TriggerDescription$ Whenever CARDNAME blocks a creature, CARDNAME deals 1 damage to that creature.
SVar:TrigDamageAttacker:DB$ DealDamage | Defined$ TriggeredAttackerLKICopy | NumDmg$ 1
Oracle:Defender\\nRed creature spells you cast cost {1} less to cast.\\nWhenever Drop Pod S.T blocks a creature, Drop Pod S.T deals 1 damage to that creature.
""",
    "s/servitor.txt": """Name:Servitor
ManaCost:1 R
Types:Artifact Creature Construct
PT:2/1
K:Haste
T:Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Battlefield | Destination$ Graveyard | Execute$ TrigDelay | TriggerZones$ Battlefield | TriggerDescription$ When CARDNAME dies, return it to its owner's hand at the beginning of the next end step.
SVar:TrigDelay:DB$ DelayedTrigger | Mode$ Phase | Phase$ End of Turn | RememberObjects$ TriggeredNewCardLKICopy | Execute$ TrigReturn | SpellDescription$ Return that creature to its owner's hand at the beginning of the next end step.
SVar:TrigReturn:DB$ ChangeZone | Defined$ DelayTriggerRememberedLKI | Origin$ Graveyard | Destination$ Hand
Oracle:Haste\\nWhen Servitor dies, return it to its owner's hand at the beginning of the next end step.
""",
    "t/terminator_s_t.txt": """Name:Terminator S.T
ManaCost:1 R R
Types:Creature Astartes Warrior
PT:3/4
K:Trample
Oracle:Trample
""",
    "p/pyroclast_squad_s_t.txt": """Name:Pyroclast Squad S.T
ManaCost:1 R R
Types:Creature Astartes Warrior
PT:3/3
K:Trample
T:Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | OptionalDecider$ You | CombatDamage$ True | Execute$ TrigDestroy | TriggerZones$ Battlefield | TriggerDescription$ Whenever CARDNAME deals combat damage to a player, you may destroy target artifact that player controls.
SVar:TrigDestroy:DB$ Destroy | ValidTgts$ Artifact.ControlledBy TriggeredTarget | TgtPrompt$ Select target artifact damaged player controls
Oracle:Trample\\nWhenever Pyroclast Squad S.T deals combat damage to a player, you may destroy target artifact that player controls.
""",
}

EDITION = """[metadata]
Code=STF
Name=S.T Forge Prototype
Date=2026-10-04
Type=Custom

[cards]
1 U Drop Pod S.T
2 C Servitor
3 U Terminator S.T
4 U Pyroclast Squad S.T
"""

ST_SMOKE = {
    "Mountain": 24,
    "Drop Pod S.T": 4,
    "Servitor": 4,
    "Terminator S.T": 4,
    "Pyroclast Squad S.T": 4,
    "Shock": 4,
    "Lightning Strike": 4,
    "Goblin Piker": 4,
    "Goblin Chariot": 4,
    "Hill Giant": 4,
}

BENCHMARK_RED = {
    "Mountain": 24,
    "Raging Goblin": 4,
    "Goblin Piker": 4,
    "Goblin Chariot": 4,
    "Hill Giant": 4,
    "Lightning Elemental": 4,
    "Shock": 4,
    "Lightning Strike": 4,
    "Volcanic Hammer": 4,
    "Lava Axe": 4,
}

DEFERRED_FULL_ST_CARDS = [
    "Infernus S.T",
    "Demolitionist S.T",
    "Whirlwind S.T",
    "Eradicator S.T",
    "Exterminatus S.T",
    "Vindicator S.T",
    "Servo-Skull",
]


def deck_text(name: str, cards: dict[str, int]) -> str:
    total = sum(cards.values())
    if total != 60:
        raise ValueError(f"{name} must contain exactly 60 cards; found {total}")
    lines = [f"Name={name}", "Deck Type=constructed", "[main]"]
    lines.extend(f"{count} {card}" for card, count in cards.items())
    return "\n".join(lines) + "\n"


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def export(user_dir: Path) -> None:
    custom = user_dir / "custom"
    for relative, script in CARD_SCRIPTS.items():
        write_text(custom / "cards" / relative, script)
    write_text(custom / "editions" / "ST Forge Prototype.txt", EDITION)

    decks = user_dir / "decks" / "constructed"
    write_text(decks / "ST Forge Smoke.dck", deck_text("ST Forge Smoke", ST_SMOKE))
    write_text(decks / "Benchmark Red Forge.dck", deck_text("Benchmark Red Forge", BENCHMARK_RED))

    manifest = {
        "forge_release_target": FORGE_RELEASE,
        "set_code": SET_CODE,
        "implemented_custom_cards": [
            "Drop Pod S.T",
            "Servitor",
            "Terminator S.T",
            "Pyroclast Squad S.T",
        ],
        "deferred_full_st_cards": DEFERRED_FULL_ST_CARDS,
        "smoke_deck_cards": sum(ST_SMOKE.values()),
        "benchmark_deck_cards": sum(BENCHMARK_RED.values()),
        "purpose": "Stage 1 Forge rules-engine integration smoke test",
    }
    write_text(user_dir / "forge_st_manifest.json", json.dumps(manifest, indent=2) + "\n")

    print(f"Forge S.T prototype exported to {user_dir}")
    print("Implemented custom cards: 4")
    print("S.T smoke deck: 60 cards")
    print("Benchmark Red Forge deck: 60 cards")
    print("Deferred full-deck custom cards:")
    for name in DEFERRED_FULL_ST_CARDS:
        print(f"  - {name}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Export S.T custom cards/decks into a Forge user directory")
    parser.add_argument("--user-dir", default=str(Path.home() / ".forge"))
    args = parser.parse_args()
    export(Path(args.user_dir).expanduser().resolve())
