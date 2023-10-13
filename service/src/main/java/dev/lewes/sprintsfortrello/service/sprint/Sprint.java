package dev.lewes.sprintsfortrello.service.sprint;

import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sprints")
public class Sprint {

    @Id
    private String id;
    private String name;
    private long startTime;
    private long endTime;
    private Status status;
    private boolean current;
    private Set<String> taskIds = new HashSet<>();

    private Sprint() {

    }

    public Sprint(String name, Status status) {
        this.name = name;
        this.status = status;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
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

    public Set<String> getTaskIds() {
        return taskIds;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    enum Status {
        PLANNING,
        IN_PROGRESS,
        ENDED
    }

}
