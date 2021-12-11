import webdata.*;

import java.util.*;

public class Main {
    static final String INDEX_DIR = "indexDirectory";
    public void testing(String filename) {

//        String term = "watermelon";
        String term = "better";
        int reviewID = 1;
        Enumeration e;
//
        IndexWriter iw = new IndexWriter();
        iw.write(filename, INDEX_DIR);

        IndexReader indexReader = new IndexReader(INDEX_DIR);
        System.out.println("getProductId: " + reviewID);
        System.out.println(indexReader.getProductId(reviewID));
//////        -------------------------------------------------
//
        System.out.println("---------------------------");
        System.out.println("getScore: " + reviewID);
        System.out.println(indexReader.getReviewScore(reviewID));
        System.out.println("getScore: " + 1000);
        System.out.println(indexReader.getReviewScore(1000));
//
        System.out.println("---------------------------");
        System.out.println("getReviewHelpfulnessNumerator: " + reviewID);
        System.out.println(indexReader.getReviewHelpfulnessNumerator(reviewID));
//        System.out.println(indexReader.getReviewHelpfulnessNumerator(2));

        System.out.println("---------------------------");
        System.out.println("getReviewHelpfulnessDenominator: " + reviewID);
        System.out.println(indexReader.getReviewHelpfulnessDenominator(reviewID));
//        System.out.println(indexReader.getReviewHelpfulnessDenominator(2));
//
        System.out.println("---------------------------");
        System.out.println("getReviewLength: " + reviewID);
        System.out.println(indexReader.getReviewLength(reviewID));

        System.out.println("------------ Token queries ---------------");
        System.out.println("getTokenFrequency: " + term);
        System.out.println(indexReader.getTokenFrequency(term));
        System.out.println("getTokenCollectionFrequency: " + term);
        System.out.println(indexReader.getTokenCollectionFrequency(term));
        System.out.println("---------------------------");
        System.out.println("getReviewsWithToken: " + term); // problem
        e = indexReader.getReviewsWithToken("food");
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
//
        System.out.println("---------------------------");
        System.out.println("getNumberOfReviews");
        System.out.println(indexReader.getNumberOfReviews());
        System.out.println("---------------------------");
        System.out.println("getTokenSizeOfReviews");
        System.out.println(indexReader.getTokenSizeOfReviews());
        System.out.println("---------------------------");
        System.out.println("getProductReviews: ");
        e = indexReader.getProductReviews("B006K2ZZ7K");
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
//
        iw.removeIndex(INDEX_DIR);
    }

    public static HashMap<Integer, Integer> histogram(Enumeration<Integer> numbers) {
        HashMap<Integer, Integer> hist = new HashMap<>();
        while (numbers.hasMoreElements()) {
            int n = numbers.nextElement();
            if (hist.containsKey(n)) {
                hist.replace(n, hist.get(n) + 1);
            } else {
                hist.put(n, 1);
            }
        }
        return hist;
    }


    public static void main(String[] args) {

        Main sb = new Main();
        String filename;
//        filename = "/cs/+/course/webdata/movies.txt";
//        filename = "/cs/usr/eranhashmo/temp/data_place/finefoods.txt.gz";
        filename = "1000.txt";

//        System.out.println((double)3/2);
//        sb.testing(filename);

        IndexWriter iw = new IndexWriter();
        iw.removeIndex(INDEX_DIR);

        int k = 3;
        double lambda = 0.5;

        String query = "love candy";
//        String query = "a b c c";
        ArrayList<String> qlist = new ArrayList(Arrays.asList(query.toLowerCase().split("\\W")));
        Enumeration<Integer> e;
        Collection<String> es;

        long startTime = System.nanoTime();
        iw.write(filename, INDEX_DIR);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        System.out.println("construction time: " + (timeElapsed / (1000 * 1000 * 1000)));
        startTime = System.nanoTime();
        IndexReader ir = new IndexReader(INDEX_DIR);
        ReviewSearch rs = new webdata.ReviewSearch(ir);
        e = rs.vectorSpaceSearch(Collections.enumeration(qlist), k);
        System.out.println("vectorSpaceSearch");
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
        System.out.println("languageModelSearch");
        e = rs.languageModelSearch(Collections.enumeration(qlist), lambda, k);
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }


        //B001RVFDOO    chips
//        horror B004391DK0     biscuits
        // B00139TT72   dog food

//        B000G6RYNE
//                B00622CYVS
//        B001EQ5EJQ
//                B000RHXIGO
//        B003YXWAF8
//                B000GJQ8J2
        System.out.println("productSearch");
        es = rs.productSearch(Collections.enumeration(qlist), k);
        es.forEach(System.out::println);
//        e = ir.getProductReviews(es.iterator().next());
//        ArrayList<Integer> productScores = new ArrayList<>();
//        while (e.hasMoreElements()) {
//            productScores.add(ir.getReviewScore(e.nextElement()));
//        }
//        System.out.println(histogram(Collections.enumeration(productScores)));
//
//        endTime = System.nanoTime();
//        timeElapsed = endTime - startTime;
//        System.out.println("query time: " + (timeElapsed / (1000 * 1000 * 1000)));
//        iw.removeIndex(INDEX_DIR);
//        sb.testing(filename);


//        try {
//            BufferedReader reader = new BufferedReader(new FileReader("intermediate_token_file2"));
//            String q = reader.readLine();
//            ArrayList<String> queries = new ArrayList<>();
//            while (q != null) {
//                queries.add(q);
//                q = reader.readLine();
//            }
//            IndexReader ir = new IndexReader(INDEX_DIR);
//
//            startTime = System.nanoTime();
//            for (String query: queries) {
////                System.out.println(ir.getReviewsWithToken(query));
//                ir.getReviewsWithToken(query);
//            }
//            endTime = System.nanoTime();
//            timeElapsed = endTime - startTime;
//            System.out.println("getReviewsWithToken time: " + (timeElapsed / (1000 * 1000)));
//
//            startTime = System.nanoTime();
//            for (String query: queries) {
////                System.out.println(ir.getTokenFrequency(query));
//                ir.getTokenFrequency(query);
//            }
//            endTime = System.nanoTime();
//            timeElapsed = endTime - startTime;
//            System.out.println("getTokenFrequency time: " + (timeElapsed / (1000 * 1000)));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
}
