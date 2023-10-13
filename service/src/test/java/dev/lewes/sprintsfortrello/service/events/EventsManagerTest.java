package dev.lewes.sprintsfortrello.service.events;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {SprintsForTrelloApplication.class})
public class EventsManagerTest {

    @Autowired
    public EventsManager eventsManager;

    @Test
    public void registerEvents() {
        MockListener mockListener = new MockListener();
        eventsManager.registerListener(mockListener);

        UUID fakeCardId = UUID.randomUUID();

        eventsManager.fireEvent(new FakeTrelloCardEvent(fakeCardId));

        assertThat(mockListener.getReceivedFakeTrelloCardId(), is(equalTo(fakeCardId)));
    }

    public static class MockListener implements Listener {

        public UUID getReceivedFakeTrelloCardId() {
            return receivedFakeTrelloCardId;
        }

        private UUID receivedFakeTrelloCardId;

        @EventHandler
        public void fakeTrelloCardUpdated(FakeTrelloCardEvent event) {
            receivedFakeTrelloCardId = event.getCardId();
        }

    }

    public static class FakeTrelloCardEvent extends Event {

        public FakeTrelloCardEvent(UUID cardId) {
            this.cardId = cardId;
        }

        public UUID getCardId() {
            return cardId;
        }

        private final UUID cardId;

    }


}
