package dev.lewes.sprintsfortrello.service.sprint;

import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sprints")
public class Sprint {

    @Id
    private String id;

    public String getName() {
        return name;
    }

    private String name;

    private long startTime;

    private long endTime;

    private Status status;

    private Set<String> taskIds = new HashSet<>();

    public Set<String> getTaskIds() {
        return taskIds;
    }

    private Sprint() {

    }

    public Sprint(String name, Status status) {
        this.name = name;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Status getStatus() {
        return status;
    }

    enum Status {
        PLANNING,
        IN_PROGRESS,
        ENDED
    }

}
