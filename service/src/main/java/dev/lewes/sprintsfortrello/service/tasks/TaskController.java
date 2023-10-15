package dev.lewes.sprintsfortrello.service.tasks;

import dev.lewes.sprintsfortrello.service.events.EventsManager;
import dev.lewes.sprintsfortrello.service.tasks.events.ExistingSprintTaskUpdatedEvent;
import dev.lewes.sprintsfortrello.service.tasks.events.NewSprintTaskEvent;
import dev.lewes.sprintsfortrello.service.tasks.events.SprintTaskEvent;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    @Autowired
    private TrelloService trelloService;

    @Autowired
    private SprintTaskRepository sprintTaskRepository;

    @Autowired
    private EventsManager eventsManager;

    @GetMapping("tasks")
    public ResponseEntity<List<SprintTask>> getAllTasks() {
        return ResponseEntity.ok(sprintTaskRepository.findAll());
    }

    @PostMapping("tasks")
    public synchronized ResponseEntity<List<SprintTask>> syncTasksFromTrello() {
        List<TrelloCard> cards = trelloService.getCards(trelloService.getSprintBoardId());
        List<SprintTaskEvent> sprintTaskEvents = new ArrayList<>();

        List<SprintTask> tasks = cards.stream()
            .map(card -> {
                SprintTask sprintTask = new SprintTask();

                Optional<SprintTask> existing = sprintTaskRepository.findByTrelloCardId(card.getId());

                if(existing.isPresent()) {
                    sprintTask = existing.get();

                    sprintTaskEvents.add(new ExistingSprintTaskUpdatedEvent(sprintTask));
                } else {
                    sprintTaskEvents.add(new NewSprintTaskEvent(sprintTask));
                }

                sprintTask.setTrelloCard(card);
                sprintTask.setPoints(cardToPoints(card));

                return sprintTask;
            })
            .collect(Collectors.toList());

        sprintTaskRepository.saveAll(tasks);

        sprintTaskEvents.forEach(event -> eventsManager.fireEvent(event));

        return ResponseEntity.ok(sprintTaskRepository.findAll());
    }

    public int cardToPoints(TrelloCard card) {
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(card.getName());

        if(matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

}
