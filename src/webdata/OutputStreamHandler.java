package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OutputStreamHandler {

    private RandomAccessFile writer;
    private BufferedOutputStream bos;
    private long writerIndex;

    public OutputStreamHandler(String filePath) throws IOException {
        File file = new File(filePath);
        writer = new RandomAccessFile(file, "rw");
        FileOutputStream os = new FileOutputStream(writer.getFD());
        bos = new BufferedOutputStream(os);
        writerIndex = 0;
    }

    public OutputStreamHandler(String filePath, int bufferSize) throws IOException {
        File file = new File(filePath);
        writer = new RandomAccessFile(file, "rw");
        FileOutputStream os = new FileOutputStream(writer.getFD());
        bos = new BufferedOutputStream(os, bufferSize);
        writerIndex = 0;
    }

    public long getFilePointer() {
        return writerIndex;
    }

    public void seek(long filePointer) throws IOException {
        bos.flush();
        writer.seek(filePointer);
        writerIndex = filePointer;
//        os = new FileOutputStream(writer.getFD());
//        bos = new BufferedOutputStream(os);
    }

    public long length() throws IOException{
        return writer.length();
    }

    /**
     * Write a single token couple: (reviewID, term) onto a temporary file for external sort.
     * @param term: String, the term of the token.
     * @param reviewID: review in which the token was found.
     * @throws IOException if one of the write method failed
     */
    public void writeCouple(String term, int reviewID) throws
            IOException{
        writeInt(reviewID);
        writeString(term + IndexWriter.INTERMEDIATE_SEPARATOR);
    }

    /**
     * Write given string to given file in byte format.
     * @param str: string to write.
     * @throws IOException if write action failed
     */
    public void writeString(String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        bos.write(bytes);
        writerIndex += bytes.length;
    }

    public void writeShort(short value) throws IOException{
        bos.write(ByteBuffer.allocate(Short.BYTES).putShort(value).array());
        writerIndex += Short.BYTES;
    }

    public void writeInt(int value) throws IOException{
        bos.write(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
        writerIndex += Integer.BYTES;
    }

    public void writeLong(long value) throws IOException {
        bos.write(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
        writerIndex += Long.BYTES;
    }

//    /**
//     * Encode the specified int as a variable length integer into the supplied OutputStream.
//     * @param value: the int value.
//     * @throws IOException: if the value cannot be written to the output file.
//     */
//    public void writeVInt(int value) throws IOException {
//        if (value > 0x0FFFFFFF || value < 0) writer.write((byte)(0x80 | ((value >>> 28))));
//        if (value > 0x1FFFFF || value < 0)   writer.write((byte)(0x80 | ((value >>> 21) & 0x7F)));
//        if (value > 0x3FFF || value < 0)     writer.write((byte)(0x80 | ((value >>> 14) & 0x7F)));
//        if (value > 0x7F || value < 0)       writer.write((byte)(0x80 | ((value >>>  7) & 0x7F)));
//        writer.write((byte)(value & 0x7F));
//    }

    /**
     * Encode the specified int as a variable length integer into the supplied OutputStream.
     * @param value: the int value.
     * @throws IOException: if the value cannot be written to the output file.
     */
    public void writeVInt(int value) throws IOException {
        if (value > 0x0FFFFFFF || value < 0) {
            bos.write((byte)(0x80 | ((value >>> 28))));
            writerIndex++;
        }
        if (value > 0x1FFFFF || value < 0) {
            bos.write((byte)(0x80 | ((value >>> 21) & 0x7F)));
            writerIndex++;
        }
        if (value > 0x3FFF || value < 0) {
            bos.write((byte)(0x80 | ((value >>> 14) & 0x7F)));
            writerIndex++;
        }
        if (value > 0x7F || value < 0) {
            bos.write((byte)(0x80 | ((value >>>  7) & 0x7F)));
            writerIndex++;
        }
        bos.write((byte)(value & 0x7F));
        writerIndex++;
    }

    public void close() throws IOException{
        bos.flush();
        bos.close();
    }
}
