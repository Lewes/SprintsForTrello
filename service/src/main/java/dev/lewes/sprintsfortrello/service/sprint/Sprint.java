package dev.lewes.sprintsfortrello.service.sprint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sprints")
public class Sprint {

    @Id
    private String id;
    private String name;

    private long startTime;

    private SprintStatus status;
    private boolean current;
    private Set<String> taskIds = new HashSet<>();

    private int estimatedDurationInDays;
    private int startingPoints;

    @JsonCreator
    public Sprint(@JsonProperty("name") String name,
        @JsonProperty("status") SprintStatus status) {
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

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public SprintStatus getStatus() {
        return status;
    }

    public void setStatus(SprintStatus status) {
        this.status = status;
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

    public int getEstimatedDurationInDays() {
        return estimatedDurationInDays;
    }

    public void setEstimatedDurationInDays(int estimatedDurationInDays) {
        this.estimatedDurationInDays = estimatedDurationInDays;
    }

    public int getStartingPoints() {
        return startingPoints;
    }

    public void setStartingPoints(int startingPoints) {
        this.startingPoints = startingPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Sprint sprint = (Sprint) o;

        return startTime == sprint.startTime &&
            current == sprint.current &&
            estimatedDurationInDays == sprint.estimatedDurationInDays &&
            startingPoints == sprint.startingPoints &&
            Objects.equals(id, sprint.id) &&
            Objects.equals(name, sprint.name) &&
            status == sprint.status &&
            Objects.equals(taskIds, sprint.taskIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startTime, status, current, taskIds, estimatedDurationInDays, startingPoints);
    }

    enum SprintStatus {
        PLANNING(0),
        IN_PROGRESS(1),
        ENDED(2);

        private final int time;

        SprintStatus(int time) {
            this.time = time;
        }

        public int getTime() {
            return time;
        }

        public boolean isBefore(SprintStatus other) {
            return time < other.getTime();
        }

    }

}
