package dev.lewes.sprintsfortrello.service.tasks.events;

import dev.lewes.sprintsfortrello.service.events.Event;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;

public abstract class SprintTaskEvent extends Event {

    private final SprintTask sprintTask;

    protected SprintTaskEvent(SprintTask sprintTask) {
        this.sprintTask = sprintTask;
    }

    public SprintTask getSprintTask() {
        return sprintTask;
    }

}