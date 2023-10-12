package dev.lewes.sprintsfortrello.service.sprint;

import dev.lewes.sprintsfortrello.service.sprint.Sprint.Status;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.SprintTaskRepository;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Sprint> createSprint(@RequestBody Map<String, String> params) {
        String name = params.get("name");

        Sprint sprint = new Sprint(name, Status.PLANNING);

        sprintRepository.save(sprint);

        return ResponseEntity.ok(sprint);
    }

    @PostMapping("sprints/{id}/tasks")
    public ResponseEntity<Sprint> addTasksToSprint(@PathVariable String id) {
        Sprint sprint = sprintRepository.findById(id).get();

        sprint.getTaskIds().addAll(sprintTaskRepository.findAll().stream()
            .filter(task -> task.getTrelloCard().getIdList().equals(connectionProperties.getBacklogColumnId()))
            .map(SprintTask::getId).toList());

        return ResponseEntity.ok(sprint);
    }

}
