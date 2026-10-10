package forge.ai.simulation;

public final class Stage8vTopLevelTelemetryTest {
    public static void main(String[] args) {
        String event = Stage8vTopLevelTelemetry.export(20261041L, 2L, 4, 1);
        require(event.contains("\"top_level_candidate_count\":4"));
        require(event.contains("\"proposed_candidate_index\":1"));
        require(event.contains("\"complete_legal_enumeration\":false"));
        require(event.contains("\"priority_pass_enumerated\":false"));
        require(!event.contains("opponent_hand"));
        invalid(-1L, 4, 1);
        invalid(0L, 0, 0);
        invalid(0L, 4, 4);
        invalid(0L, 4, -1);
        System.out.println("Stage 8V Java telemetry tests passed");
    }

    private static void invalid(long decision, int count, int proposed) {
        try {
            Stage8vTopLevelTelemetry.export(5L, decision, count, proposed);
            throw new AssertionError("Expected invalid input to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("Stage 8V telemetry regression");
    }
}
