package dev.lewes.sprintsfortrello.service.tasks;

import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sprintTasks")
public class SprintTask {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SprintTask that = (SprintTask) o;
        return timeCompleted == that.timeCompleted && Objects.equals(id, that.id) && Objects.equals(trelloCard, that.trelloCard) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, trelloCard, status, timeCompleted);
    }

    @Id
    private String id;

    private TrelloCard trelloCard;

    private Status status;

    private long timeCompleted;

    public TrelloCard getTrelloCard() {
        return trelloCard;
    }

    public void setTrelloCard(TrelloCard trelloCard) {
        this.trelloCard = trelloCard;
    }

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        DONE
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public long getTimeCompleted() {
        return timeCompleted;
    }

}
