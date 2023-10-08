package dev.lewes.sprintsfortrello.service.sprint;

import org.springframework.stereotype.Service;

@Service
public class SprintManager {

    private final SprintRepository sprintRepository;

    public SprintManager(SprintRepository sprintRepository) {
        this.sprintRepository = sprintRepository;
    }

}
