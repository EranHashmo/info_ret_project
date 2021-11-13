package webdata;

import java.util.Objects;

public class TokenCouple implements Comparable<TokenCouple>{
    private String term;
    private int reviewID;

    public TokenCouple( String term, int reviewID) {
        this.reviewID = reviewID;
        this.term = term;
    }

    public int getReviewID() {
        return reviewID;
    }


    public String getTerm() {
        return term;
    }

    public boolean equals(TokenCouple other) {
        if (this == other) {
            return true;
        }
        return (this.reviewID == other.getReviewID() &&
                Objects.equals(this.term, other.getTerm()));
    }

    public int compareTo(TokenCouple other) {
        if (!Objects.equals(this.term, other.getTerm())) {
            return this.term.compareTo(other.term);
        }
        return Integer.compare(this.reviewID, other.getReviewID());
    }

    public String toString() {
        return "(" + term + ", " + reviewID + ")";
    }
}
