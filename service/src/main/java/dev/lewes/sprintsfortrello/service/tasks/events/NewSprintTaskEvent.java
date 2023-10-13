package dev.lewes.sprintsfortrello.service.tasks.events;

import dev.lewes.sprintsfortrello.service.tasks.SprintTask;

public class NewSprintTaskEvent extends SprintTaskEvent {

    public NewSprintTaskEvent(SprintTask sprintTask) {
        super(sprintTask);
    }

}
