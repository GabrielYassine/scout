package dk.dtu.scout.logging;
import java.util.ArrayList;
import java.util.List;

public class LoggedSeries<T> {
    private final SeriesMode mode;
    private final List<T> values = new ArrayList<>();

    public LoggedSeries(SeriesMode mode) {
        this.mode = mode;
    }

    public void add(T value) {
        if (mode == SeriesMode.LATEST_ONLY) {
            values.clear();
        }
        values.add(value);
    }

    public SeriesMode getMode() {
        return mode;
    }

    public List<T> getValues() {
        return values;
    }
}