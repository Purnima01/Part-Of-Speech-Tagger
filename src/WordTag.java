/**
 * Tag and state used interchangeably throughout
 */
public class WordTag {
    private String word;
    private String tag;
    public WordTag(String word, String tag) {
        this.word = word;
        this.tag = tag;
    }
    public String getWord() {
        return word;
    }
    public String getTag() {
        return tag;
    }
}
