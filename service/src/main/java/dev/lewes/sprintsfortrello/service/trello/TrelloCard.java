package dev.lewes.sprintsfortrello.service.trello;

import java.util.Objects;
import org.springframework.data.annotation.Id;

public class TrelloCard {

    @Id
    private String id;

    public void setName(String name) {
        this.name = name;
    }

    public void setIdList(String idList) {
        this.idList = idList;
    }

    private String name;

    private String idList;

    private TrelloCard() {

    }

    public TrelloCard(String id, String name, String idList) {
        this.id = id;
        this.name = name;
        this.idList = idList;
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

    @Override
    public String toString() {
        return "TrelloCard{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", idList='" + idList + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TrelloCard that = (TrelloCard) o;

        return Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(idList, that.idList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, idList);
    }
}
