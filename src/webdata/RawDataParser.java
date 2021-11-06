package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by eranhashmo on 4/21/2021.
 */

/** Class that reads reviews from the raw data file.
 * filePath: bla.
 */
public class RawDataParser {
    static final int PID_LENGTH = 10;
    private BufferedReader reader;
    private boolean eof;

    /**
     * @param inputFile: Raw data file.
     * @throws IOException
     */
    public RawDataParser(final String inputFile) throws IOException {
        eof = false;
        if (inputFile.endsWith(".gz")) {
            GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(inputFile));
            reader = new BufferedReader(new InputStreamReader(gzip));
        } else {
            reader = new BufferedReader(new FileReader(inputFile));
        }
    }

    public List<RawReview> fileToString(int bufferSize) throws IOException {
        int fieldPointer = 0;
        int helperIndex = 0;
        int helpfulness1 = 0;
        int helpfulness2 = 0;
        int score = 0;
        String line;
        String productID = "";
        String text = "";
        ArrayList<RawReview> result = new ArrayList<>(bufferSize);

        line = reader.readLine();
        while (result.size() < bufferSize){
            if (line == null) {
                eof = true;
                return result;
            }
            if (line.startsWith("product/productId: ")) {
                fieldPointer = "product/productId: ".length();
                productID = line.substring(fieldPointer, fieldPointer + PID_LENGTH);
            }
            if (line.startsWith("review/helpfulness: ")) {
                fieldPointer = line.indexOf("review/helpfulness: ") + "review/helpfulness: ".length();
                helperIndex = line.substring(fieldPointer).indexOf("/");
                helpfulness1 = Integer.parseInt(line.substring(fieldPointer, fieldPointer + helperIndex));
                fieldPointer += helperIndex + 1;
                helpfulness2 = Integer.parseInt(line.substring(fieldPointer));
            }
            if (line.startsWith("review/score: ")) {
                fieldPointer = line.indexOf("review/score: ") + "review/score: ".length();
                score = Character.getNumericValue(line.charAt(fieldPointer));
            }
            if (line.startsWith("review/text: ")) {
                fieldPointer = line.indexOf("review/text: ") + "review/text: ".length();
                text = line.substring(fieldPointer);
                result.add(new RawReview(productID, helpfulness1, helpfulness2, score, text));
            }
            line = reader.readLine();

        }
        return result;
    }


    void close() throws IOException{
        reader.close();
    }

    boolean endOfFile() {
        return eof;
    }
}
