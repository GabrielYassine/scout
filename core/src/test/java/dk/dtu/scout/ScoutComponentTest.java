package dk.dtu.scout;

import dk.dtu.scout.dto.Parameter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoutComponentTest {

    @Test
    void defaultMethodsReturnEmptyValuesAndDoNothing() {
        ScoutComponent component = new MinimalComponent();
        State state = new State();

        component.init(state);
        component.configure(Map.of("ignored", 1));

        assertTrue(component.supportedSearchSpaces().isEmpty());
        assertTrue(component.getStateVariables(state).isEmpty());
    }

    private static class MinimalComponent implements ScoutComponent {
        @Override
        public String id() {
            return "minimal";
        }

        @Override
        public String displayName() {
            return "Minimal";
        }

        @Override
        public String description() {
            return "Minimal component";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }
}