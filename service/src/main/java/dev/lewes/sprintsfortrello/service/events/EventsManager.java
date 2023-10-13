package dev.lewes.sprintsfortrello.service.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EventsManager {

    private Set<Listener> listeners = new HashSet<>();

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public void fireEvent(Object object) {
        for(Listener listener : listeners) {
            for(Method method : listener.getClass().getMethods()) {
                if(!method.isAnnotationPresent(EventHandler.class)) {
                    continue;
                }

                if(!method.getParameterTypes()[0].isAssignableFrom(object.getClass())) {
                    continue;
                }

                try {
                    method.invoke(listener, object);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
