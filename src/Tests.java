import webdata.IndexReader;
import webdata.IndexWriter;
import webdata.Parser;
import webdata.Review;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;

public class Tests {
    public static String indexDir = "indexDir";
//    public static String inputFile = "1000.txt";
    public static String inputFile = "movies.txt.gz";

    static final String OUTPUT = "sandboxOut.txt";
    static final int TINY_DATA_SIZE = 6;

    public static Map<Integer, Review> testReviews;
    public static Map<String, int[]> testTokens;

    public static void textTokensNum(String text)
    {
        int count = 0;
        List<String> terms = Arrays.asList(text.toLowerCase().split("\\W"));
        for (String t : terms) {
            if (t.length() > 0) {
                count++;
            }
        }
        System.out.println(count);
    }

    public static void buildIndex() throws OutOfMemoryError
    {
        long start = 0, end = 0;
        IndexWriter iw = new IndexWriter();
        start = currentTimeMillis();
        iw.write(inputFile, indexDir);
        end = currentTimeMillis();
        System.out.println("build time: " + (end - start));
    }

    public static void removeIndex()
    {
        IndexWriter iw = new IndexWriter();
        iw.removeIndex(indexDir);
    }

    public static void readTest()
    {
//        long start = 0, end = 0;
        IndexReader ir = new IndexReader(indexDir);

        testReviews = new HashMap<>();
        testReviews.put(1, new Review("B001E4KFG0", 1, 1, 5, 48));
        testReviews.put(500, new Review("B000G6RYNE", 0, 0, 5, 73));
        testReviews.put(1000, new Review("B006F2NYI2", 2, 5, 2, 102));
//        testReviews.put(2, new Review("B00813GRG4", 0, 0, 1, 32));
//        testReviews.put(999, new Review("B006F2NYI2", 1, 2, 1, 57));

        System.out.println("num of reviews: " + ir.getNumberOfReviews());
//        for (int reviewID : testReviews.keySet()) {
//            System.out.println("-------- review " + reviewID + "------------");
//            System.out.println(
//                    "product id = " + ir.getProductId(reviewID)
//                            + " || should be: " + testReviews.get(reviewID).getProductID());
//            System.out.println(
//                    "score of review = " + ir.getReviewScore(reviewID)
//                            + " || should be: " + testReviews.get(reviewID).getScore());
//            System.out.println(
//                    "helpfulness1 = " + ir.getReviewHelpfulnessNumerator(reviewID)
//                            + " || should be: " + testReviews.get(reviewID).getHelpfulness1());
//            System.out.println(
//                    "helpfulness2 = " + ir.getReviewHelpfulnessDenominator(reviewID)
//                            + " || should be: " + testReviews.get(reviewID).getHelpfulness2());
//            System.out.println(
//                    "num of tokens = " + ir.getReviewLength(reviewID)
//                            + " || should be: " + testReviews.get(reviewID).getNumOfTokens());
//            System.out.println("-------- review " + reviewID + " end ------------");
//        }

        testTokens = new HashMap<>();
        testTokens.put("it", new int[]{591, 1504});
//        testTokens.put("allergy", new int[]{5, 6});
//        testTokens.put("chicken", new int[]{23, 30});
//        testTokens.put("of", new int[]{589, 1335});

        System.out.println("num of tokens: " + ir.getTokenSizeOfReviews());
        for (String token: testTokens.keySet()) {
            System.out.println("------ token " + token + "-----");
            System.out.println("getTokenFrequency: " + ir.getTokenFrequency(token) ); //+ " || should be: " + testTokens.get(token)[0]);
            System.out.println("getTokenCollectionFrequency " + ir.getTokenCollectionFrequency(token) ); //+ " || should be: " + testTokens.get(token)[1]);

            long start = currentTimeMillis();
            ir.getReviewsWithToken(token);
            long end = currentTimeMillis();
            System.out.println("getReviewsWithToken: " + token + ", time: " + (end - start));

//            getReviewsWithToken(ir, token);
            System.out.println("------ token " + token + " end -----");
        }
    }

    private static void getReviewsWithToken(IndexReader ir, String token) {
        Enumeration e;
        System.out.println("getReviewsWithToken: " + token); // problem
        e = ir.getReviewsWithToken(token);
        File termResults = new File("getReviewsWithToken_TEST_" + token);
        try {
            FileWriter writer = new FileWriter(termResults);
            while (e.hasMoreElements()) {
//                System.out.println(e.nextElement());
                writer.write(e.nextElement().toString() + "\n");
            }
            writer.close();
        } catch (IOException ex) {
            System.out.println("failed to write termResults: " + token);
            ex.printStackTrace();
        }
    }

    private void parserReadTest() {
        Review r;

        try {
            Parser p = new Parser(OUTPUT);
            for (int i = 0; i < TINY_DATA_SIZE; i++) {
                r = p.readReview(i);
                System.out.println(r);
            }
//            p.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        removeIndex();
        try {
            buildIndex();
            readTest();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
    }
}
