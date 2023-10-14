package dev.lewes.sprintsfortrello.service.sprint;

import java.util.HashMap;
import java.util.Map;

/**
 * SprintsForTrello - Developed by Lewes D. B. (Boomclaw). All rights reserved 2023.
 */
public class SprintProgress {

    private final Map<String, Integer> days2RemainingPoints = new HashMap<>();

    public Map<String, Integer> getDays2RemainingPoints() {
        return days2RemainingPoints;
    }

}
