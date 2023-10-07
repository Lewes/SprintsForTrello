package dev.lewes.sprintsfortrello.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrelloService {

    private final TrelloConnectionProperties trelloConnectionProperties;

    @Autowired
    public TrelloService(TrelloConnectionProperties trelloConnectionProperties) {
        this.trelloConnectionProperties = trelloConnectionProperties;
    }

}