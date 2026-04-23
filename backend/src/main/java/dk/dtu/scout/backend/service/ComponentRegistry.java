package dk.dtu.scout.backend.service;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponentRegistry<T extends ScoutComponent> {

    private final Map<String, Class<? extends T>> componentClassById;
    private final ApplicationContext applicationContext;

    public ComponentRegistry(List<T> components, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.componentClassById = new LinkedHashMap<>();

        for (T component : components) {
            String id = component.id();
            @SuppressWarnings("unchecked") // Safe because we only put classes of T in the map
            Class<? extends T> componentClass = (Class<? extends T>) component.getClass();
            Class<? extends T> existingClass = componentClassById.putIfAbsent(id, componentClass);
            if (existingClass != null) {
                throw new IllegalStateException("Duplicate component ID between classes: " + existingClass.getSimpleName() + " and " + componentClass.getSimpleName());
            }
        }
    }

    public T create(String id) {
        Class<? extends T> componentClass = componentClassById.get(id);
        if (componentClass == null) {
            throw new BadRequestException("Unknown component: " + id + ". Available: " + componentClassById.keySet());
        }
        return applicationContext.getBean(componentClass);
    }
}