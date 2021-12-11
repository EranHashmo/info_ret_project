package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
     * @param bufferSize: number of couples to read
     * @return ArrayList of couples read.
     * @throws IOException if readCouple fail at some point.
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
     * @throws IOException if a read method failed.
     */
    public TokenCouple readCouple() throws IOException{
        int reviewID = readInt();
        String term = readTerm();
        return new TokenCouple(term, reviewID);
    }


    /**
     * Helper method for readTriplets
     * Reads the term of the next triplet in the file.
     * @return String: the term read
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
