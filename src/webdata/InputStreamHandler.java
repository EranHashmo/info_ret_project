package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InputStreamHandler {
    private RandomAccessFile reader;
//    FileInputStream ins;
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

    public long getFilePointer() throws IOException{
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

//    /**
//     * reads all instances of triplets with given term from file.
//     * Assume the file is sorted and all instances are found together.
////     * @param term term to search.
//     * @return
//     */
//    public int readAllInstances(List<TokenTriplet> allInstances, String term) throws IOException {
//        int counter = 0;
//        if (this.getFilePointer() >= this.length()) {
//            return counter;
//        }
//        long lastPointer = 0;
////        long lastPointer = getFilePointer();
//        TokenTriplet cur = readTriplet();
//
//        if (!Objects.equals(cur.getTerm(), term)) {
//            this.seek(lastPointer);
//            return counter;
//        }
////        String term = cur.getTerm();
//        while (Objects.equals(cur.getTerm(), term)) {
//            allInstances.add(cur);
//            counter++;
//            if (this.getFilePointer() == this.length()) {
//                return counter;
//            }
//            lastPointer = this.getFilePointer();
//            cur = readTriplet();
//        }
//        this.seek(lastPointer);
//        return counter;
//    }


    /**
     * reads all instances of couples with given term from file.
     * Assume the file is sorted and all instances are found together.
     //     * @param term term to search.
     * @return
     */
    public int readAllInstances(List<TokenCouple> allInstances, String term) throws IOException {
        int counter = 0;
        if (this.getFilePointer() >= this.length()) {
            return counter;
        }
        long lastPointer = 0;
//        long lastPointer = getFilePointer();
        TokenCouple cur = readCouple();

        if (!Objects.equals(cur.getTerm(), term)) {
            this.seek(lastPointer);
            return counter;
        }
//        String term = cur.getTerm();
        while (Objects.equals(cur.getTerm(), term)) {
            allInstances.add(cur);
            counter++;
            if (this.getFilePointer() == this.length()) {
                return counter;
            }
            lastPointer = this.getFilePointer();
            cur = readCouple();
        }
        this.seek(lastPointer);
        return counter;
    }

    /**
     * @param bufferSize: number of triplets to read
     * @return
     * @throws IOException
     */
    public ArrayList<TokenTriplet> read_triplets(int bufferSize)
            throws IOException{
        int bufferCheck = 0;

        ArrayList<TokenTriplet> triplets = new ArrayList<>(bufferSize);
        while (bufferCheck < bufferSize && this.getFilePointer() < this.length()) {
//            if (bufferCheck == 52934) System.out.println("here");     // debug
            TokenTriplet tt = readTriplet();
            triplets.add(tt);
            bufferCheck ++;
        }
        return triplets;
    }


    /**
     * @param bufferSize: number of couples to read
     * @return
     * @throws IOException
     */
    public ArrayList<TokenCouple> readCouples(int bufferSize)
            throws IOException{
        int bufferCheck = 0;

        ArrayList<TokenCouple> couples = new ArrayList<>(bufferSize);
        while (bufferCheck < bufferSize && this.getFilePointer() < this.length()) {
//            if (bufferCheck == 52934) System.out.println("here");     // debug
            TokenCouple tc = readCouple();
            couples.add(tc);
            bufferCheck ++;
        }
        return couples;
    }


    /**
     * Read a single couple (reviewID, term).
     * @return a new TokenCouple object.
     * @throws IOException
     */
    private TokenCouple readCouple() throws IOException{
        int reviewID = readInt();
        String term = readTerm();
        return new TokenCouple(term, reviewID);
    }

    /**
     * Read a single triplet (reviewID, frequency, term).
     * @return a new TokenTriplet object.
     * @throws IOException
     */
    private TokenTriplet readTriplet() throws IOException{
        int reviewID = readInt();
        short frequency = readShort();
        String term = readTerm();
        return new TokenTriplet(term, reviewID, frequency);
    }

    /**
     * Helper method for readTriplets
     * Reads the term of the next triplet in the file.
     * @return
     */
    private String readTerm() throws IOException{
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

    public short readShort() throws IOException{
        byte[] bytes = new byte[Short.BYTES];
//        ins.readNBytes(bytes, 0 , Short.BYTES);
        readIndex += bis.read(bytes, 0, Short.BYTES);
        return ByteBuffer.wrap(bytes).getShort();
    }

    public int readInt() throws IOException{
        byte[] bytes = new byte[Integer.BYTES];
//        ins.readNBytes(bytes, 0 , Integer.BYTES);
        readIndex += bis.read(bytes, 0, Integer.BYTES);
        return ByteBuffer.wrap(bytes).getInt();
    }

    public long readLong() throws IOException {
        byte[] bytes = new byte[Long.BYTES];
//        ins.readNBytes(bytes, 0 , Long.BYTES);
        readIndex += bis.read(bytes, 0, Long.BYTES);
        return ByteBuffer.wrap(bytes).getLong();

    }

    public void close() throws IOException{
//        reader.close();
        bis.close();
    }
}
