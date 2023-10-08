package dev.lewes.sprintsfortrello.service.sprint.task;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sprintTasks")
public class SprintTask {

    @Id
    private String id;

    private String cardId;

    private Status status;

    private long timeCompleted;

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        DONE
    }

    public String getId() {
        return id;
    }

    public String getCardId() {
        return cardId;
    }

    public Status getStatus() {
        return status;
    }

    public long getTimeCompleted() {
        return timeCompleted;
    }

}
