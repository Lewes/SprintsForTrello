package dev.lewes.sprintsfortrello.service.sprint;

import static org.springframework.http.ResponseEntity.status;

import dev.lewes.sprintsfortrello.service.sprint.Sprint.SprintStatus;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask.Status;
import dev.lewes.sprintsfortrello.service.tasks.SprintTaskRepository;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

        Sprint sprint = new Sprint(name, SprintStatus.PLANNING);
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

        boolean current = (boolean) params.getOrDefault("current", false);
        sprint.setCurrent(current);

        if(params.containsKey("status")) {
            SprintStatus newStatus = SprintStatus.valueOf((String) params.get("status"));

            if(newStatus == sprint.getStatus() ||
                newStatus.isBefore(sprint.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            if(newStatus == SprintStatus.IN_PROGRESS) {
                sprint.setStartTime(System.currentTimeMillis());
                sprint.setEstimatedDurationInDays(connectionProperties.getSprintLengthInDays());
                sprint.setStartingPoints(sprint.getTaskIds().size());
            }

            sprint.setStatus(newStatus);
        }

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

    @GetMapping("sprints/{id}/progress")
    public ResponseEntity<SprintProgress> getSprintProgress(@PathVariable String id) {
        SprintProgress sprintProgress = new SprintProgress();

        Optional<Sprint> sprint = getSprintByIdOrSynonym(id);

        if(sprint.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if(sprint.get().getStatus() == SprintStatus.PLANNING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(sprint.get().getStartTime());

        List<SprintTask> tasks = sprint.get().getTaskIds().stream()
            .map(taskId -> sprintTaskRepository.findById(taskId).get())
            .toList();

        int daysCompleted = daysBetween(calendar, Calendar.getInstance());

        for(int x = 0; x <= daysCompleted; x++) {
            Calendar newCalendar = (Calendar) calendar.clone();

            newCalendar.add(Calendar.DAY_OF_YEAR, x);

            int value = 0;

            for(SprintTask task : tasks) {
                if(task.getStatus() == Status.NOT_STARTED ||
                    task.getStatus() == Status.IN_PROGRESS ||
                    (task.getStatus() == Status.DONE && task.getTimeCompleted() < newCalendar.getTimeInMillis())) {
                    value++;
                }
            }

            sprintProgress.getDays2RemainingPoints().put(dateToFormatted(newCalendar.getTime()), value);
        }

        for(int x = 0; x <= sprint.get().getEstimatedDurationInDays(); x++) {
            Calendar newCalendar = (Calendar) calendar.clone();

            newCalendar.add(Calendar.DAY_OF_YEAR, x);

            double expected = (double) sprint.get().getStartingPoints() * ((double) (sprint.get().getEstimatedDurationInDays() - x) / sprint.get().getEstimatedDurationInDays());

            sprintProgress.getDays2ExpectedPoints().put(dateToFormatted(newCalendar.getTime()), expected);
        }

        return ResponseEntity.ok(sprintProgress);
    }

    private int daysBetween(Calendar startDate, Calendar endDate) {
        long end = endDate.getTimeInMillis();
        long start = startDate.getTimeInMillis();

        return (int) TimeUnit.MILLISECONDS.toDays(Math.abs(end - start));
    }

    private String dateToFormatted(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        return simpleDateFormat.format(date);
    }

}