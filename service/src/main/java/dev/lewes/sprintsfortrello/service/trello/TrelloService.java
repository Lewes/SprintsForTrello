package dev.lewes.sprintsfortrello.service.trello;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TrelloService {

    @Autowired
    private TrelloConnectionProperties trelloConnectionProperties;

    @Autowired
    private RestTemplate restTemplate;

    public boolean connect() {
        String apiUrl = trelloConnectionProperties.buildApiUrl("1/members/me");

        ResponseEntity<JsonNode> responseEntity;

        try {
            responseEntity = restTemplate.exchange(RequestEntity.get(URI.create(apiUrl))
                .accept(MediaType.APPLICATION_JSON)
                .build(), JsonNode.class);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    public Optional<TrelloBoard> getBoard(String id) {
        String apiUrl = trelloConnectionProperties.buildApiUrl("1/boards/" + id);

        try {
            return Optional.ofNullable(restTemplate.exchange(RequestEntity.get(URI.create(apiUrl))
                .accept(MediaType.APPLICATION_JSON)
                .build(), TrelloBoard.class).getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<TrelloCard> getCards(String boardId) {
        String apiUrl = trelloConnectionProperties.buildApiUrl("1/cards/" + boardId);

        try {
            return List.of(restTemplate.exchange(RequestEntity.get(URI.create(apiUrl))
                .accept(MediaType.APPLICATION_JSON)
                .build(), TrelloCard[].class).getBody());
        } catch (Exception e) {
            e.printStackTrace();

            return Collections.emptyList();
        }
    }

}