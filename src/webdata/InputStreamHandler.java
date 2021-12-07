package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class InputStreamHandler {
    private RandomAccessFile reader;
    private BufferedInputStream bis;

    long readIndex;

    public InputStreamHandler(String filePath) throws IOException {
        File file = new File(filePath);
        reader = new RandomAccessFile(file, "rw");
        FileInputStream ins = new FileInputStream(reader.getFD());
        bis = new BufferedInputStream(ins);
        readIndex = 0;
    }

    public InputStreamHandler(String filePath, int bufferSize) throws IOException {
        File file = new File(filePath);
        reader = new RandomAccessFile(file, "rw");
        FileInputStream ins = new FileInputStream(reader.getFD());
        bis = new BufferedInputStream(ins, bufferSize);
        readIndex = 0;
    }

    public long getFilePointer() {
        return readIndex;
    }

    public void seek(long filePointer) throws IOException {
        reader.seek(filePointer);
        FileInputStream ins = new FileInputStream(reader.getFD());
        bis = new BufferedInputStream(ins);
        readIndex = filePointer;
    }

    public long length() throws IOException{
        return reader.length();
    }


    /**
     * reads all instances of couples with given term from file.
     * Assume the file is sorted and all instances are found together.
     //     * @param term term to search.
     * @return
     */
//    public int readAllInstances(List<TokenCouple> allInstances, String term) throws IOException {
    public int readAllInstances(List<Integer> allInstances, String term, int maxReads) throws IOException {
        int counter = 0;
        if (this.getFilePointer() >= this.length()) {
            return counter;
        }
        long lastPointer = 0;
//        long lastPointer = getFilePointer();
        TokenCouple cur = readCouple();

        if (!Objects.equals(cur.getTerm(), term)) {
//            this.seek(lastPointer);
            return counter;
        }
//        String term = cur.getTerm();
        while (Objects.equals(cur.getTerm(), term)) {
//            allInstances.add(cur);
            counter++;
            if (counter > maxReads) {
                System.out.println("reached max reads"); // debug
                break;
            }

            allInstances.add(cur.getReviewID());

//            if (allInstances.size() % 100000 == 0) System.out.println(allInstances.size() + ":: " + term); //debug

            if (this.getFilePointer() == this.length()) {
                return counter;
            }
            lastPointer = this.getFilePointer();
            cur = readCouple();
        }
        this.seek(lastPointer);
        return counter;
    }

    public List<Integer> readAllInstances(String term, int maxReads) throws IOException {
        ArrayList<Integer> result = new ArrayList<>();
        if (this.getFilePointer() >= this.length()) {
            return result;
        }
        long lastPointer = 0;
//        long lastPointer = getFilePointer();
        TokenCouple cur = readCouple();

        if (!Objects.equals(cur.getTerm(), term)) {
            this.seek(lastPointer);
            return result;
        }
//        String term = cur.getTerm();
        while (Objects.equals(cur.getTerm(), term)) {
//            allInstances.add(cur);
            result.add(cur.getReviewID());
//            counter++;

//            if (allInstances.size() % 100000 == 0) System.out.println(allInstances.size() + ":: " + term); //debug

            if (this.getFilePointer() == this.length()) {
                return result;
            }
            lastPointer = this.getFilePointer();
            cur = readCouple();
        }
        this.seek(lastPointer);
        return result;
    }


    /**
     * @param bufferSize: number of couples to read
     * @return
     * @throws IOException
     */
    public ArrayList<TokenCouple> readCouples(int bufferSize)
            throws IOException{
        ArrayList<TokenCouple> couples = new ArrayList<>(bufferSize);
        while (couples.size() < bufferSize && this.getFilePointer() < this.length()) {
            TokenCouple tc = readCouple();
            couples.add(tc);
        }
        return couples;
    }


    /**
     * Read a single couple (reviewID, term).
     * @return a new TokenCouple object.
     * @throws IOException
     */
    public TokenCouple readCouple() throws IOException{
        int reviewID = readInt();
        String term = readTerm();
        return new TokenCouple(term, reviewID);
    }


    /**
     * Helper method for readTriplets
     * Reads the term of the next triplet in the file.
     * @return
     */
    private String readTerm() throws IOException {
        char curChar;
        StringBuilder curTerm;
        curTerm = new StringBuilder();
        curChar = (char) (bis.read() & 0xFF);
        readIndex++;
        while (curChar != IndexWriter.INTERMEDIATE_SEPARATOR.charAt(0)) {
            curTerm.append(curChar);
            curChar = (char) (bis.read() & 0xFF);
            readIndex++;
        }
        return curTerm.toString();
    }

    public short readShort() throws IOException {
        byte[] bytes = new byte[Short.BYTES];
        readIndex += bis.read(bytes, 0, Short.BYTES);
        return ByteBuffer.wrap(bytes).getShort();
    }

    public int readInt() throws IOException {
        byte[] bytes = new byte[Integer.BYTES];
        readIndex += bis.read(bytes, 0, Integer.BYTES);
        return ByteBuffer.wrap(bytes).getInt();
    }

    public long readLong() throws IOException {
        byte[] bytes = new byte[Long.BYTES];
        readIndex += bis.read(bytes, 0, Long.BYTES);
        return ByteBuffer.wrap(bytes).getLong();

    }

    public void close() throws IOException {
        bis.close();
    }
}
