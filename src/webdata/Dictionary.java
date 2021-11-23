package webdata;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Assumes there is enough data in main memory to store entire dictionary before compressing it.
 */
public class Dictionary {
    static final int CONTAINED_LIST = 0;
    static final int CONTAINED_FREQ = 1;

    private int numOfReviews;
    private int numOfTokens;
    private ArrayList<Token> tokens;
    private String wordString;

    public Dictionary(String dir) {
        readTokens(dir);
    }

    /**
     * Returns token corresponding to term.
     * The dictionary must be tokens.
     * @param term:
     * @return: Token object corresponding to given term.
     */
    public Token getToken(String term) throws IOException {
        int tokenPointer = binarySearch(term);
        if (tokenPointer < 0) {
            return null;
        }
        return tokens.get(tokenPointer);
    }

    /**
     * Reads the concatenated string into the dictionary.
     * @param dir: directory where the dictionary is kept.
     * @throws IOException
     */
    private void readTermString(String dir) throws IOException {
        Path termString = Paths.get(dir + File.separator + IndexWriter.TERM_FILE);
        wordString = new String(Files.readAllBytes(termString));
    }


//    /**
//     * Return pointer to the position in the indexFile where the review is at According to reviewID.
//     * @param reviewID: ID of the review to look for.
//     * @return position in the index file where the review is at.
//     */
//    public long getReviewPointer(int reviewID, String workDir) throws IOException {
//        int longSizeBytes = 8;
//        if (reviewID > numOfReviews) {
//            return -1;
//        }
//        File reviewPtrFile = new File(workDir +
//                File.separator + IndexWriter.DICT_REVIEW_PTRS);
//        RandomAccessFile reviewPointersReader = new RandomAccessFile(reviewPtrFile, "r");
//        reviewPointersReader.seek((reviewID - 1) * longSizeBytes);
//        long reviewPointer = reviewPointersReader.readLong();
//        reviewPointersReader.close();
//        return reviewPointer;
//    }

    /**
     *
     * @return Number of reviews in the index.
     */
    public int getNumReviews() {
        return numOfReviews;
    }

    /**
     *
     * @return Total number of tokens in the index (counts duplicates)
     */
    public int getNumTokens() {
        return numOfTokens;
    }

    /**
     *
     * @return mapping from reviewID to the productID of corresponding review.
     */
    public ArrayList<String> getReviewMapPids(String workDir) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        Path reviewPIDFile = Paths.get(workDir + File.separator + IndexWriter.DICT_REVIEW_PIDS);
        String allPIDs = new String(Files.readAllBytes(reviewPIDFile));

        for (int bytePointer = 0; bytePointer < allPIDs.length(); bytePointer += RawDataParser.PID_LENGTH) {
            result.add(allPIDs.substring(bytePointer, bytePointer + RawDataParser.PID_LENGTH));
        }
        return result;
    }

    /**
     * Given a token, read the list of reviews containing the token and a list
     * of frequencies of the token in each review
//     * @param token:
     * @param dir: directory where the index is kept.
     * @return ArrayLis of two Array lists:
     *          containingReviews: The first list contains review IDs where the token appears.
     *          containingReviewsFreq: The second list contains the number of appearances
     *              of the token in a corresponding review.
     *          for each 0 <= i <= containingReviews.size() the review at index i has
     *              containingReviewsFreq[i] appearances of the given token in it.
     * @throws IOException
     */
//    public ArrayList<ArrayList<Integer>> getContainingReviews(Token token, String dir) throws IOException {
    public ArrayList<Integer> getContainingReviews(String term, String dir) throws IOException {

        int lastReadInt = 0;
        int readInt = 0;
        ArrayList<Integer> containingList = new ArrayList<>();
//        ArrayList<Integer> containingListFreq = new ArrayList<>();

        File listFile = new File(dir + File.separator + IndexWriter.POSTING_LIST_FILE);
        RandomAccessFile listFileReader = new RandomAccessFile(listFile, "r");
//        listFileReader.seek(token.getListPointer());

        int tokenPointer = binarySearch(term);
        if (tokenPointer < 0) {
            return null;
        }

        Token token = tokens.get(tokenPointer);
        long nextPointer;
        if (tokenPointer == tokens.size() - 1) {
            nextPointer = listFileReader.length();
        } else {
            nextPointer = tokens.get(tokenPointer + 1).getListPointer();
        }
        listFileReader.seek(token.getListPointer());

//        int listSize = Parser.readVInt(listFileReader);
        while (listFileReader.getFilePointer() != nextPointer) {
//        for (int i = 0; i < listSize; i++) {
            readInt = Parser.readVInt(listFileReader);
            containingList.add(lastReadInt + readInt);
            lastReadInt = lastReadInt + readInt;
        }
//        for (int i = 0; i < listSize; i++) {
//            containingListFreq.add(Parser.readVInt(listFileReader));
//        }


        listFileReader.close();
//        ArrayList<ArrayList<Integer>> pair = new ArrayList<>();
//        pair.add(containingList);
//        pair.add(containingListFreq);
//        return pair;
        return containingList;
    }

    /**
     * Load the pointers to the dictionary in Token data structure.
     * @param dir: directory to read from.
     */
    public void readTokens(String dir) {
        tokens = new ArrayList<>();
        File f = new File(dir + File.separator + IndexWriter.TOKEN_MAP);
        try {
            RandomAccessFile tokenReader = new RandomAccessFile(f.getAbsolutePath(), "r");
            tokenReader.seek(0);
            Token token;
            while (tokenReader.getFilePointer() < f.length()) {
                token = new Token();
                token.setTermPointer(tokenReader.readInt());
                token.setListPointer(tokenReader.readLong());
                tokens.add(token);
            }
            tokenReader.close();
            readTermString(dir);
            readNumOfReviews(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Look in the dictionary for the token pointing at given term.
     * @param term Term to search for.
     * @return The index of the token.
     */
    private int binarySearch(String term) {
        int low = 0;
        int high = tokens.size();
        int mid = (low + high) / 2;
        String pointedAt;
        int midPointer = tokens.get(mid).getTermPointer();
        int nextWord;
        if (mid == tokens.size() - 1) {
            pointedAt = wordString.substring(midPointer);
        } else {
            nextWord = tokens.get(mid + 1).getTermPointer();
            pointedAt = wordString.substring(midPointer, nextWord);
        }
        while (!Objects.equals(term, pointedAt)) {
            if (high == mid || low == mid) {
                return -1;
            }
            if (pointedAt.compareTo(term) < 0) {
                low = mid;
            } else {
                high = mid;
            }
            mid = (low + high) / 2;
            midPointer = tokens.get(mid).getTermPointer();
            if (mid == tokens.size() - 1) {
                pointedAt = wordString.substring(midPointer);
            } else {
                nextWord = tokens.get(mid + 1).getTermPointer();
                pointedAt = wordString.substring(midPointer, nextWord);
            }
        }
        return mid;
    }

    /**
     * Read the number of reviews from the disk and save on memory.
     * @param dir: directory where the index is stored at.
     * @throws IOException
     */
    private void readNumOfReviews(String dir) throws IOException {
        File f = new File(dir + File.separator + IndexWriter.REVIEW_DATA_FILE);
        RandomAccessFile reviewFile = new RandomAccessFile(f, "r");
        reviewFile.seek(0);
        this.numOfReviews = reviewFile.readInt();
        this.numOfTokens = reviewFile.readInt();
        reviewFile.close();
    }
}

