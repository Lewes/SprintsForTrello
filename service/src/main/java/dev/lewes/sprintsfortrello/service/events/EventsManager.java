package dev.lewes.sprintsfortrello.service.events;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EventsManager {

    private final Set<Listener> listeners = new HashSet<>();

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public void fireEvent(Event event) {
        for(Listener listener : listeners) {
            for(Method method : listener.getClass().getMethods()) {
                if(!method.isAnnotationPresent(EventHandler.class)) {
                    continue;
                }

                if(!doesListenerParameterExtendEvent(event, method)) {
                    continue;
                }

                try {
                    method.invoke(listener, event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static boolean doesListenerParameterExtendEvent(Event event, Method method) {
        return method.getParameterTypes()[0].isAssignableFrom(event.getClass());
    }

}
