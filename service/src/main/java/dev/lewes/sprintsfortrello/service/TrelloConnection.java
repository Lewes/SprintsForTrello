package dev.lewes.sprintsfortrello.service;

import java.net.URI;
import org.bson.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class TrelloConnection {

    @Autowired
    private TrelloConnectionProperties trelloConnectionProperties;

    @Autowired
    private RestTemplate restTemplate;

    public boolean connect() {
        String apiUrl = trelloConnectionProperties.getUrl() + "1/members/me?key=" + trelloConnectionProperties.getApiKey() + "&token=" + trelloConnectionProperties.getToken();

        ResponseEntity<JsonObject> responseEntity;

        try {
            responseEntity = restTemplate.exchange(RequestEntity.get(URI.create(apiUrl))
                .accept(MediaType.APPLICATION_JSON)
                .build(), JsonObject.class);
        } catch (Exception e) {
            return false;
        }

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

}
