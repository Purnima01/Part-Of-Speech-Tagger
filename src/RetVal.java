import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by purnima on 2/27/16.
 */
public class RetVal {
    private List<List<WordTag>> parsedInput;
    private Map<String, Integer> stateCounts;

    RetVal(List<List<WordTag>> parsedInput, Map<String, Integer> uniqueStates) {
        this.parsedInput = parsedInput;
        this.stateCounts = uniqueStates;
    }

    public Map<String, Integer> getStateCounts() {
        return stateCounts;
    }
    public List<List<WordTag>> getParsedInput() {
        return parsedInput;
    }
}
