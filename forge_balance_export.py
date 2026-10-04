from __future__ import annotations

import argparse
from pathlib import Path

from forge_export import ST_FULL, deck_text, export as export_full_set, write_text

BALANCE_EDITION = """[metadata]
Code=STB
Name=S.T Forge Balance Lab
Date=2026-10-04
Type=Custom

[cards]
1 U Drop Pod S.T No Discount
2 U Drop Pod S.T No Ping
3 U Drop Pod S.T Blank
"""

VARIANT_SCRIPTS = {
    "d/drop_pod_s_t_no_discount.txt": """Name:Drop Pod S.T No Discount
ManaCost:2
Types:Artifact Creature Vehicle
PT:0/3
K:Defender
T:Mode$ AttackerBlockedByCreature | ValidCard$ Creature | ValidBlocker$ Card.Self | Execute$ TrigDamageAttacker | TriggerZones$ Battlefield | TriggerDescription$ Whenever CARDNAME blocks a creature, CARDNAME deals 1 damage to that creature.
SVar:TrigDamageAttacker:DB$ DealDamage | Defined$ TriggeredAttackerLKICopy | NumDmg$ 1
Oracle:Defender\\nWhenever Drop Pod S.T No Discount blocks a creature, it deals 1 damage to that creature.
""",
    "d/drop_pod_s_t_no_ping.txt": """Name:Drop Pod S.T No Ping
ManaCost:2
Types:Artifact Creature Vehicle
PT:0/3
K:Defender
S:Mode$ ReduceCost | ValidCard$ Creature.Red | Type$ Spell | Activator$ You | Amount$ 1 | Description$ Red creature spells you cast cost {1} less to cast.
Oracle:Defender\\nRed creature spells you cast cost {1} less to cast.
""",
    "d/drop_pod_s_t_blank.txt": """Name:Drop Pod S.T Blank
ManaCost:2
Types:Artifact Creature Vehicle
PT:0/3
K:Defender
Oracle:Defender
""",
}

ARMS = {
    "baseline": ("ST Balance Baseline", "Drop Pod S.T"),
    "no_discount": ("ST Balance Drop Pod No Discount", "Drop Pod S.T No Discount"),
    "no_ping": ("ST Balance Drop Pod No Ping", "Drop Pod S.T No Ping"),
    "blank": ("ST Balance Drop Pod Blank", "Drop Pod S.T Blank"),
}


def make_variant_deck(replacement_name: str) -> dict[str, int]:
    cards = dict(ST_FULL)
    copies = cards.pop("Drop Pod S.T")
    cards[replacement_name] = copies
    return cards


def export_balance_lab(user_dir: Path) -> None:
    # First export the already-validated full S.T set and benchmark deck.
    export_full_set(user_dir)

    custom = user_dir / "custom"
    for relative, script in VARIANT_SCRIPTS.items():
        write_text(custom / "cards" / relative, script)
    write_text(custom / "editions" / "S.T Forge Balance Lab.txt", BALANCE_EDITION)

    decks = user_dir / "decks" / "constructed"
    for key, (deck_name, replacement_name) in ARMS.items():
        cards = ST_FULL if key == "baseline" else make_variant_deck(replacement_name)
        write_text(decks / f"{deck_name}.dck", deck_text(deck_name, cards))

    print("Forge balance lab exported.")
    print("Arms:")
    for key, (deck_name, replacement_name) in ARMS.items():
        print(f"  {key}: {deck_name} ({replacement_name})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Export Drop Pod S.T Forge balance-lab variants")
    parser.add_argument("--user-dir", default=str(Path.home() / ".forge"))
    args = parser.parse_args()
    export_balance_lab(Path(args.user_dir).expanduser().resolve())
