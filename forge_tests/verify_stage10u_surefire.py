#!/usr/bin/env python3
"""Fail closed unless exactly 152 real Forge TestNG methods ran and passed."""
import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

CLASS = "forge.ai.Stage10UUncuratedDrawTest"
EXPECTED = {
    "nativeTwoLaneBearElfLeftRegisteredFirst",
    "nativeTwoLaneBearElfRightRegisteredFirst",
    "nativeEmptyLibraryAttackerLosesOnlyOnDraw",
    "nativeEmptyLibraryDefenderLosesOnlyOnDraw",
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
    "nativeTwoSuccessiveUncuratedBlocksAttackerRegisteredFirst",
    "nativeTwoSuccessiveUncuratedBlocksDefenderRegisteredFirst",
    "nativeUncuratedTwentyLifeForgeOutcomeAttackerRegisteredFirst",
    "nativeUncuratedTwentyLifeForgeOutcomeDefenderRegisteredFirst",
    "nativeNaturallyDepletedLibraryAfterRealContestedCombatAttackerRegisteredFirst",
    "nativeNaturallyDepletedLibraryAfterRealContestedCombatDefenderRegisteredFirst",
    "nativeReciprocalAttackCapablePlayersPlayToActualOutcomeLeftRegisteredFirst",
    "nativeReciprocalAttackCapablePlayersPlayToActualOutcomeRightRegisteredFirst",
    "nativeLifeAwareCounterattackNaturalGameLeftRegisteredFirst",
    "nativeLifeAwareCounterattackNaturalGameRightRegisteredFirst",
    "nativeReserveDeploymentNaturalGameLeftRegisteredFirst",
    "nativeReserveDeploymentNaturalGameRightRegisteredFirst",
    "nativeLethalRiskReserveNaturalGameLeftRegisteredFirst",
    "nativeLethalRiskReserveNaturalGameRightRegisteredFirst",
    "nativeEarlyReserveNaturalGameLeftRegisteredFirst",
    "nativeEarlyReserveNaturalGameRightRegisteredFirst",
    "nativeReactiveHealthyBlockingCohortLeftRegisteredFirst",
    "nativeReactiveHealthyBlockingCohortRightRegisteredFirst",
    "nativeThreeSpeciesVanillaBearCohortLeftRegisteredFirst",
    "nativeThreeSpeciesVanillaBearCohortRightRegisteredFirst",
    "nativeUnequalVanillaCombatLeftRegisteredFirst",
    "nativeUnequalVanillaCombatRightRegisteredFirst",
    "nativeCooperativeBlockingLeftRegisteredFirst",
    "nativeCooperativeBlockingRightRegisteredFirst",
    "nativeFourStrengthVanillaLeftRegisteredFirst",
    "nativeFourStrengthVanillaRightRegisteredFirst",
    "nativeGeneralCooperationLeftRegisteredFirst",
    "nativeGeneralCooperationRightRegisteredFirst",
    "nativeDamageDivisionLeftRegisteredFirst",
    "nativeDamageDivisionRightRegisteredFirst",
    "nativeDamageAwareSplitAndTwoBlockerDeathsLeftRegisteredFirst",
    "nativeDamageAwareSplitAndTwoBlockerDeathsRightRegisteredFirst",
    "nativePublicLethalSwarmLeftRegisteredFirst",
    "nativePublicLethalSwarmRightRegisteredFirst",
    "nativeNonlethalMixedStrengthPressureLeftRegisteredFirst",
    "nativeNonlethalMixedStrengthPressureRightRegisteredFirst",
    "nativeEmergencyJointBlockLeftRegisteredFirst",
    "nativeEmergencyJointBlockRightRegisteredFirst",
    "nativeMixedEmergencyJointBlockLeftRegisteredFirst",
    "nativeMixedEmergencyJointBlockRightRegisteredFirst",
    "nativeThreePowerEmergencyBlockLeftRegisteredFirst",
    "nativeThreePowerEmergencyBlockRightRegisteredFirst",
    "nativeBasicForestPressureLeftRegisteredFirst",
    "nativeBasicForestPressureRightRegisteredFirst",
    "nativePublicLethalBeatsReserveLeftRegisteredFirst",
    "nativePublicLethalBeatsReserveRightRegisteredFirst",
    "nativeNonlethalAllSmallPressureLeftRegisteredFirst",
    "nativeNonlethalAllSmallPressureRightRegisteredFirst",
    "nativeTwoLaneEmergencyDefenseLeftRegisteredFirst",
    "nativeTwoLaneEmergencyDefenseRightRegisteredFirst",
    "nativeLlanowarEmergencyChumpLeftRegisteredFirst",
    "nativeLlanowarEmergencyChumpRightRegisteredFirst",
    "refereeAllocatesLethalSplitAcrossElfAndBearTargets",
    "refereeRejectsInvalidElfDamageBindingsWithoutMutation",
    "stage11yOwnHandPreferenceUsesIndependentWorkersAndIgnoresPrivateWorlds",
    "stage11yMalformedHandPreferencesCannotChooseDuplicateHiddenOrNonintegerCards",
    "stage11yCleanupRejectsWrongCountTurnAndPhaseBeforeActorChoice",
    "stage11yStaleAndForeignHandBindingsAreRejectedBeforeAnyDiscard",
    "stage11yNativeCleanupDiscardsOwnCardsClearsDamageAndResumesTurns",
    "stage11yNativeCleanupUsesRefereeModifiedMaximumWithoutLeakingIt",
    "stage11yNaturalCleanupCapableMixedGameLeftRegisteredFirst",
    "stage11yNaturalCleanupCapableMixedGameRightRegisteredFirst",
}

REJECTIONS = {
    "illegalExtraActorCommandIsRejectedBeforeForgeCanChangeState",
    "forgedCreatureCommandsAndUnsupportedCardAreRejected",
    "forgedAttackPayloadsAndOutOfTurnAccessFailClosed",
}


def cases(folder: Path):
    if not folder.is_dir():
        raise ValueError("Surefire reports missing")
    seen = {}
    for path in sorted(folder.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter():
            if case.tag.rsplit("}", 1)[-1] != "testcase":
                continue
            if case.get("classname") != CLASS:
                raise ValueError("unexpected test class")
            name = case.get("name")
            if not name or name in seen:
                raise ValueError("duplicate or anonymous test method")
            seen[name] = [child for child in case
                          if child.tag.rsplit("}", 1)[-1] in ("failure", "error", "skipped")]
    return seen


def verify(folder: Path) -> dict:
    seen = cases(folder)
    if set(seen) != EXPECTED:
        raise ValueError(f"methods differ: missing={sorted(EXPECTED - seen.keys())}, extra={sorted(seen.keys() - EXPECTED)}")
    for name, problems in seen.items():
        if problems:
            raise ValueError(f"test failed or skipped: {name}")
    return {"gate": "PASS", "test_class": CLASS, "executed_methods": len(seen),
            "previous_regressions": 144, "own_hand_cleanup_cases": 8,
            "full_magic_game": False, "reserved_seed_games_run": False}


def verify_mutation(folder: Path, maven_exit: int) -> dict:
    if maven_exit != 1:
        raise ValueError("expected Maven test failure, not success or process crash")
    seen = cases(folder)
    if set(seen) != REJECTIONS:
        raise ValueError("mutation must execute exactly the three repaired rejection methods")
    for name, problems in seen.items():
        if len(problems) != 1:
            raise ValueError(f"mutation survived or had multiple failures: {name}")
        failure = problems[0]
        if (failure.tag.rsplit("}", 1)[-1] != "failure"
                or failure.get("type") != "java.lang.AssertionError"
                or "FORGED_PAYLOAD_ACCEPTED:" not in failure.get("message", "")):
            raise ValueError(f"unexpected failure does not prove mutation detection: {name}")
    return {"gate": "PASS", "disabled_validator_detected_by": sorted(seen)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.reports), sort_keys=True))
    except (OSError, ValueError, ET.ParseError) as exc:
        print(json.dumps({"gate": "FAIL", "reason": str(exc)}), file=sys.stderr)
        sys.exit(1)
