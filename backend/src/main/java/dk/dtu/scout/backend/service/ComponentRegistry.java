package dk.dtu.scout.backend.service;

import dk.dtu.scout.Component;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComponentRegistry<T extends Component> {

    private final Map<String, Class<? extends T>> componentClassById;
    private final ApplicationContext applicationContext;

    public ComponentRegistry(List<T> components, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.componentClassById = components.stream()
            .collect(Collectors.toMap(
                Component::id,
                component -> (Class<? extends T>) component.getClass(),
                (c1, c2) -> {
                    throw new IllegalStateException(
                        "Duplicate component ID between classes: " +
                        c1.getSimpleName() + " and " + c2.getSimpleName()
                    );
                }
            ));
    }

    public T create(String id) {
        Class<? extends T> componentClass = componentClassById.get(id);
        if (componentClass == null) {
            throw new IllegalArgumentException(
                "Unknown component: " + id +
                ". Available: " + componentClassById.keySet()
            );
        }
        return applicationContext.getBean(componentClass);
    }
}
