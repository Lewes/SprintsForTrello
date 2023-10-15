package dev.lewes.sprintsfortrello.service.tasks.events;

import dev.lewes.sprintsfortrello.service.tasks.SprintTask;

public class SprintTaskRemovedEvent extends SprintTaskEvent {

    public SprintTaskRemovedEvent(SprintTask sprintTask) {
        super(sprintTask);
    }

}
