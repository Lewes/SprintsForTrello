package dev.lewes.sprintsfortrello.service.tasks.events;

import dev.lewes.sprintsfortrello.service.tasks.SprintTask;

public class ExistingSprintTaskUpdatedEvent extends SprintTaskEvent {

    public ExistingSprintTaskUpdatedEvent(SprintTask sprintTask) {
        super(sprintTask);
    }

}
