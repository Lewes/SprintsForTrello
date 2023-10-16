package dev.lewes.sprintsfortrello.service.trello;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.springframework.data.annotation.Id;

public class TrelloCard {

    @Id
    private String id;
    private String name;
    private String idList;

    @JsonCreator
    public TrelloCard(@JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("idList") String idList) {
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

    public void setName(String name) {
        this.name = name;
    }

    public String getIdList() {
        return idList;
    }

    public void setIdList(String idList) {
        this.idList = idList;
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