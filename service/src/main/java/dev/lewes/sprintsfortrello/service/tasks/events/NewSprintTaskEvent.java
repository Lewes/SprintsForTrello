package dev.lewes.sprintsfortrello.service.tasks.events;

import dev.lewes.sprintsfortrello.service.tasks.SprintTask;

public class NewSprintTaskEvent {

    private final SprintTask sprintTask;

    public NewSprintTaskEvent(SprintTask sprintTask) {
        this.sprintTask = sprintTask;
    }

    public SprintTask getSprintTask() {
        return sprintTask;
    }

}
