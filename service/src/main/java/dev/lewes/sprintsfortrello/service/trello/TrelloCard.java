package dev.lewes.sprintsfortrello.service.trello;

public class TrelloCard {

    private String id;

    private String name;

    private String idList;

    private TrelloCard() {

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIdList() {
        return idList;
    }
}
