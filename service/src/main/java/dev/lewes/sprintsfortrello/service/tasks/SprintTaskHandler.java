package dev.lewes.sprintsfortrello.service.tasks;

import dev.lewes.sprintsfortrello.service.events.EventHandler;
import dev.lewes.sprintsfortrello.service.events.EventsManager;
import dev.lewes.sprintsfortrello.service.events.Listener;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask.Status;
import dev.lewes.sprintsfortrello.service.tasks.events.ExistingSprintTaskUpdatedEvent;
import dev.lewes.sprintsfortrello.service.tasks.events.NewSprintTaskEvent;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SprintTaskHandler implements Listener {

    @Autowired
    private SprintTaskRepository sprintTaskRepository;

    @Autowired
    private TrelloProperties trelloProperties;

    public SprintTaskHandler(EventsManager eventsManager) {
        eventsManager.registerListener(this);
    }

    @EventHandler
    public void onNewSprintTask(NewSprintTaskEvent event) {
        updateSprintTaskStatusToReflectTrello(event.getSprintTask());
    }

    @EventHandler
    public void onExistingTaskUpdated(ExistingSprintTaskUpdatedEvent event) {
        updateSprintTaskStatusToReflectTrello(event.getSprintTask());
    }

    public void updateSprintTaskStatusToReflectTrello(SprintTask task) {
        if(task.getTrelloCard().getIdList().equals(trelloProperties.getBacklogColumnId())) {
            task.setStatus(Status.NOT_STARTED);
        }

        if(task.getTrelloCard().getIdList().equals(trelloProperties.getDoneColumnId()) && task.getStatus() != Status.DONE) {
            task.setTimeCompleted(System.currentTimeMillis());
            task.setStatus(Status.DONE);
        }

        this.sprintTaskRepository.save(task);
    }

}
