package webdata;

import java.io.*;
import java.util.*;

import static java.lang.System.currentTimeMillis;

/**
 * Created by eranhashmo on 5/6/2021.
 */
public class IndexWriter {
    private final int REVIEWS_TO_READ =   1000 * 1000;
//    private final int REVIEWS_TO_READ =   1000;

    private final int MAX_REVIEWS_IN_MEM = 3 *1000*1000;
//    private final int MAX_REVIEWS_IN_MEM = 1000;
//    private final int MAX_REVIEWS_IN_MEM = 2;

    private final int MAX_COUPLES_IN_MEM = 27 * 1000 * 1000;
//    private final int MAX_COUPLES_IN_MEM = 1000;
//    private final int MAX_COUPLES_IN_MEM = 2;

    public static final String REVIEW_DATA_FILE = "review_index";
    public static final String TERM_FILE = "term_String_File";
    public static final String DICT_REVIEW_PIDS = "dict_review_PIDs";
    public static final String TOKEN_MAP = "dict_index";
    public static final String POSTING_LIST_FILE = "posting_lists";

    public static final String INTERMEDIATE_TOKEN_FILE = "intermediate_token_file";
    public static final String INTERMEDIATE_TOKEN_FILE2 = "intermediate_token_file2";
    public static final String INTERMEDIATE_SEPARATOR = "$";

//    private int termPointer;
    private int[] termLengths;

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
            OutputStreamHandler reviewPIDWriter = new OutputStreamHandler(dir + File.separator + DICT_REVIEW_PIDS);
            OutputStreamHandler intermediateFileWriter = new OutputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE);

            reviewDataWriter.seek(Integer.BYTES * 2);

            long start = currentTimeMillis();
            long end = 0;

            while (!rawParser.endOfFile()) {
                List<RawReview> parsedRaw = rawParser.fileToString(MAX_REVIEWS_IN_MEM);

                for (RawReview rr: parsedRaw) {
                    curReviewID ++;

                    reviewPIDWriter.writeString(rr.getProductID());
                    reviewDataWriter.writeString(rr.getProductID());
                    reviewDataWriter.writeInt(rr.getHelpfulness1());
                    reviewDataWriter.writeInt(rr.getHelpfulness2());
                    reviewDataWriter.writeInt(rr.getScore());

                    numTokensInCurrent = getTokenCouples(rr.getText(), curReviewID, intermediateFileWriter);
                    reviewDataWriter.writeInt(numTokensInCurrent);
                    numOfTokens += numTokensInCurrent;
                    if (curReviewID >= REVIEWS_TO_READ) {
                        break;
                    }
                }
            }

            end = currentTimeMillis();
            System.out.println("read reviews time: " + (end - start));

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
     * Parse a text field of a review denoted by reviewID into separated tokens and for each token create a couple.
     * Couple: (reviewID, term) where:
     * - term is the string associated with the token,
     * - reviewID is an integer given to the review.
     * Dump spaces and non-alphanumeric characters.
     * Writes the couples into an intermediate file to save on disk while reading the rest of the queries.
     * @param text review text field.
     * @param reviewID  Integer, starts from 1
     * @param intermediateFileWriter writes the couple to the temporary file.
     * @return
     * @throws IOException
     */
    private int getTokenCouples(String text, int reviewID, OutputStreamHandler intermediateFileWriter)
            throws IOException {
        List<String> splitTerms = Arrays.asList(text.toLowerCase().split("[\\W_]"));
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
     *  first, read blocks of couples from the temporary file, sort them and rewrite the sorted couples
     *      into the file
     *  later, the sorted blocks are read term-by-term in order to write the dictionary.
     * @param dir: dictionary to write the index files to.
     * @throws IOException
     */
    private void buildDictionary(String dir) throws IOException{
//        termPointer = 0;

        long start = currentTimeMillis();
        ArrayList<Long> filePointers = externalSortFirst(dir);
        long end = currentTimeMillis();
        System.out.println("first stage time: " + (end - start));
        start = currentTimeMillis();
        externalSortSecond(filePointers, dir);
        end = currentTimeMillis();
        System.out.println("second stage time: " + (end - start));
    }

    /**
     * First stage of the external sort.
     * All tokens from the temporary file are read, sorted, and then rewritten, block-by-block.
//     * @param sortFileReader: temporary file for external sort
//     * @param sortFileWriter: temporary file for external sort
     * @return A list of file pointers, each points at the beginning of a block of couples in the file
     * @throws IOException
     */
    private ArrayList<Long> externalSortFirst(String dir) throws IOException
    {
        InputStreamHandler sortFileReader = new InputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE);
        OutputStreamHandler sortFileWriter = new OutputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE2);
        TreeSet<String> termIDs = new TreeSet<>();

        long leftFilePointer = 0;
        ArrayList<TokenCouple> allBuffer;
        ArrayList<Long> filePointers = new ArrayList<>();

        sortFileReader.seek(0);
        allBuffer = sortFileReader.readCouples(MAX_COUPLES_IN_MEM);

        while (!allBuffer.isEmpty()) {
            filePointers.add(leftFilePointer);
            Collections.sort(allBuffer);
            for (TokenCouple t: allBuffer) {
                sortFileWriter.writeCouple(t.getTerm(), t.getReviewID());
                termIDs.add(t.getTerm());
            }
            leftFilePointer = sortFileWriter.getFilePointer();
            allBuffer = sortFileReader.readCouples(MAX_COUPLES_IN_MEM);
        }
        System.out.println("num of tokens externalSortFirst: " + termIDs.size()); // debug
        sortFileReader.close();
        sortFileWriter.close();

        setTermLengths(dir, termIDs);

        return filePointers;
    }

    private void setTermLengths(String dir, TreeSet<String> termIDs) throws IOException {
        OutputStreamHandler termStringWriter = new OutputStreamHandler(dir + File.separator + TERM_FILE);
        int termIndex = 0;
        termLengths = new int[termIDs.size()];
        for (String s: termIDs) {
            termLengths[termIndex] = s.length();
            termIndex++;
            termStringWriter.writeString(s);
        }
        termStringWriter.close();
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
//        System.out.println("------------------ externalSortSecond start --------------");
        InputStreamHandler intermediateFile = new InputStreamHandler(dir + File.separator + INTERMEDIATE_TOKEN_FILE2);
        OutputStreamHandler indexWriter = new OutputStreamHandler(dir + File.separator + TOKEN_MAP);
        OutputStreamHandler listIndexWriter = new OutputStreamHandler(dir + File.separator + POSTING_LIST_FILE);
        BufferedReader termReader = new BufferedReader(new FileReader (dir + File.separator + TERM_FILE));

        TreeSet<Integer> activePointers = new TreeSet<>();
        ArrayList<Long> filePointers = new ArrayList<>(blockPointers);
        System.out.println("block pointers: " + blockPointers.size()); // debug
        int writeTermPointer = 0;
        int curLength;
        int curTermIndex = 0;
        char[] nextTermArr;
        String nextTerm;
        long nextPointer;
        int curReviewID = 0;
        int lastWroteInt = 0;

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

                indexWriter.writeInt(writeTermPointer);
                writeTermPointer += nextTerm.length();
                indexWriter.writeLong(listIndexWriter.getFilePointer());

                lastWroteInt = 0;

                for (int p : activePointers) {
                    intermediateFile.seek(filePointers.get(p));

                    if (p != blockPointers.size() - 1) {
                        nextPointer = blockPointers.get(p+1);
                    } else {
                        nextPointer = intermediateFile.length();
                    }

                    // ---------------------------------------- readAllInstances -----------------
                    if (intermediateFile.getFilePointer() >= intermediateFile.length()) {
                        continue;
                    }
                    long lastPointer = p;
                    TokenCouple cur = intermediateFile.readCouple();

                    if (!Objects.equals(cur.getTerm(), nextTerm)) {
                        continue;
                    }
                    while (Objects.equals(cur.getTerm(), nextTerm)) {
                        curReviewID = cur.getReviewID();

                        listIndexWriter.writeVInt(curReviewID - lastWroteInt);
                        lastWroteInt = curReviewID;

                        lastPointer = intermediateFile.getFilePointer();

                        if (intermediateFile.getFilePointer() >= nextPointer) {
                            break;
                        }
                        cur = intermediateFile.readCouple();
                    }
                    filePointers.set(p, lastPointer);
                }
                for (int p = 0; p < blockPointers.size() - 1; p++) {
                    if (filePointers.get(p) >= blockPointers.get(p + 1)) {
                        activePointers.remove(p);
                    }
                }
                if (filePointers.get(filePointers.size() - 1) == intermediateFile.length()) {
                    activePointers.remove(filePointers.size() - 1);
                }
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            intermediateFile.close();
            indexWriter.close();
            listIndexWriter.close();
            termReader.close();
        }
    }
}
