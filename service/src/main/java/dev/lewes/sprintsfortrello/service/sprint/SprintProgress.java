package dev.lewes.sprintsfortrello.service.sprint;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SprintsForTrello - Developed by Lewes D. B. (Boomclaw). All rights reserved 2023.
 */
public class SprintProgress {

    private final Map<String, Integer> days2RemainingPoints = new LinkedHashMap<>();

    private final Map<String, Double> days2ExpectedPoints = new LinkedHashMap<>();

    public Map<String, Integer> getDays2RemainingPoints() {
        return days2RemainingPoints;
    }

    public Map<String, Double> getDays2ExpectedPoints() {
        return days2ExpectedPoints;
    }

}
