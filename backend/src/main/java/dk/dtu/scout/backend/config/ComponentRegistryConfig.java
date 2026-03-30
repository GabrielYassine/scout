package dk.dtu.scout.backend.config;

import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.service.ComponentRegistry;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for component registries.
 * Creates Spring beans that auto-discover all components of each type.
 */
@Configuration
public class ComponentRegistryConfig {

    @Bean
    public ComponentRegistry<SelectionRule> selectionRuleRegistry(
            List<SelectionRule> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<ParentSelectionRule> parentSelectionRuleRegistry(
            List<ParentSelectionRule> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<PopulationModel> populationModelRegistry(
            List<PopulationModel> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<StopCondition> stopConditionRegistry(
            List<StopCondition> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<Observer> observerRegistry(
            List<Observer> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<Problem> problemRegistry(
            List<Problem> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<SearchSpace> searchSpaceRegistry(
            List<SearchSpace> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }

    @Bean
    public ComponentRegistry<Generator> mutationRegistry(
            List<Generator> components,
            ApplicationContext context) {
        return new ComponentRegistry<>(components, context);
    }
}
