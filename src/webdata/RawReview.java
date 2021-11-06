package webdata;

/**
 * Created by eranhashmo on 4/10/2021.
 */
public class RawReview {
    private String productID;
    private int helpfulness1;
    private int helpfulness2;
    private int score;
    private String text;
    /**
     * Data structure that holds all relevant fields of a review.
     * @param pId
     * @param help1: Helpfulness Numerator
     * @param help2: HelpfulnessDenominator
     * @param score
     * @param text
     */
    public RawReview(String pId, int help1, int help2, int score, String text) {
        this.productID = pId;
        this.helpfulness1 = help1;
        this.helpfulness2 = help2;
        this.score = score;
        this.text = text;
    }

    public String getProductID() {
        return productID;
    }

    public int getHelpfulness1() {
        return helpfulness1;
    }

    public int getHelpfulness2() {
        return helpfulness2;
    }

    public int getScore() {
        return score;
    }

    public String getText() {
        return text;
    }


}
