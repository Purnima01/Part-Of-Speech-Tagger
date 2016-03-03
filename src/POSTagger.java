import java.io.*;
import java.util.*;

/*
 * Part-of-speech tagger using the Viterbi algorithm:
 * finds the most likely part of speech for each of the words/
 * tokens in an English sentence.
 *
 * args[0] : input path to data file
 * args[1] : path to test file
 * args[2] : output file to store word-tag pairs
 *
 * Data format should be similar to that of the
 * Wall Street Journal corpus.
 *
 */
public class POSTagger {
    public static void main(String[] args) throws IOException {
        ReadFile dataRead = new ReadFile();
        RetVal data = dataRead.readInput(args[0]);

        List<List<WordTag>> wordsAndTags = data.getParsedInput();
        Map<String, Integer> stateCounts = data.getStateCounts();
        Set<String> states = stateCounts.keySet();
        int numStates = states.size();

        stateCounts.put("START", wordsAndTags.size());
        stateCounts.put("END", wordsAndTags.size());

        POSTagger posTagger = new POSTagger();

        Map<String, Map<String, Integer>> wordStateCounts =
            posTagger.getWordStateCounts(wordsAndTags);

        Map<Integer, String> idForStates =
            posTagger.initializeIDToStateMapping(states);

        Map<String, Map<String, Integer>> stateTransitionCounts =
                posTagger.getStateTransitionCounts(wordsAndTags);

        List<List<String>> testSentences = dataRead.readTest(args[1]);

        File outFile = new File (args[2]);
        FileWriter fileWriter = new FileWriter(outFile);
        PrintWriter pWriter = new PrintWriter(fileWriter);

        for (List<String> sentence : testSentences) {
            int T = sentence.size();

            int[][] backPtr = posTagger.viterbi(sentence, numStates, T, wordStateCounts,
                    idForStates, stateTransitionCounts, stateCounts);

            int finalStateNumber = numStates + 1;
            posTagger.printFullTagSequenceForSentence(finalStateNumber, T,
                backPtr, idForStates, sentence, pWriter);
            pWriter.println();
        }
        pWriter.close();
    }

    private int[][] viterbi(List<String> sentence, int N, int T,
      Map<String, Map<String, Integer>> wordStateCounts, Map<Integer, String> idForStates,
      Map<String, Map<String, Integer>> stateTransitionCounts, Map<String, Integer> stateCounts) {

        double[][] viterbi = new double[N + 2][T+1];
        int[][] backptr = new int[N + 2][T + 1];

        initializeviterbi(viterbi);

        //init step
        initializationStep(N, viterbi, backptr, idForStates,
            stateTransitionCounts, stateCounts, sentence, wordStateCounts);

        //recursion step
        for (int t = 2; t < T; t ++) {
            String word = sentence.get(t);
            for (int stateID = 1; stateID <= N; stateID ++) {
                String currState = idForStates.get(stateID);

                for (int prvStateId = 1; prvStateId <= N; prvStateId ++) {
                    double V = viterbi[prvStateId][t-1];
                    String prvState = idForStates.get(prvStateId);
                    double A = transitionProb(prvState,
                      currState, stateTransitionCounts, stateCounts);
                    double B = emissionProb(word, currState, wordStateCounts, stateCounts);
                    double pathProbability = V * A * B;

                    if (pathProbability > viterbi[stateID][t]) {
                        backptr[stateID][t] = prvStateId;
                    }
                    viterbi[stateID][t] = Math.max(viterbi[stateID][t], pathProbability);
                }
            }
        }

        //termination step
        terminationStep(N, T, viterbi, backptr, idForStates,
                stateTransitionCounts, stateCounts);

        return backptr;
    }

    private void initializeviterbi(double[][] viterbi) {
        for (int i = 0; i < viterbi.length; i ++) {
            for (int j = 0; j < viterbi[0].length; j ++) {
                viterbi[i][j] = -1;
            }
        }
    }

    private void terminationStep(int N, int T, double[][] viterbi, int[][] backptr,
        Map<Integer, String> idForStates, Map<String, Map<String, Integer>> stateTransitionCounts,
        Map<String, Integer> stateCounts) {

        int finalStateNumber = N + 1;
        String currState = "END";
        for (int prvStateID = 1; prvStateID <= N; prvStateID ++) {
            String prvState = idForStates.get(prvStateID);
            double product = viterbi[prvStateID][T - 1] * transitionProb(prvState,
                    currState, stateTransitionCounts, stateCounts);
            double value = viterbi[finalStateNumber][T];
            if (product >= value) {
                backptr[finalStateNumber][T] = prvStateID;
            }
            viterbi[finalStateNumber][T] = Math.max(viterbi[finalStateNumber][T], product);
        }
    }

    private void initializationStep(int N, double[][] viterbi, int[][] backptr,
        Map<Integer, String> idForStates, Map<String, Map<String, Integer>> stateTransitionCounts,
        Map<String, Integer> stateCounts, List<String> sentence, Map<String,
        Map<String, Integer>> wordStateCounts) {

        for (int stateID = 1; stateID <= N; stateID ++) {
            String currState = idForStates.get(stateID);
            String prvState = "START";

            double A = transitionProb(prvState,
                    currState, stateTransitionCounts, stateCounts);
            String firstWord = sentence.get(1);
            double B = emissionProb(firstWord, currState, wordStateCounts, stateCounts);

            viterbi[stateID][1] = A * B;
            //points to start
            backptr[stateID][1] = 0;
        }
    }

    private void printFullTagSequenceForSentence(int endState, int T, int[][] backPtr,
        Map<Integer, String> idForStates, List<String> testSentence, PrintWriter pWriter) {
        int lastState = backPtr[endState][T];
        printtagseq(lastState, T - 1, backPtr, idForStates, testSentence, pWriter);
    }

    private void printtagseq(int currStateId, int currObs, int[][] backPtr,
        Map<Integer, String> idForStates, List<String> testSentence, PrintWriter pWriter) {
        if (currStateId == 0) {
            return;
        }
        int prvStateId = backPtr[currStateId][currObs];

        printtagseq(prvStateId, currObs - 1, backPtr, idForStates, testSentence, pWriter);
        String currState = idForStates.get(currStateId);
        pWriter.println(testSentence.get(currObs) + "\t" + currState);
    }

    private double transitionProb(String prvState, String currState,
      Map<String, Map<String, Integer>> stateTransitionCounts, Map<String, Integer> stateCounts) {

        Map<String, Integer> mapOfTransitionCountsFromAllPrvStates =
            stateTransitionCounts.get(currState);

        int countOfTransitionFromRequiredPrvState =
            (mapOfTransitionCountsFromAllPrvStates.containsKey(prvState) ?
            mapOfTransitionCountsFromAllPrvStates.get(prvState) : 0);
        int totalCountOfPrvStateAppearances = stateCounts.get(prvState);

        double transitionProbabilityFromPrvStateToCurrState =
                (double) countOfTransitionFromRequiredPrvState/totalCountOfPrvStateAppearances;

        return transitionProbabilityFromPrvStateToCurrState;
    }

    private double emissionProb(String word, String currState,
      Map<String, Map<String, Integer>> wordStateCounts, Map<String, Integer> stateCounts) {
        //set uniform probability for unseen words in test data
        double probabilityUnseenWord = 0.5;

        if (!wordStateCounts.containsKey(word)) {
            return probabilityUnseenWord;
        }

        Map<String, Integer> stateCountsForWord = wordStateCounts.get(word);

        int countOfWordInThisState =
            stateCountsForWord.containsKey(currState) ? stateCountsForWord.get(currState) : 0;
        int totalCountOfStateAppearances = stateCounts.get(currState);

        double emissionProbabilityOfWordGivenState =
                (double) countOfWordInThisState/totalCountOfStateAppearances;

        return emissionProbabilityOfWordGivenState;
    }

    private Map<String, Map<String, Integer>> getStateTransitionCounts(
       List<List<WordTag>> wordsAndTags) {

        Map<String, Map<String, Integer>> transitionCountsMap =
                new HashMap<String, Map<String, Integer>>();

        populateStartToFirstWordTransitionCounts(transitionCountsMap, wordsAndTags);
        populateTransitionCountsForWordsInSentence(transitionCountsMap, wordsAndTags);
        populateLastWordToEndTransitionCounts(transitionCountsMap, wordsAndTags);

        return transitionCountsMap;
    }

    private void populateLastWordToEndTransitionCounts(
       Map<String, Map<String, Integer>> transitionCountsMap, List<List<WordTag>> wordsAndTags) {

        for (List<WordTag> sentence : wordsAndTags) {
            WordTag lastWordTag = sentence.get(sentence.size() - 1);
            String currTag = "END";
            String prvTag = lastWordTag.getTag();

            getStateTransitionCountsHelp(prvTag, currTag, transitionCountsMap);
        }
    }

    private void populateTransitionCountsForWordsInSentence(
       Map<String, Map<String, Integer>> transitionCountsMap, List<List<WordTag>> wordsAndTags) {

        for (List<WordTag> sentence : wordsAndTags) {
            //start from the second word-tag pair
            for (int i = 1; i < sentence.size(); i ++) {
                String prvTag = sentence.get(i - 1).getTag();
                String currTag = sentence.get(i).getTag();

                getStateTransitionCountsHelp(prvTag, currTag, transitionCountsMap);
            }
        }
    }

    private void populateStartToFirstWordTransitionCounts(
            Map<String, Map<String, Integer>> transitionCountsMap,
            List<List<WordTag>> wordsAndTags) {
        //a. for start->first word transitions
        String prvTag = "START";
        for (List<WordTag> sentence : wordsAndTags) {
            WordTag firstWordTag = sentence.get(0);
            String currTag = firstWordTag.getTag();

            getStateTransitionCountsHelp(prvTag, currTag, transitionCountsMap);
        }
    }

    private void getStateTransitionCountsHelp(String prvState, String currState,
        Map<String, Map<String, Integer>> transitionCountsMap) {

        //Inner level map (value of String in <String, Map<String1, Int>>)
        Map<String, Integer> transitionFromPrvStateCount;

        if (!transitionCountsMap.containsKey(currState)) {
            transitionFromPrvStateCount = new HashMap<String, Integer>();
            transitionFromPrvStateCount.put(prvState, 1);
        } else {
            transitionFromPrvStateCount = transitionCountsMap.get(currState);

            if (!transitionFromPrvStateCount.containsKey(prvState)) {
                transitionFromPrvStateCount.put(prvState, 1);
            } else {
                int countTransitions = transitionFromPrvStateCount.get(prvState);
                transitionFromPrvStateCount.put(prvState, countTransitions + 1);
            }
        }
        transitionCountsMap.put(currState, transitionFromPrvStateCount);
    }

    private Map<String, Map<String, Integer>> getWordStateCounts(
       List<List<WordTag>> wordsAndTags) {

        Map<String, Map<String, Integer>> wordStateCount =
                new HashMap<String, Map<String, Integer>>();

        for (List<WordTag> sentence : wordsAndTags) {
            for (WordTag singleWordTag : sentence) {
                getWordStateCountsHelp(singleWordTag, wordStateCount);
            }
        }

        return wordStateCount;
    }

    private void getWordStateCountsHelp(
       WordTag singleWordTag, Map<String, Map<String,Integer>> wordStateCount) {

        String word = singleWordTag.getWord();
        String state = singleWordTag.getTag();
        Map<String, Integer> stateCountMap;

        if (!wordStateCount.containsKey(word)) {
            stateCountMap = new HashMap<String, Integer>();
            stateCountMap.put(state, 1);
        } else {
            stateCountMap = wordStateCount.get(word);
            if (!stateCountMap.containsKey(state)) {
                stateCountMap.put(state, 1);
            } else {
                int stateCountForWord = stateCountMap.get(state);
                stateCountMap.put(state, stateCountForWord + 1);
            }
        }
        wordStateCount.put(word, stateCountMap);
    }

    private Map<Integer, String> initializeIDToStateMapping(Set<String> states) {
        int id = 1;
        Map<Integer, String> idMap = new HashMap<Integer, String>();

        idMap.put(0, "START");
        for (String state : states) {
            if (state.equals("START") || state.equals("END")) {
                continue;
            }
            idMap.put(id, state);
            id ++;
        }
        idMap.put(id, "END");

        return idMap;
    }
}