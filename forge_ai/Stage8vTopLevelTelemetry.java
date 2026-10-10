package forge.ai.simulation;

/** Read-only counts of AI-filtered top-level candidates; not all legal actions. */
public final class Stage8vTopLevelTelemetry {
    private Stage8vTopLevelTelemetry() {}
    public static String export(long seed, long decision, int count, int chosen) {
        if (decision < 0 || count < 1 || chosen < 0 || chosen >= count)
            throw new IllegalArgumentException("invalid proposed index");
        return "{\"schema_version\":\"stage8v-ai-filtered-top-level-v1\","
                + "\"source\":\"SpellAbilityPicker.getCandidateSpellsAndAbilities\","
                + "\"complete_legal_enumeration\":false,"
                + "\"target_combinations_enumerated\":false,"
                + "\"priority_pass_enumerated\":false,"
                + "\"selected_status\":\"proposed_before_execution\","
                + "\"run_seed\":" + seed + ","
                + "\"decision_index\":" + decision + ","
                + "\"top_level_candidate_count\":" + count + ","
                + "\"proposed_candidate_index\":" + chosen + "}";
    }
}
