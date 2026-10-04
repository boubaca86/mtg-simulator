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
    "i/infernus_s_t.txt": """Name:Infernus S.T
ManaCost:1 R R
Types:Creature Astartes Warrior
PT:3/2
K:Trample
T:Mode$ AttackerUnblocked | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigDestroy | TriggerDescription$ Whenever CARDNAME attacks a player and isn't blocked, you may pay {R}. If you do, CARDNAME assigns no combat damage this combat and you destroy target land that player controls that can't produce {R}.
SVar:TrigDestroy:AB$ Destroy | Cost$ R | ValidTgts$ Land.ControlledBy TriggeredDefendingPlayer+!canProduceManaColor Red | TgtPrompt$ Select target land defending player controls that can't produce red mana | SubAbility$ DBNoCombatDamage
SVar:DBNoCombatDamage:DB$ Effect | StaticAbilities$ SNoCombatDamage | Duration$ UntilHostLeavesPlayOrEOT
SVar:SNoCombatDamage:Mode$ AssignNoCombatDamage | ValidCard$ Card.EffectSource | Description$ EFFECTSOURCE assigns no combat damage this turn.
Oracle:Trample\\nWhenever Infernus S.T attacks a player and isn't blocked, you may pay {R}. If you do, Infernus S.T assigns no combat damage this combat and you destroy target land that player controls that can't produce {R}.
""",
    "d/demolitionist_s_t.txt": """Name:Demolitionist S.T
ManaCost:3 R
Types:Creature Astartes Warrior
PT:3/2
T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigDestroy | TriggerDescription$ When CARDNAME dies, destroy target land an opponent controls that can't produce {R}.
SVar:TrigDestroy:DB$ Destroy | ValidTgts$ Land.OppCtrl+!canProduceManaColor Red | TgtPrompt$ Select target land an opponent controls that can't produce red mana
Oracle:When Demolitionist S.T dies, destroy target land an opponent controls that can't produce {R}.
""",
    "w/whirlwind_s_t.txt": """Name:Whirlwind S.T
ManaCost:3 R R
Types:Artifact Vehicle
A:AB$ DealDamage | Cost$ T | ValidTgts$ Creature.OppCtrl | TgtPrompt$ Select target creature an opponent controls | NumDmg$ 3 | SubAbility$ DBCantBlock | SpellDescription$ CARDNAME deals 3 damage to target creature an opponent controls. That creature can't block this turn.
SVar:DBCantBlock:DB$ Pump | Defined$ Targeted | IsCurse$ True | KW$ HIDDEN CARDNAME can't block.
A:AB$ Destroy | Cost$ T tapXType<1/Artifact.Other+sameName/another untapped artifact you control named Whirlwind S.T> | ValidTgts$ Land.OppCtrl+!canProduceManaColor Red | TgtPrompt$ Select target land an opponent controls that can't produce red mana | SorcerySpeed$ True | ActivationLimit$ 1 | SpellDescription$ Coordinated Bombardment — Tap CARDNAME and another untapped artifact you control named Whirlwind S.T: Destroy target land an opponent controls that can't produce {R}. Activate only as a sorcery and only once each turn.
Oracle:{T}: Whirlwind S.T deals 3 damage to target creature an opponent controls. That creature can't block this turn.\\nCoordinated Bombardment — {T}, Tap another untapped artifact you control named Whirlwind S.T: Destroy target land an opponent controls that can't produce {R}. Activate only as a sorcery and only once each turn.
""",
    "e/eradicator_s_t.txt": """Name:Eradicator S.T
ManaCost:3 R R
Types:Creature Astartes Warrior
PT:6/5
K:Menace
T:Mode$ AttackerUnblocked | ValidCard$ Card.Self | TriggerZones$ Battlefield | OptionalDecider$ You | Execute$ TrigDestroy | TriggerDescription$ Whenever CARDNAME attacks and isn't blocked, you may have it assign no combat damage this combat. If you do, destroy target land that player controls that can't produce {R} or target nonred artifact that player controls.
SVar:TrigDestroy:DB$ Destroy | ValidTgts$ Land.ControlledBy TriggeredDefendingPlayer+!canProduceManaColor Red,Artifact.nonRed+ControlledBy TriggeredDefendingPlayer | TgtPrompt$ Select target eligible land or nonred artifact defending player controls | SubAbility$ DBNoCombatDamage
SVar:DBNoCombatDamage:DB$ Effect | StaticAbilities$ SNoCombatDamage | Duration$ UntilHostLeavesPlayOrEOT
SVar:SNoCombatDamage:Mode$ AssignNoCombatDamage | ValidCard$ Card.EffectSource | Description$ EFFECTSOURCE assigns no combat damage this turn.
Oracle:Menace\\nWhenever Eradicator S.T attacks and isn't blocked, you may have it assign no combat damage this combat. If you do, destroy target land that player controls that can't produce {R} or target nonred artifact that player controls.
""",
    "e/exterminatus_s_t.txt": """Name:Exterminatus S.T
ManaCost:3 R R
Types:Sorcery
S:Mode$ CantBeCast | ValidCard$ Card.Self | EffectZone$ All | CheckSVar$ Life | SVarCompare$ GE6 | Description$ Cast this spell only if you have 5 or less life.
SVar:Life:Count$YourLifeTotal
A:SP$ ChooseCard | ValidTgts$ Opponent | Choices$ Land.TargetedPlayerCtrl+!canProduceManaColor Red | Amount$ X | Mandatory$ True | RememberChosen$ True | ChoiceTitle$ Choose half the eligible lands, rounded up | SubAbility$ DBDestroy | SpellDescription$ Destroy half the lands target opponent controls that can't produce {R}, rounded up.
SVar:X:Count$Valid Land.TargetedPlayerCtrl+!canProduceManaColor Red/HalfUp
SVar:DBDestroy:DB$ DestroyAll | ValidCards$ Land.IsRemembered | SubAbility$ DBCleanup
SVar:DBCleanup:DB$ Cleanup | ClearRemembered$ True | ClearChosenCard$ True
Oracle:Cast this spell only if you have 5 or less life.\\nDestroy half the lands target opponent controls that can't produce {R}, rounded up.
""",
    "v/vindicator_s_t.txt": """Name:Vindicator S.T
ManaCost:R R R R R R R R
Types:Artifact Creature Vehicle
PT:8/12
K:Trample
K:Ward:3
T:Mode$ Attacks | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigBlast | TriggerDescription$ Whenever CARDNAME attacks or blocks, it deals 4 damage to target creature or artifact an opponent controls. Then you may pay {X}. Spend only red mana on X. If you do, it deals X additional damage to that permanent. If X is 4 or more, destroy target land that opponent controls that can't produce {R}.
T:Mode$ Blocks | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigBlast | Secondary$ True
SVar:TrigBlast:AB$ DealDamage | Cost$ X | XColor$ Red | ValidTgts$ Creature.OppCtrl,Artifact.OppCtrl | TgtPrompt$ Select target creature or artifact an opponent controls | NumDmg$ Y | SubAbility$ DBBranch
SVar:X:Count$xPaid
SVar:Y:Count$xPaid/Plus.4
SVar:DBBranch:DB$ Branch | BranchConditionSVar$ X | BranchConditionSVarCompare$ GE4 | TrueSubAbility$ DBDestroyLand
SVar:DBDestroyLand:DB$ Destroy | ValidTgts$ Land.OppCtrl+!canProduceManaColor Red | TgtPrompt$ Select target land an opponent controls that can't produce red mana
Oracle:Trample, ward {3}\\nWhenever Vindicator S.T attacks or blocks, it deals 4 damage to target creature or artifact an opponent controls. Then you may pay {X}. Spend only red mana on X. If you do, Vindicator S.T deals X additional damage to that permanent. If X is 4 or more, destroy target land that opponent controls that can't produce {R}.
""",
    "s/servo_skull.txt": """Name:Servo-Skull
ManaCost:R
Types:Artifact Fortification
T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigAttach | TriggerDescription$ When CARDNAME enters, attach it to target land an opponent controls.
SVar:TrigAttach:DB$ Attach | ValidTgts$ Land.OppCtrl | TgtPrompt$ Select target land an opponent controls
T:Mode$ Taps | ValidCard$ Land.FortifiedBy | TriggerZones$ Battlefield | Execute$ TrigDamage | TriggerDescription$ Whenever fortified land becomes tapped, CARDNAME deals 1 damage to that land's controller.
SVar:TrigDamage:DB$ DealDamage | Defined$ TriggeredCardController | NumDmg$ 1
T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Land.FortifiedBy | TriggerZones$ Battlefield | Execute$ TrigSac | TriggerDescription$ When fortified land leaves the battlefield, sacrifice CARDNAME.
SVar:TrigSac:DB$ Sacrifice | Defined$ Self
Oracle:When Servo-Skull enters, attach it to target land an opponent controls.\\nWhenever fortified land becomes tapped, Servo-Skull deals 1 damage to that land's controller.\\nWhen fortified land leaves the battlefield, sacrifice Servo-Skull.\\nServo-Skull can't become attached to another land.
""",
}

EDITION = """[metadata]
Code=STF
Name=S.T Forge Prototype
Date=2026-10-04
Type=Custom

[CreatureTypes]
Astartes:Astartes

[cards]
1 U Drop Pod S.T
2 C Servitor
3 U Terminator S.T
4 U Pyroclast Squad S.T
5 U Infernus S.T
6 U Demolitionist S.T
7 R Whirlwind S.T
8 R Eradicator S.T
9 M Exterminatus S.T
10 M Vindicator S.T
11 U Servo-Skull
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

ST_FULL = {
    "Mountain": 24,
    "Drop Pod S.T": 4,
    "Servitor": 4,
    "Terminator S.T": 4,
    "Pyroclast Squad S.T": 4,
    "Infernus S.T": 4,
    "Demolitionist S.T": 4,
    "Whirlwind S.T": 4,
    "Eradicator S.T": 4,
    "Vindicator S.T": 2,
    "Servo-Skull": 1,
    "Exterminatus S.T": 1,
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

IMPLEMENTED_CUSTOM_CARDS = [
    "Drop Pod S.T",
    "Servitor",
    "Terminator S.T",
    "Pyroclast Squad S.T",
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
    lines = [
        "[metadata]",
        f"Name={name}",
        "Deck Type=Constructed",
        "[main]",
    ]
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
    write_text(decks / "ST Forge Full.dck", deck_text("ST Forge Full", ST_FULL))
    write_text(decks / "Benchmark Red Forge.dck", deck_text("Benchmark Red Forge", BENCHMARK_RED))

    manifest = {
        "forge_release_target": FORGE_RELEASE,
        "set_code": SET_CODE,
        "implemented_custom_cards": IMPLEMENTED_CUSTOM_CARDS,
        "deferred_full_st_cards": [],
        "smoke_deck_cards": sum(ST_SMOKE.values()),
        "full_deck_cards": sum(ST_FULL.values()),
        "benchmark_deck_cards": sum(BENCHMARK_RED.values()),
        "purpose": "Stage 1B full S.T Forge rules-engine validation",
    }
    write_text(user_dir / "forge_st_manifest.json", json.dumps(manifest, indent=2) + "\n")

    print(f"Forge S.T prototype exported to {user_dir}")
    print(f"Implemented custom cards: {len(IMPLEMENTED_CUSTOM_CARDS)}")
    print("S.T smoke deck: 60 cards")
    print("S.T full deck: 60 cards")
    print("Benchmark Red Forge deck: 60 cards")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Export S.T custom cards/decks into a Forge user directory")
    parser.add_argument("--user-dir", default=str(Path.home() / ".forge"))
    args = parser.parse_args()
    export(Path(args.user_dir).expanduser().resolve())
