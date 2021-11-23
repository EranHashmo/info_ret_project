package webdata;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IndexReader {
    private String workDir;
    private Dictionary dictionary;
    private Parser parser;
    /**
     * Creates an IndexReader which will read from the given directory
     */
    public IndexReader(String dir) {
        workDir = dir;
        dictionary = new Dictionary(dir);

        parser = new Parser(workDir + File.separator + IndexWriter.REVIEW_DATA_FILE);
    }

    /**
     * Read review data from disk according to reviewID
     * @param reviewId: review to read
     * @return Review object
     */
    private Review readReview(int reviewId) {
        Review review = null;
        if (reviewId > dictionary.getNumReviews()) {
            System.out.println("Invalid reviewID");
            return null;
        }
        try {
//            long reviewPtr = dictionary.getReviewPointer(reviewId, workDir);
//            if (reviewPtr < 0) {
//                return null;
//            }
            long reviewPtr = (2*Integer.BYTES) + (reviewId - 1) * (RawDataParser.PID_LENGTH + 4*Integer.BYTES);
            review = parser.readReview(reviewPtr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return review;
    }

    /**
     * Find and returns a token from the dictionary according to the term pointed at by the token.
     * @param term: string the token corresponds to.
     * @return Token object
     */
    private Token getTokenFromDict(String term) {
        Token token = null;
        try {
            token = dictionary.getToken(term);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

    /**
     * Returns the product identifier for the given review
     * Returns null if there is no review with the given identifier
     */
    public String getProductId(int reviewId) {
        Review review = readReview(reviewId);
        if (review == null) {
            return null;
        }
        return review.getProductID();

    }

    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewScore(int reviewId) {
        Review review = readReview(reviewId);
        if (review == null) {
            return -1;
        }
        return review.getScore();
    }

    /**
     * Returns the numerator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessNumerator(int reviewId) {
        Review review = readReview(reviewId);
        if (review == null) {
            return -1;
        }
        return review.getHelpfulness1();
    }

    /**
     * Returns the denominator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessDenominator(int reviewId) {
        Review review = readReview(reviewId);
        if (review == null) {
            return -1;
        }
        return review.getHelpfulness2();
    }

    /**
     * Returns the number of tokens in a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewLength(int reviewId) {
        Review review = readReview(reviewId);
        if (review == null) {
            return -1;
        }
        return review.getNumOfTokens();
    }

    /**
     * Return the number of reviews containing a given token (i.e., word)
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenFrequency(String token) {
        int counter = 1;
        try {
//            Token foundToken = dictionary.getToken(token);
//            if (foundToken == null) {
//                return 0;
//            }
//            ArrayList<Integer> containingReviews =
//                    dictionary.getContainingReviews(foundToken, workDir).
//                            get(Dictionary.CONTAINED_LIST);
//            if (containingReviews == null) {
//                return 0;
//            }
//            return containingReviews.size();
            ArrayList<Integer> reviewIDs = dictionary.getContainingReviews(token, workDir);
            if (reviewIDs == null) {
                return 0;
            }
            int curReviewID = reviewIDs.get(0);

            for (Integer t: reviewIDs) {
                if (t != curReviewID) {
                    counter++;
                    curReviewID = t;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return counter;
    }

    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
//        int counter = 0;
        ArrayList<Integer> containingReviews = new ArrayList<>();
        try {
//            Token foundToken = dictionary.getToken(token);
//            if (foundToken == null) {
//                return 0;
//            }
            containingReviews = dictionary.getContainingReviews(token, workDir);
            if (containingReviews == null) {
                return 0;
            }
//            ArrayList<Integer> containingReviewsFreq =
//                    dictionary.getContainingReviews(foundToken, workDir).
//                            get(Dictionary.CONTAINED_FREQ);
//            if (containingReviewsFreq == null) {
//                return 0;
//            }
//            for (int f : containingReviewsFreq) {
//                counter += f;
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return containingReviews.size();

    }

    /**
     * Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
     * that id-n is the n-th review containing the given token and freq-n is the
     * number of times that the token appears in review id-n
     * Only return ids of reviews that include the token
     * Note that the integers should be sorted by id
     *
     * Returns an empty Enumeration if there are no reviews containing this token
     */
    public Enumeration<Integer> getReviewsWithToken(String token) {
        ArrayList<Integer> reviewsWithTokens = new ArrayList<>();
//        Token foundToken = getTokenFromDict(token);
//        if (foundToken == null) {
//            return Collections.enumeration(reviewsWithTokens);
//        }
        try {
            ArrayList<Integer> reviewIDs = dictionary.getContainingReviews(token, workDir);
            if (reviewIDs == null) {
                return Collections.enumeration(reviewsWithTokens);
            }

            int curReviewID = reviewIDs.get(0);
            int curFrequency = 0;

            reviewsWithTokens.add(curReviewID);
            for (Integer t: reviewIDs) {
                if (t == curReviewID) {
                    curFrequency++;
                }
                else {
                    reviewsWithTokens.add(curFrequency);
                    // add new review
                    curReviewID = t;
                    reviewsWithTokens.add(t);
                    curFrequency = 1;
                }
            }
            reviewsWithTokens.add(curFrequency);
//            ArrayList<ArrayList<Integer>> reviewList =
//                    dictionary.getContainingReviews(foundToken, workDir);
//            ArrayList<Integer> containingReviews = reviewList.get(Dictionary.CONTAINED_LIST);
//            ArrayList<Integer> frequencyList = reviewList.get(Dictionary.CONTAINED_FREQ);
//            if (containingReviews != null && frequencyList != null) {
//                for (int i = 0; i < containingReviews.size(); i++) {
//                    reviewsWithTokens.add(containingReviews.get(i));
//                    reviewsWithTokens.add(frequencyList.get(i));
//                }
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.enumeration(reviewsWithTokens);
    }

    /**
     * Return the number of product reviews available in the system
     */
    public int getNumberOfReviews() {
        return dictionary.getNumReviews();
    }

    /**
     * Return the number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        return dictionary.getNumTokens();
    }

    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     *
     * Returns an empty Enumeration if there are no reviews for this product
     */
    public Enumeration<Integer> getProductReviews(String productId) {
        ArrayList<Integer> resultIds = new ArrayList<>();
        ArrayList<String> allIDS = new ArrayList<>();
        try {
            allIDS = dictionary.getReviewMapPids(workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int r = 0; r < allIDS.size(); r++) {
            if (Objects.equals(allIDS.get(r), productId)) {
                resultIds.add(r + 1);
            }
        }
        return Collections.enumeration(resultIds);
    }
}