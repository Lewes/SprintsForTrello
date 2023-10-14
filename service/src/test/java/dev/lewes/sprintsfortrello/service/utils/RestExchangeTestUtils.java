package dev.lewes.sprintsfortrello.service.utils;

import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.patch;
import static org.springframework.http.RequestEntity.post;

import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import java.net.URI;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class RestExchangeTestUtils {

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final TrelloProperties trelloProperties;

    public RestExchangeTestUtils(TrelloProperties trelloProperties) {
        this.trelloProperties = trelloProperties;
    }

    public <T> ResponseEntity<T> getAtUrl(String path, Class<T> responseType) {
        return restTemplate.exchange(get(URI.create(buildApiUrl(path))).accept(MediaType.APPLICATION_JSON).build(), responseType);
    }

    public <T> ResponseEntity<T> postAtUrl(String path, Object body, Class<T> responseType) {
        return restTemplate.exchange(post(URI.create(buildApiUrl(path)))
            .accept(MediaType.APPLICATION_JSON)
            .body(body), responseType);
    }


    public <T> ResponseEntity<T> patchAtUrl(String path, Object body, Class<T> responseType) {
        return restTemplate.exchange(patch(URI.create(buildApiUrl(path)))
            .accept(MediaType.APPLICATION_JSON)
            .body(body), responseType);
    }

    public String buildApiUrl(String path) {
        return trelloProperties.getUrl() + path;
    }


}