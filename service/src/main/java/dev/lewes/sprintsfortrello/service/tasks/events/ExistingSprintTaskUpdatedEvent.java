package dev.lewes.sprintsfortrello.service.tasks.events;

import dev.lewes.sprintsfortrello.service.tasks.SprintTask;

public class ExistingSprintTaskUpdatedEvent {

    private final SprintTask sprintTask;

    public ExistingSprintTaskUpdatedEvent(SprintTask sprintTask) {
        this.sprintTask = sprintTask;
    }

    public SprintTask getSprintTask() {
        return sprintTask;
    }

}
