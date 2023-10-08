package dev.lewes.sprintsfortrello.service.sprint;

import dev.lewes.sprintsfortrello.service.sprint.Sprint.Status;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SprintController {

    @Autowired
    private SprintRepository sprintRepository;

    @PostMapping("sprints")
    public ResponseEntity<Sprint> createSprint(@RequestBody Map<String, String> params) {
        String name = params.get("name");

        Sprint sprint = new Sprint(name, Status.PLANNING);

        sprintRepository.save(sprint);

        return ResponseEntity.ok(sprint);
    }

}
