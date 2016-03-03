import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Read train and test data
 */
public class ReadFile {
    public RetVal readInput(String filePath) throws FileNotFoundException {

        File file = new File(filePath);
        Scanner scanner = new Scanner(file);

        List<List<WordTag>> parsedInput = new ArrayList<List<WordTag>>();      //step 6

        List<WordTag> sentence = new ArrayList<WordTag>();

        Map<String, Integer> stateCounts = new HashMap<String, Integer>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            //end of sentence
            if (line.length() == 0) {
                parsedInput.add(sentence);
                sentence = new ArrayList<WordTag>();
            } else {
                //line is part of current sentence, parse into word-tag format and add to sentence
                String[] lineSplit = line.split("\\t");
                WordTag wordAndTag = new WordTag(lineSplit[0], lineSplit[1]);
                int updatedCount = (stateCounts.containsKey(lineSplit[1])) ?
                        stateCounts.get(lineSplit[1]) : 0;
                stateCounts.put(lineSplit[1], updatedCount + 1);   //step 2
                sentence.add(wordAndTag);
            }
        }

        return new RetVal(parsedInput, stateCounts);
    }

    public List<List<String>> readTest(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        Scanner scanner = new Scanner(file);

        List<List<String>> parsedTest = new ArrayList<List<String>>();
        List<String> sentence = new ArrayList<String>();
        sentence.add(""); //0th index element is bogus

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            //end of sentence
            if (line.length() == 0) {
                parsedTest.add(sentence);
                sentence = new ArrayList<String>();
                sentence.add(""); //0th index element is bogus
                continue;
            }
            sentence.add(line);
        }
        return parsedTest;
    }
}