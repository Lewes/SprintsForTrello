package dev.lewes.sprintsfortrello.service.tasks;

import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sprintTasks")
public class SprintTask {

    @Id
    private String id;

    private String cardId;

    private TrelloCard trelloCard;

    private Status status;

    private long timeCompleted;

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

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
