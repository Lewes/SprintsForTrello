package dev.lewes.sprintsfortrello.service.sprint;

import static org.springframework.http.ResponseEntity.status;

import dev.lewes.sprintsfortrello.service.sprint.Sprint.Status;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.SprintTaskRepository;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SprintController {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private SprintTaskRepository sprintTaskRepository;

    @Autowired
    private TrelloProperties connectionProperties;

    @PostMapping("sprints")
    public ResponseEntity<Sprint> createSprint(@RequestBody Map<String, Object> params) {
        String name = (String) params.get("name");
        boolean current = (boolean) params.getOrDefault("current", false);

        Sprint sprint = new Sprint(name, Status.PLANNING);
        sprint.setCurrent(current);

        sprintRepository.save(sprint);

        return ResponseEntity.ok(sprint);
    }

    @GetMapping("sprints/{id}")
    public ResponseEntity<Sprint> getSprint(@PathVariable String id) {
        Optional<Sprint> sprint = getSprintByIdOrSynonym(id);

        return sprint.map(ResponseEntity::ok)
            .orElseGet(() -> status(HttpStatus.NOT_FOUND).build());
    }

    private Optional<Sprint> getSprintByIdOrSynonym(String id) {
        Optional<Sprint> sprint;

        if(id.equalsIgnoreCase("current")) {
            sprint = sprintRepository.getCurrentSprint();
        } else {
            sprint = sprintRepository.findById(id);
        }
        return sprint;
    }

    @PatchMapping("sprints/{id}")
    public ResponseEntity<Sprint> updateSprint(@PathVariable String id, @RequestBody Map<String, Object> params) {
        Sprint sprint = getSprintByIdOrSynonym(id).get();

        boolean current = (boolean) params.get("current");
        sprint.setCurrent(current);

        sprintRepository.save(sprint);

        return ResponseEntity.ok(sprint);
    }

    @GetMapping("sprints/{id}/tasks")
    public ResponseEntity<List<SprintTask>> getSprintTasks(@PathVariable String id) {
        Optional<Sprint> sprint = getSprintByIdOrSynonym(id);

        if(sprint.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<SprintTask> list = sprint.get().getTaskIds().stream()
            .map(taskId -> sprintTaskRepository.findById(taskId).get())
            .toList();

        return ResponseEntity.ok(list);
    }

    @PostMapping("sprints/{id}/tasks")
    public ResponseEntity<List<SprintTask>> addTasksToSprint(@PathVariable String id) {
        Optional<Sprint> sprint = getSprintByIdOrSynonym(id);

        if(sprint.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        sprint.get().getTaskIds().addAll(sprintTaskRepository.findAll().stream()
            .filter(task -> task.getTrelloCard().getIdList().equals(connectionProperties.getBacklogColumnId()))
            .map(SprintTask::getId)
            .toList());

        sprintRepository.save(sprint.get());

        List<SprintTask> list = sprint.get().getTaskIds().stream()
            .map(taskId -> sprintTaskRepository.findById(taskId).get())
            .toList();

        return ResponseEntity.ok(list);
    }

}
