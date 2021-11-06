package webdata;

/**
 * Created by eranhashmo on 4/21/2021.
 */

/**
 * Class that encapsulates an entry in the term dictionary.
 * holds:
 *  the frequency of the term's appearance in the index,
 *  a pointer to where it is held in the dictionary
 *  and a list of review id's where the term appears.
 */
public class Token {
    private int termPointer;
    private long listPointer;

    Token() {
    }

    public int getTermPointer() {
        return termPointer;
    }

    /**
     * When the dictionary is compressed, replace the term String held by the token
     *  with a pointer to the string in the compressed dictionary.
     * @param termPointer: pointer to the term in the dictionary's concatenated string.
     */
    public void setTermPointer(int termPointer) {
        this.termPointer = termPointer;
    }

    public long getListPointer() {
        return listPointer;
    }

    public void setListPointer(long listPointer) {
        this.listPointer = listPointer;
    }
}