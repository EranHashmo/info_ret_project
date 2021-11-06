package webdata;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;


public class ReviewSearch {
    IndexReader ir;
    /**
     * Constructor
     */
    public ReviewSearch(IndexReader iReader) {
        ir = iReader;
    }
    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the vector space ranking function lnn.ltc (using the
     * SMART notation)
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> vectorSpaceSearch(Enumeration<String> query, int k) {
        int numContainingReviews;
        double q_tf;
        double q_idf;
        double qVectorNorm = 0;
        int corpus_size = ir.getNumberOfReviews();
        HashMap<String, Double> qVector = histogram(query);
        HashMap<Integer, Double> reviewScores = new HashMap<>();

        // Query
        for (Map.Entry<String, Double> entry : qVector.entrySet()) {
            q_tf = 1 + log10(entry.getValue());
            numContainingReviews = ir.getTokenFrequency(entry.getKey());      // expensive
            q_idf = 0;
            if (numContainingReviews > 0) {
                q_idf = log10((double) corpus_size / numContainingReviews);
            }
            qVector.replace(entry.getKey(), q_tf * q_idf);
            qVectorNorm += pow(qVector.get(entry.getKey()),2);
        }
        qVectorNorm = sqrt(qVectorNorm);
        for (Map.Entry<String, Double> entry : qVector.entrySet()) {
            qVector.replace(entry.getKey(), entry.getValue() / qVectorNorm);
        }

        // Reviews
        for (Map.Entry<String, Double> entry : qVector.entrySet()) {
            Enumeration<Integer> containingReviews_E = ir.getReviewsWithToken(entry.getKey());
            while (containingReviews_E.hasMoreElements()) {
                int reviewID = containingReviews_E.nextElement();
                int termFrequency = containingReviews_E.nextElement();
                double r_tf = 1 + log10(termFrequency);
                if (reviewScores.containsKey(reviewID)) {
                    reviewScores.replace(reviewID, reviewScores.get(reviewID) + r_tf * entry.getValue());
                }
                else {
                    reviewScores.put(reviewID, r_tf * qVector.get(entry.getKey()));
                }
            }
        }

        List answer = Arrays.asList(reviewScores
                .entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<Integer, Double>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(k)
                .map(Map.Entry::getKey).toArray());
        return Collections.enumeration(answer);
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the language model ranking function, smoothed using a
     * mixture model with the given value of lambda
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> languageModelSearch(Enumeration<String> query,
                                                    double lambda, int k) {
        HashMap<Integer, Double> reviewScores = new HashMap<>();
        HashMap<String, Double> termModels = new HashMap<>();
        HashMap<Integer, HashMap<String, Integer>> reviews = new HashMap<>();
        int cft;
        int totalTokenNum = ir.getTokenSizeOfReviews();
        int termFrequency;
        double probTermReview;
        String term;
        while (query.hasMoreElements()) {
            term = query.nextElement();
            cft = ir.getTokenCollectionFrequency(term);
            termModels.put(term, (1 - lambda) * cft / totalTokenNum);
            Enumeration<Integer> containingReviews_E = ir.getReviewsWithToken(term);
            while ((containingReviews_E.hasMoreElements())) {
                int reviewID = containingReviews_E.nextElement();
                termFrequency = containingReviews_E.nextElement();
                if (reviews.containsKey(reviewID)) {
                    reviews.get(reviewID).put(term, termFrequency);
                }
                else {
                    HashMap<String, Integer> reviewMap = new HashMap<>();
                    reviewMap.put(term, termFrequency);
                    reviews.put(reviewID, reviewMap);
                }
            }
        }
        for (Map.Entry<Integer, HashMap<String, Integer>> r: reviews.entrySet()) {
            reviewScores.put(r.getKey(), (double)1);
            for (Map.Entry<String, Double> termModel: termModels.entrySet()) {
                int reviewSize = ir.getReviewLength(r.getKey());
                termFrequency = r.getValue().getOrDefault(termModel.getKey(), 0);
                probTermReview = (lambda * termFrequency / reviewSize) + termModel.getValue();
                reviewScores.replace(r.getKey(), reviewScores.get(r.getKey()) * probTermReview);
            }
        }
        List answer = Arrays.asList(reviewScores
                .entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<Integer, Double>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(k)
                .map(Map.Entry::getKey).toArray());
        return Collections.enumeration(answer);
    }
    /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */
    public Collection<String> productSearch(Enumeration<String> query, int k) {
        HashMap<String, Double> productScores = new HashMap<>();
        HashSet<String> productCandidates = new HashSet<>();
        Enumeration<Integer> reviews;

        int total_reviews = ir.getNumberOfReviews();
        // decides balance between quality and quantity. bigger factor means larger weight to average scores
        //  over number of reviews
        double weightFactor = (double) 10 / total_reviews;
        double productRank;
        int reviewScore;
        int reviewHelpNominator;
        int reviewHelpDenominator;
        double helpfulnessFactor;
        // filter reviews with low helpfulness ratio
        double helpfulnessThreshold = 0.3;

        int kTag = 2*k;
        int maxReviewsToLook = max(total_reviews/20, 100);
        List<String> queryList = Collections.list(query);
        while (productCandidates.size() < k & kTag < maxReviewsToLook) {
            reviews = vectorSpaceSearch(Collections.enumeration(queryList), kTag);
            while (reviews.hasMoreElements()) {
                productCandidates.add(ir.getProductId(reviews.nextElement()));
            }
            kTag *= 2;
        }

        for (String pID: productCandidates) {
            productRank = 0;
            List<Integer> pReviews = Collections.list(ir.getProductReviews(pID));
            for (int curReview: pReviews) {
                reviewScore = ir.getReviewScore(curReview);
                reviewHelpNominator = ir.getReviewHelpfulnessNumerator(curReview);
                reviewHelpDenominator = ir.getReviewHelpfulnessDenominator(curReview);
                if (reviewHelpDenominator == 0 | reviewHelpNominator == 0) {
                    helpfulnessFactor = 0;
                }
                else {
                    helpfulnessFactor = (double) reviewHelpNominator / reviewHelpDenominator;
                }
                if (helpfulnessFactor > helpfulnessThreshold) {
                    productRank += reviewScore;
                }
            }
            productRank = weightFactor*(productRank / pReviews.size()) +
                    ((double) pReviews.size() / total_reviews);
            productScores.put(pID, productRank);
        }

        return productScores
                .entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, Double>::getValue).reversed())
                .limit(k)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private HashMap<String, Double> histogram(Enumeration<String> terms) {
        HashMap<String, Double> hist = new HashMap<>();
        while (terms.hasMoreElements()) {
            String q_term = terms.nextElement();
            if (hist.containsKey(q_term)) {
                hist.replace(q_term, hist.get(q_term) + 1);
            } else {
                hist.put(q_term, (double) 1);
            }
        }
        return hist;
    }
}
