package webdata;

import java.io.*;
import java.util.*;

import static java.lang.System.currentTimeMillis;
//import matplotlib.pyplot as plt

/**
 * Created by eranhashmo on 5/6/2021.
 */
public class IndexWriter {
    private final int REVIEWS_TO_READ =  1 * 1000 * 1000;

    private final int MAX_REVIEWS_IN_MEM = 3 *1000*1000;
    private final int MAX_TRIPLETS_IN_MEM = 27 * 1000 * 1000;
//    private final int MAX_TRIPLETS_IN_MEM = 1000;


    public static final String REVIEW_DATA_FILE = "review_index";
    public static final String TERM_FILE = "term_String_File";
//    public static final String DICT_REVIEW_PTRS = "dict_review_ptrs";
    public static final String DICT_REVIEW_PIDS = "dict_review_PIDs";
    public static final String TOKEN_MAP = "dict_index";

//    public static final String LIST_POINTERS = "listPointers";
    public static final String POSTING_LIST_FILE = "posting_lists";

    public static final String INTERMEDIATE_TOKEN_FILE = "intermediate_token_file";
    public static final String INTERMEDIATE_TOKEN_FILE2 = "intermediate_token_file2";
    public static final String INTERMEDIATE_SEPARATOR = "$";

    private int termPointer;
    private int[] termLengths;
//    private TreeSet<String> termIDs;

    public IndexWriter() {
    }

    /**
     * Safely create all temporary files for the index
     * @param dir directory where the index files should be created
     * @return True is all files created successfully. False otherwise.
     *          If False is return IndexWriter will print a message and quit.
     */
    private boolean createFiles(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                System.out.println("unable to create directory");
                return false;
            }
        }
        if (!directory.isDirectory()) {
            System.out.println("Not a directory path");
            return false;
        }
        ArrayList<File> fileList = new ArrayList<>();
        fileList.add(new File(dir + File.separator + REVIEW_DATA_FILE));
        fileList.add(new File(dir + File.separator + TERM_FILE));
//        fileList.add(new File(dir + File.separator + DICT_REVIEW_PTRS));
        fileList.add(new File(dir + File.separator + DICT_REVIEW_PIDS));
        fileList.add(new File(dir + File.separator + TOKEN_MAP));
        fileList.add(new File(dir + File.separator + POSTING_LIST_FILE));
        fileList.add(new File(dir + File.separator + INTERMEDIATE_TOKEN_FILE));
        fileList.add(new File(dir + File.separator + INTERMEDIATE_TOKEN_FILE2));

        try {
            for (File f: fileList) {
                if (!f.exists()) {
                    f.createNewFile();
                }
            }
        } catch (IOException e) {
            System.out.println("Writer unable to create file");
            return false;
        }
        return true;
    }

    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void write(String inputFile, String dir) throws OutOfMemoryError {
        int curReviewID = 0;
        int numTokensInCurrent = 0;
        int numOfTokens = 0;

        if (!createFiles(dir)) {
            return;
        }
        try {
            RawDataParser rawParser = new RawDataParser(inputFile);

            OutputStreamHandler reviewDataWriter = new OutputStreamHandler(dir + File.separator + REVIEW_DATA_FILE);
//            OutputStreamHandler reviewPointersWriter = new OutputStreamHandler(dir + File.separator + DICT_REVIEW_PTRS);
            OutputStreamHandler reviewPIDWriter = new OutputStreamHandler(dir + File.separator + DICT_REVIEW_PIDS);
            OutputStreamHandler intermediateFileWriter = new OutputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE);

            reviewDataWriter.seek(Integer.BYTES * 2);

            long start = currentTimeMillis();
            long end = 0;

            while (!rawParser.endOfFile()) {
                List<RawReview> parsedRaw = rawParser.fileToString(MAX_REVIEWS_IN_MEM);

                for (RawReview rr: parsedRaw) {
                    curReviewID ++;

//                    reviewPointersWriter.writeLong(reviewDataWriter.getFilePointer());
                    reviewPIDWriter.writeString(rr.getProductID());
                    reviewDataWriter.writeString(rr.getProductID());
//                    reviewDataWriter.writeVInt(rr.getHelpfulness1());
//                    reviewDataWriter.writeVInt(rr.getHelpfulness2());
//                    reviewDataWriter.writeVInt(rr.getScore());

                    reviewDataWriter.writeInt(rr.getHelpfulness1());
                    reviewDataWriter.writeInt(rr.getHelpfulness2());
                    reviewDataWriter.writeInt(rr.getScore());

//                    numTokensInCurrent = getTokenTriplets(rr.getText(), curReviewID, intermediateFileWriter);
                    numTokensInCurrent = getTokenCouples(rr.getText(), curReviewID, intermediateFileWriter);

//                    reviewDataWriter.writeVInt(numTokensInCurrent);
                    reviewDataWriter.writeInt(numTokensInCurrent);
                    numOfTokens += numTokensInCurrent;

                    if (curReviewID >= REVIEWS_TO_READ) {
                        break;
                    }
                }
            }

            end = currentTimeMillis();
            System.out.println("read reviews time: " + (end - start));

//            reviewPointersWriter.close();
            reviewPIDWriter.close();
            rawParser.close();

            // Write number of reviews followed by number of tokens into the beginning of review data file.
            reviewDataWriter.seek(0);
            reviewDataWriter.writeInt(curReviewID);
            reviewDataWriter.writeInt(numOfTokens);
            reviewDataWriter.close();

            intermediateFileWriter.close();
            buildDictionary(dir);

        } catch (IOException e) {
            System.out.println("Writer unable to write");
            e.printStackTrace();
        }
//        catch (OutOfMemoryError e) {
//            System.out.println("managed to read: " + curReviewID);
//            e.printStackTrace();
//            throw e;
//        }
    }

    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
        File directory = new File(dir);
        if (! directory.isDirectory()) {
            System.out.println("Not a directory path");
        }
        File[] files =  directory.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (!f.delete()) {
                System.out.println(f.getName() + " file not deleted");
            }
        }
        directory.delete();
    }

    /**
     * Parse a text field of a review denoted by reviewID into separated tokens and for each token create a
     * triplet: (reviewID, frequency, term) where:
     * - term is the string associated with the token,
     * - reviewID is an integer given to the review.
     * - frequency is the number of times the term appears in the given text field.
     * Dump spaces and non alphanumeric characters.
     * Add the triplets to the input list.
     * Writes the triplets into an intermediate file to save on disk while reading the rest of the queries.
     * @param text: review text field.
     * @param reviewID:
     * @return : number of tokens in the review.
     */
    private int getTokenTriplets(String text, int reviewID, OutputStreamHandler intermediateFileWriter)
            throws IOException {
        List<String> terms = Arrays.asList(text.toLowerCase().split("[\\W_]"));
        HashMap<String, Short> histogram = new HashMap<>();
        List<String> sortedTerms;
        if (terms.size() == 0) {
            return 0;
        }
        int numOfTokens = 0;
        short curFrequency = 0;
        for (String t : terms) {
            if (t.length() < 1) {
                continue;
            }
            if (histogram.containsKey(t)) {
                histogram.replace(t, (short)(histogram.get(t) + 1));
            }
            else {
                histogram.put(t, (short)1);
            }
        }
        sortedTerms = new ArrayList<>(histogram.keySet());
        for (String t : sortedTerms) {
            curFrequency = histogram.get(t);
            intermediateFileWriter.writeTriplet(t, reviewID, curFrequency);
            numOfTokens += curFrequency;
        }
        return numOfTokens;
    }

    private int getTokenCouples(String text, int reviewID, OutputStreamHandler intermediateFileWriter)
            throws IOException {
        List<String> splitTerms = Arrays.asList(text.toLowerCase().split("[\\W_]"));
//        HashMap<String, Short> histogram = new HashMap<>();
        int numOfTokens = 0;
        if (splitTerms.size() == 0) {
            return 0;
        }
        for (String t : splitTerms) {
            if (t.length() > 0) {
                intermediateFileWriter.writeCouple(t, reviewID);
                numOfTokens++;
            }
        }
        return numOfTokens;
    }

    /**
     * Manage the construction of the dictionary:
     *  first, read blocks of triplets from the temporary file, sort them and rewrite the sorted triplets
     *      into the file
     *  later, the sorted blocks are read term-by-term in order to write the dictionary.
     * @param dir: dictionary to write the index files to.
     * @throws IOException
     */
    private void buildDictionary(String dir) throws IOException{
//        InputStreamHandler interReader1 = new InputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE);
//        OutputStreamHandler interWriter = new OutputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE2);

//        termIDs = new TreeSet<>();
        termPointer = 0;

        long start = currentTimeMillis();
        ArrayList<Long> filePointers = externalSortFirst(dir);
//        ArrayList<Long> filePointers = externalSortFirst(interReader1, interWriter);
        long end = currentTimeMillis();
        System.out.println("first stage time: " + (end - start));
//        System.out.println("terms: " + termIDs.size());
//        interReader1.close();
//        interWriter.close();

//        InputStreamHandler interReader2 = new InputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE2, MAX_TRIPLETS_IN_MEM);
        start = currentTimeMillis();
        externalSortSecond(filePointers, dir);
//        externalSortSecond(filePointers, dir, interReader2);
        end = currentTimeMillis();
        System.out.println("second stage time: " + (end - start));
//        interReader2.close();
    }

    /**
     * First stage of the external sort.
     * All tokens from the temporary file are read, sorted, and then rewritten, block-by-block.
//     * @param sortFileReader: temporary file for external sort
//     * @param sortFileWriter: temporary file for external sort
     * @return A list of file pointers, each points at the beginning of a block of triplets in the file
     * @throws IOException
     */
    private ArrayList<Long> externalSortFirst(String dir) throws IOException{
        InputStreamHandler sortFileReader = new InputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE);
        OutputStreamHandler sortFileWriter = new OutputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE2);
        TreeSet<String> termIDs = new TreeSet<>();

        long leftFilePointer = 0;
//        ArrayList<TokenTriplet> allBuffer;
        ArrayList<TokenCouple> allBuffer;
        ArrayList<Long> filePointers = new ArrayList<>();

        sortFileReader.seek(0);
//        allBuffer = sortFileReader.read_triplets(MAX_TRIPLETS_IN_MEM);
        allBuffer = sortFileReader.readCouples(MAX_TRIPLETS_IN_MEM);

        while (!allBuffer.isEmpty()) {
            filePointers.add(leftFilePointer);
            sortFileWriter.seek(leftFilePointer);
            Collections.sort(allBuffer);
//            sortFileWriter.writeTripletsFromList(allBuffer);
//            for (TokenTriplet t: allBuffer) {
            for (TokenCouple t: allBuffer) {
//                sortFileWriter.writeTriplet(t.getTerm(), t.getReviewID(), t.getFrequency());
                sortFileWriter.writeCouple(t.getTerm(), t.getReviewID());
                termIDs.add(t.getTerm());
            }
            leftFilePointer = sortFileWriter.getFilePointer();
//            allBuffer = sortFileReader.read_triplets(MAX_TRIPLETS_IN_MEM);
            allBuffer = sortFileReader.readCouples(MAX_TRIPLETS_IN_MEM);

        }
        System.out.println("num of tokens externalSortFirst: " + termIDs.size()); // debug
        sortFileReader.close();
        sortFileWriter.close();

        OutputStreamHandler termStringWriter = new OutputStreamHandler(dir + File.separator + TERM_FILE);
//        OutputStreamHandler indexWriter = new OutputStreamHandler(dir + File.separator + TOKEN_MAP);
        int termIndex = 0;
        termLengths = new int[termIDs.size()];
        for (String s: termIDs) {
//            indexWriter.writeInt(termPointer);
            termLengths[termIndex] = s.length();
            termIndex++;
            termStringWriter.writeString(s);
        }
        termStringWriter.close();

        return filePointers;
    }

    /**
     *
     * @param blockPointers: list of pointers to the beginning of each block in the sort file.
     * @param dir: dictionary where to write the index to.
//     * @param intermediateFile: temporary file to sort externally.
     * @throws IOException
     */
    private void externalSortSecond(ArrayList<Long> blockPointers, String dir) throws IOException
    {
        System.out.println("------------------ externalSortSecond start --------------");

        InputStreamHandler intermediateFile = new InputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE2);
        OutputStreamHandler indexWriter = new OutputStreamHandler(dir + File.separator + TOKEN_MAP);
        OutputStreamHandler listIndexWriter = new OutputStreamHandler(dir + File.separator + POSTING_LIST_FILE);

//        InputStreamHandler termReader = new InputStreamHandler(dir + File.separator + TERM_FILE);
        BufferedReader termReader = new BufferedReader(new FileReader (dir + File.separator + TERM_FILE));
//        FileWriter termStringWriter = new FileWriter(dir + File.separator + TERM_FILE);

        TreeSet<Integer> activePointers = new TreeSet<>();
        ArrayList<Long> filePointers = new ArrayList<>(blockPointers);

//        ArrayList<Integer> allInstances;
//        ArrayList<TokenCouple> allInstances;
//        ArrayList<TokenTriplet> allInstances;
        System.out.println("block pointers: " + blockPointers.size()); // debug
        int reads;
        int curLength;
        int curTermIndex = 0;
        char[] nextTermArr;
        String nextTerm;

        long nextPointer;
        int maxReads;
        for (int p = 0; p < blockPointers.size(); p++) {
            activePointers.add(p);
        }

        try {
            while (activePointers.size() > 0) {
                curLength = termLengths[curTermIndex];
                nextTermArr = new char[curLength];
                termReader.read(nextTermArr, 0, curLength);
                nextTerm = new String(nextTermArr);
                curTermIndex++;
//                allInstances = new ArrayList<>();

                indexWriter.writeInt(termPointer);
                termPointer += nextTerm.length();
                indexWriter.writeLong(listIndexWriter.getFilePointer());

                for (int p : activePointers) {
                    intermediateFile.seek(filePointers.get(p));

                    if (p != blockPointers.size() - 1) {
                        nextPointer = blockPointers.get(p+1);
                    } else {
                        nextPointer = intermediateFile.length();
                    }


                    // ---------------------------------------- readAllInstances -----------------
                    maxReads = (int)(nextPointer - filePointers.get(p));
                    reads = 0;
//                    int counter = 0;
                    if (intermediateFile.getFilePointer() >= intermediateFile.length()) {
                        continue;
                    }
                    long lastPointer = p;
//                    long lastPointer = intermediateFile.getFilePointer();
                    TokenCouple cur = intermediateFile.readCouple();

                    if (!Objects.equals(cur.getTerm(), nextTerm)) {
//            this.seek(lastPointer);
                        continue;
                    }

//        String term = cur.getTerm();
                    int lastWroteInt = 0;

                    while (Objects.equals(cur.getTerm(), nextTerm)) {

                        int curReviewID = cur.getReviewID();
//                        short curFrequency = 0;
//            allInstances.add(cur);
                        reads++;
                        if (reads > maxReads) {
//                            System.out.println("reached max reads"); // debug
                            break;
                        }

                        listIndexWriter.writeVInt(curReviewID - lastWroteInt);
                        lastWroteInt = curReviewID;

//                        allInstances.add(cur.getReviewID());

//                      if (allInstances.size() % 100000 == 0) System.out.println(allInstances.size() + ":: " + term); //debug

                        if (intermediateFile.getFilePointer() == intermediateFile.length()) {
//                            System.out.println("pointer: " + p + " reached eof"); // debug
                            lastPointer = intermediateFile.getFilePointer();
                            break;
                        }
                        lastPointer = intermediateFile.getFilePointer();
                        cur = intermediateFile.readCouple();
                    }
//                    intermediateFile.seek(lastPointer);
//                    reads = intermediateFile.readAllInstances(allInstances, nextTerm, (int)(nextPointer - filePointers.get(p)));
//              ---------------------------------------- readAllInstances end -----------------

//                    if (reads * 24L >= nextPointer - filePointers.get(p)) {
//                        System.out.println("instances of:" + nextTerm + " exceed block bounds");
//                    }

                    if (reads != 0) {
                        filePointers.set(p, lastPointer);
                    }
                }
                for (int p = 0; p < blockPointers.size() - 1; p++) {
                    if (filePointers.get(p) >= blockPointers.get(p + 1)) {
                        activePointers.remove(p);
                    }
                }
                if (filePointers.get(filePointers.size() - 1) == intermediateFile.length()) {
                    activePointers.remove(filePointers.size() - 1);
                }

                // ----------------- writeTokens -------------------
//                if (allInstances.isEmpty()) {
//                    return;
//                }

//                int curReviewID = allInstances.get(0);
//                short curFrequency = 0;
//                LinkedList<Integer> containingReviews = new LinkedList<>();
//                LinkedList<Short> containingReviewsFreq = new LinkedList<>();

//                containingReviews.add(curReviewID);
//                for (Integer t: allInstances) {
//                    if (t == curReviewID) {
//                        curFrequency++;
//                    }
//                    else {
//                        containingReviewsFreq.add(curFrequency);
//                        // add new review
//                        curReviewID = t;
//                        containingReviews.add(t);
//                        curFrequency = 1;
//                    }
//                }
//                containingReviewsFreq.add(curFrequency);

//                indexWriter.writeInt(termPointer);
//                termPointer += nextTerm.length();
//                indexWriter.writeLong(listIndexWriter.getFilePointer());

                // -------------- writeContainingReviews ------------------------------
//                int lastWroteInt = 0;
//                listIndexWriter.writeVInt(containingReviews.size()); TODO do not read size, read distance to next pointer instead
//                for (int rId : containingReviews) {//
//                    listIndexWriter.writeVInt(rId - lastWroteInt);
//                    lastWroteInt = rId;
//                }
//                for (int rf : containingReviewsFreq) {
//                    listIndexWriter.writeVInt(rf);
//                }
//                writeContainingReviews(containingReviews, containingReviewsFreq, listIndexWriter);
                // -------------------- writeContainingReviews end ----------------------
//                writeTokens(allInstances, indexWriter, listIndexWriter, nextTerm);
                // ------------------ writeTokens end ----------------------

            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            intermediateFile.close();
//        termStringWriter.close();
            indexWriter.close();
            listIndexWriter.close();
            termReader.close();
        }
    }

    /**
     * Write complete dictionary token values into the index files from a list of triplets.
     * @param reviewIDs: list of TokenTriplets contains all triplets corresponding to a single term
//     * @param indexWriter
     * @param listIndexWriter
//     * @param termStringWriter
     * @return
     * @throws IOException
     */

//    public void writeTokens(List<TokenTriplet> triplets,
//                           OutputStreamHandler indexWriter,
//                           OutputStreamHandler listIndexWriter) throws IOException{
//    public void writeTokens(List<TokenCouple> couples,
//                            OutputStreamHandler indexWriter,
//                            OutputStreamHandler listIndexWriter) throws IOException
    public void writeTokens(List<Integer> reviewIDs,
                            OutputStreamHandler indexWriter,
                            OutputStreamHandler listIndexWriter, String term) throws IOException
    {
        if (reviewIDs.isEmpty()) {
            return;
        }

        int curReviewID = reviewIDs.get(0);
        short curFrequency = 0;

        LinkedList<Integer> containingReviews = new LinkedList<>();
        LinkedList<Short> containingReviewsFreq = new LinkedList<>();

        containingReviews.add(curReviewID);
        for (Integer t: reviewIDs) {
            if (t == curReviewID) {
                curFrequency++;
            }
            else {
                containingReviewsFreq.add(curFrequency);
                // add new review
                curReviewID = t;
                containingReviews.add(t);
                curFrequency = 1;
            }
        }
        containingReviewsFreq.add(curFrequency);
        indexWriter.writeInt(termPointer);
        termPointer += term.length();
        indexWriter.writeLong(listIndexWriter.getFilePointer());
        writeContainingReviews(containingReviews, containingReviewsFreq, listIndexWriter);

    }


    /**
     * Write posting lists of a single token.
     * For a specific token, write the token's containing reviews list and the list of frequencies
     *  in the reviews into the index file.
     * @param containingReviews: list of reviewIDs of reviews that contain the token.
     * @param containingReviewsFreq: for each reviewID i in containingReviews the i'th element of
     *                             containingReviewsFreq contains the number of times the token appeared in
     *                             the i'th review.
     * @param listIndexWriter: file where the posting lists are saved into.
     * @return the total number of appearances of the token.
     * @throws IOException
     */
    private int writeContainingReviews(List<Integer> containingReviews, List<Short> containingReviewsFreq,
                                       OutputStreamHandler listIndexWriter) throws IOException{
        int numOfTokens = 0;
        int lastWroteInt = 0;
        listIndexWriter.writeVInt(containingReviews.size());
        for (int rId : containingReviews) {
            listIndexWriter.writeVInt(rId - lastWroteInt);
            lastWroteInt = rId;
        }
        for (int rf : containingReviewsFreq) {
            listIndexWriter.writeVInt(rf);
            numOfTokens += rf;
        }
        return numOfTokens;
    }
}
