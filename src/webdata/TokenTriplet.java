package webdata;

import java.util.Objects;

/**
 * Created by eranhashmo on 5/12/2021.
 */
public class TokenTriplet implements Comparable<TokenTriplet>{
    private String term;
    private int reviewID;
    private short frequency;

    public TokenTriplet( String term, int reviewID, short frequency) {
        this.reviewID = reviewID;
        this.frequency = frequency;
        this.term = term;
    }

    public int getReviewID() {
        return reviewID;
    }

    public short getFrequency() {
        return frequency;
    }

    public String getTerm() {
        return term;
    }

    public boolean equals(TokenTriplet other) {
        if (this == other) {
            return true;
        }
        return (this.reviewID == other.getReviewID() &&
                this.frequency == other.getFrequency() &&
                Objects.equals(this.term, other.getTerm()));
    }

    public int compareTo(TokenTriplet other) {
        if (!Objects.equals(this.term, other.getTerm())) {
            return this.term.compareTo(other.term);
        }
        return Integer.compare(this.reviewID, other.getReviewID());
    }

    public String toString() {
        return "(" + term + ", " + reviewID + ", " + frequency + ")";
    }
}
