package dk.dtu.scout.backend.service;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the mapping from component id to Spring bean class for one component category.
 * The registry is used during run execution, where requests only contain string ids
 * such as "tsp", "2opt", or "max-iterations".
 * @param <T> the component interface type
 * @author s235257
 */
public class ComponentRegistry<T extends ScoutComponent> {

    private final Map<String, Class<? extends T>> componentClassById;
    private final ApplicationContext applicationContext;

    /**
     * Creates a registry from the available component beans.
     * Duplicate component ids are rejected to ensure that each id uniquely identifies one component class.
     * @param components the component beans to register
     * @param applicationContext the Spring context used to retrieve component instances
     */
    public ComponentRegistry(List<T> components, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.componentClassById = new LinkedHashMap<>();

        for (T component : components) {
            String id = component.id();

            @SuppressWarnings("unchecked") // Safe because the list only contains components of type T.
            Class<? extends T> componentClass = (Class<? extends T>) component.getClass();

            Class<? extends T> existingClass = componentClassById.putIfAbsent(id, componentClass);
            if (existingClass != null) {
                throw new IllegalStateException("Duplicate component ID between classes: " + existingClass.getSimpleName() + " and " + componentClass.getSimpleName());
            }
        }
    }

    /**
     * Returns a Spring-managed component instance for the given component id.
     * @param id the component id requested by the frontend/run request
     * @return the matching component instance
     */
    public T create(String id) {
        Class<? extends T> componentClass = componentClassById.get(id);
        if (componentClass == null) {
            throw new BadRequestException("Unknown component: " + id + ". Available: " + componentClassById.keySet());
        }

        return applicationContext.getBean(componentClass);
    }
}