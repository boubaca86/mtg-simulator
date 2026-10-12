#!/usr/bin/env python3
"""Fail closed unless exactly eighty-six real Forge Stage10W TestNG methods ran and passed."""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

CLASS = "forge.ai.Stage10UUncuratedDrawTest"
EXPECTED = {
    "realPriorityRequiresTwoExternalActorPassesToAdvance",
    "realForgeTurnLoopAutomaticallyDrawsWithoutDevTurnSwitch",
    "realPriorityObservationOmitsForgedOpponentSecretsAndPhase",
    "realPriorityLoopAcceptsLandAndRejectsOutOfTurnProposal",
    "realLoopEnforcesOneLandThenResetsOnNextNaturalTurn",
    "illegalExtraActorCommandIsRejectedBeforeForgeCanChangeState",
    "landThenOneManaCreaturePaysForestAndWaitsForOpponentPriority",
    "twoManaCreaturePaysTwoForestsAndResolvesAfterPass",
    "insufficientManaCannotCastOrMutateHandLandOrStack",
    "opponentCannotCastSorcerySpeedCreatureOutsideOwnTurn",
    "forgedCreatureCommandsAndUnsupportedCardAreRejected",
    "realForgeUnblockedBearAttackDealsTwoDamage",
    "twoUnblockedBearsAttackForFourDamage",
    "cannotAttackWithSickOrTappedBears",
    "atomicAttackProposalRejectsMixedSickAndReadyCards",
    "forgedAttackPayloadsAndOutOfTurnAccessFailClosed",
    "authenticRegisteredCountsPersistAcrossRealPriorityLandAndDraw",
    "differentHiddenOrderWorldsFeedIdenticalObservationsToIndependentActors",
    "inheritedFullInformationDecisionCallbacksFailClosedWithoutFallback",
    "inheritedEntityAndSpellDecisionFamiliesCannotInspectRefereeState",
    "inheritedSacrificeAndDestroyChoicesDenyWithoutMutation",
    "untrustedBidStaticPaymentAndReplacementCallbacksFailClosed",
    "guardedConfirmationsNeverInspectOrDependOnOpponentPrivateZones",
    "hiddenPileNameAndLibraryOrderingCallbacksAllFailClosed",
    "hiddenChoiceRejectionsInvariantToOpponentsSecretHandAndLibrary",
    "physicalStartingLibraryMatchesRegisteredMainAndSurvivesRealDraws",
    "unapprovedMulliganAndTargetCallbacksDenyWithoutCallingPrivilegedAI",
    "cardFaceSpliceAndCostChoicesDenyWithoutStateMutation",
    "hiddenWorldChangesCannotAlterDeniedDecisionsOrActorObservations",
    "openingHandUsesOnlyOwnVisibleCardsAndActualSeparateProcesses",
    "bottomSelectionUsesRefereeCountAndOwnCurrentHandOnly",
    "invalidOpeningPayloadsCannotSelectRefereeCardsOrChangeLife",
    "opponentHiddenCardsCannotInfluenceOwnOpeningHandReply",
    "completeRefereePregameShufflesDrawsAndStartsRealPriorityForTwoActors",
    "twoPlayerPregameNeverTransfersHiddenMulliganOrShuffleInformation",
    "nativeForgeFirstTurnSkipsDrawWhenForestPlayerStarts",
    "nativeForgeFirstTurnSkipsDrawWhenIslandPlayerStarts",
    "nativeLondonMulliganKeepsFrozenRegisteredCardsAndIsolatesSecrets",
    "realMatchStartPerformsSixtyCardPrepMulliganAndNativeFirstTurn",
    "realMatchStartThenNormalTurnsRespectFirstPlayerDrawAndPrivacy",
    "nativeSixtyCardMatchProgressesFiveRealTurnsWithLegalLandDrops",
    "nativeMatchRunsSevenTurnsAndPreservesPrivateViewsAndDrawCounts",
    "unphasedActionPlanCastsPaidBearAndAttacksDuringNaturalMatchTurns",
    "actionPlanNeverDisclosesPhaseAndRejectsForgedOptions",
    "independentDefenderBlocksRealAttackerAndForgeDestroysBothBears",
    "blockerPreferenceRejectsForgedPairsAndDoesNotRevealCombatState",
    "twoIndependentAttackersAreBlockedOneToOneAndAllFourBearsDie",
    "flyingAttackerCannotBeBlockedByNonflyingBearDuringRealForgeCombat",
    "tappedAndNonattackingBlockPairsAreRejectedByActualForgeLegality",
    "menaceOneProposedBlockerIsIllegalAndCombatDamageHitsPlayer",
    "menaceTwoBlockersAreLegalAndPreventPlayerCombatDamage",
    "firstStrikeAttackerKillsOrdinaryBlockerWithoutTakingDamage",
    "firstStrikeBlockerKillsNormalAttackerAndSurvives",
    "refereeAllocatesOneToOneRegisteredBearsWithoutInheritedAi",
    "refereeAllocatesTwoBlockersAndRejectsUnsupportedWithoutInheritedAi",
    "refereeStrictlyRejectsDuplicateKeysInActionAndOpeningReplies",
    "refereeStrictlyRejectsNonfiniteTrailingAndOversizedActorReplies",
    "refereeRejectsTruncatedUnterminatedAndOverlongReplyFrames",
    "refereeRejectsMalformedUtf8AndReadsExactMultibyteFrames",
    "refereeDeadlineDeniesSilentActorAndKeepsGameUnchanged",
    "refereeDeadlineAcceptsPromptFramesAndDeniesPartialUnfinishedReply",
    "realSilentPythonActorTimesOutAndOtherActorContinues",
    "realPartialPythonOpeningReplyTimesOutAndOtherActorContinues",
    "realNonreadingPythonActorCannotBlockNormalRefereeSends",
    "realNonreadingPythonActorCannotBlockOpeningHandRefereeSends",
    "sharedExchangeDeadlineRejectsCombinedDelayWithoutStateMutation",
    "sharedExchangeDeadlineAcceptsPromptNormalAndOpeningHandResponses",
    "realDelayedPythonNormalReplyDeniedBySharedBudgetAndPeerSurvives",
    "realDelayedPythonOpeningReplyDeniedBySharedBudgetAndPeerSurvives",
    "realPromptPythonWorkerAnswersBothRequestsWithinSharedBudget",
    "nativeLethalUnblockedCombatRecordsForgeWinnerAndLoser",
    "nativeNonlethalUnblockedCombatDoesNotInventGameOver",
    "nativeRegisteredPaidCreatureCastThenNaturalCombatTriggersForgeWin",
    "nativeRegisteredPaidCreatureCastThenNonlethalCombatContinues",
    "nativeTwoSeparatePaidBearCombatsCauseCumulativeForgeVictory",
    "nativeTwoSeparatePaidBearCombatsLeaveOneLifeWithoutFalseWinner",
    "nativeTwentyLifeTenDistinctPaidBearCombatsProduceForgeWinner",
    "nativeTwentyLifeThreeDistinctPaidBearCombatsRemainNonlethal",
    "nativeBothManaPaidCombatantsBlockWhenAttackerRegisteredFirst",
    "nativeBothManaPaidCombatantsBlockWhenDefenderRegisteredFirst",
    "nativeUncuratedOpeningDrawAndPaidBearWhenAttackerRegisteredFirst",
    "nativeUncuratedOpeningDrawAndPaidBearWhenDefenderRegisteredFirst",
    "nativeBothUncuratedPlayersPayAndResolveBearsLeftRegisteredFirst",
    "nativeBothUncuratedPlayersPayAndResolveBearsRightRegisteredFirst",
    "nativeUncuratedPaidBearsContestedCombatAttackerRegisteredFirst",
    "nativeUncuratedPaidBearsContestedCombatDefenderRegisteredFirst",
}

def verify(folder: Path) -> dict:
    if not folder.is_dir():
        raise ValueError("Surefire reports missing")
    seen = {}
    for path in sorted(folder.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if case.get("classname") not in (CLASS, CLASS.rsplit(".", 1)[-1]):
                continue
            name = case.get("name")
            if not name or name in seen:
                raise ValueError("duplicated or anonymous method")
            failures = [e for e in case if e.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
            seen[name] = failures
    if set(seen) != EXPECTED:
        raise ValueError(f"methods differ: found {sorted(seen)}, expected {sorted(EXPECTED)}")
    for name, problems in seen.items():
        if problems:
            raise ValueError(f"test failed or skipped: {name}")
    return {"gate":"PASS","executed_methods":len(EXPECTED),
            "scope":"Pinned Forge native combat with two external actors: actual delayed and prompt external Python actors under shared I/O budget, native registered mana-paid creature, multi-turn cumulative combat and genuine 20-life-to-zero Forge victory, both actors paid casts and legal blockers across native turns, natural uncurated private opening hands and first paid creature spell, plus two independent uncurated players' genuine paid creature spells and naturally cast 2/2 versus 2/2 block/combat deaths, lethal/nonlethal game outcome, inherited backpressure, framing, damage, first-strike, menace and privacy regressions",
            "full_magic_game":False}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports),sort_keys=True))
    except (OSError,ValueError,ET.ParseError) as exc:
        print(json.dumps({"gate":"FAIL","reason":str(exc)}), file=sys.stderr)
        sys.exit(1)
