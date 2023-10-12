package dev.lewes.sprintsfortrello.service.events;

public abstract class LazyListener<T> implements Listener {

    public abstract void on(T t);

    @EventHandler
    public void execute(T t) {
        on(t);
    }

}
