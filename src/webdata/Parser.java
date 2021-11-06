package webdata;

/**
 * Created by eranhashmo on 4/10/2021.
 */

import java.io.IOException;
import java.io.File;
import  java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * Class that reads from the index files and builds Review objects that encapsulate all relevant review data.
 */
public class Parser {
    private File f;

    public Parser(String filepath){
        f = new File(filepath);
    }

    /**
     * Read a single review from the index.
     * Integers are encoded using varInt.
     * @param reviewPtr: pointer to the byte on the index to start reading from.
     * @return Review object that encapsulates all relevant review data.
     * @throws IOException: If the parser could not read the file.
     */
    public Review readReview(long reviewPtr) throws IOException {
        RandomAccessFile file = new RandomAccessFile(f, "r");
        file.seek(reviewPtr);
        byte[] pIdData = new byte[RawDataParser.PID_LENGTH];
        file.read(pIdData);
        String pId = new String(pIdData, StandardCharsets.UTF_8);
//        int helpfulness1 = readVInt(file);
//        int helpfulness2 = readVInt(file);
//        int score = readVInt(file);
//        int numOfTokens = readVInt(file);
        int helpfulness1 = file.readInt();
        int helpfulness2 = file.readInt();
        int score = file.readInt();
        int numOfTokens = file.readInt();
        file.close();
        return new Review(pId, helpfulness1, helpfulness2, score, numOfTokens);
    }

    /**
     * Read a variable length integer from the supplied InputStream
     * @return the int value
     * @throws IOException if the value cannot be read from the input
     */
    static int readVInt(RandomAccessFile file) throws IOException {

        byte b = (byte)file.read();

        if (b == (byte) 0x80) {
            throw new RuntimeException("Attempting to read null value as int");
        }

        int value = b & 0x7F;
        while ((b & 0x80) != 0) {
            b = (byte)file.read();
            value <<= 7;
            value |= (b & 0x7F);
        }
        return value;
    }

    /**
     * Read a variable length long from the supplied InputStream.
     * @param in the input stream to read from
     * @return the long value
     * @throws IOException if the value cannot be read from the input stream
     */
    public static long readVLong(RandomAccessFile in) throws IOException {
        byte b = (byte)in.read();

        if (b == (byte) 0x80) {
            throw new RuntimeException("Attempting to read null value as long");
        }

        long value = b & 0x7F;
        while ((b & 0x80) != 0) {
            b = (byte)in.read();
            value <<= 7;
            value |= (b & 0x7F);
        }

        return value;
    }

}

