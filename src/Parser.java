import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    
    public static void main(String[] args) {
        
        if (args.length != 1) {
            System.err.println("Invalid arguments"+args.length);
            System.exit(1);
        }

        FileInputStream file = null;

        try {
            file = new FileInputStream(args[0]);

            if(!Parser.getMagicNumber(file).equals("Mini-PNG")) {
                System.err.println("Invalid format : a Mini-PNG file is expected");
                System.exit(1);
            }

            int[] arg = new int[3]; // array containing width | height | type
            List<char[]> data = new ArrayList<>();
            int testEof = file.read();
            boolean eof = (testEof == -1);
            boolean headerPresent = false;

            while (!eof) {
                char type = (char) testEof;

                if (type == 'H') {
                    arg = Parser.readHBlock(file);
                    headerPresent = true;
                }
                
                else if (type == 'C') {
                    Parser.readCBlock(file);
                }

                else if (type == 'D') {
                    data.add(Parser.readDBlock(file));
                }
                else {
                    System.err.println("Invalid block structure");
                    System.exit(1);
                }

                testEof = file.read();
                if (testEof == -1) { eof = true; }
            }

            if (!headerPresent) {
                System.err.println("missing header");
                System.exit(1);
            }

            int effectiveSize = 0;
            for(char[] element : data) {
                effectiveSize += element.length;
            }
            System.out.println(effectiveSize);

            if (effectiveSize != (arg[0]*arg[1])) {
                System.err.println("Invalid Mini-PNG : size of the datas does not match with the size announced in the header");
                System.exit(1);
            }

            for (char[] element : data) {
                Parser.printData(element, arg[0], arg[1], arg[2]);
            }
            System.out.println("end of file");


            
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(file != null) { file.close(); }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    // reading the magic number
    public static String getMagicNumber(FileInputStream file) {
        int c;
        String magicNumber = "";
        
        try {    
            for(int i = 0; i < 8; i++) {
                c = file.read();
                if (c == -1) {
                    System.err.println("invalid magic number");
                    System.exit(1);
                }
                magicNumber += (char)c;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return magicNumber;
    }

    // return an array containing width | height | type
    public static int[] readHBlock(FileInputStream file) {
        int[] result = new int[3];
        try {

            int size = Parser.getSize(file); // 1 byte need to be consumed

            byte[] buffer = new byte[4];

            file.read(buffer);
            result[0] = ByteBuffer.wrap(buffer).getInt();
            file.read(buffer);
            result[1] = ByteBuffer.wrap(buffer).getInt();
            result[2] = file.read();
            System.out.println("largeur :"+result[0]+" hauteur : "+result[1]+" type :"+result[2]);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void readCBlock(FileInputStream file) {
        try {

            int size = Parser.getSize(file);

            String commentaire = "";
            for (int i = 0; i < size; i++) {
                commentaire += (char) file.read();
            }

            System.out.println("commentaire :");
            System.out.println(commentaire);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static char[] readDBlock(FileInputStream file) {

        int size = Parser.getSize(file);

        char[][] bitLine = new char[size][8];

        try {
            for (int i = 0; i < size; i++) {
                int c = file.read();

                // padding the bit representation
                int paddingOffset = 8 - Integer.toBinaryString(c).toCharArray().length;
                for (int j = 0; j < paddingOffset; j++) {
                    bitLine[i][j] = '0';
                }
                for (int j = paddingOffset; j < Integer.toBinaryString(c).toCharArray().length + paddingOffset; j++) {
                    bitLine[i][j] = Integer.toBinaryString(c).toCharArray()[j - paddingOffset];
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Parser.flattenArray(bitLine);
    }

    public static void printData(char[] dataArray, int largeur, int hauteur, int type) {
        for(int i = 0; i < (dataArray.length/largeur); i++) {
            String line = "";

            for(int j = 0; j < largeur; j++) {
                if(dataArray[i*largeur + j] == '0') {
                    line += "X";
                }
                if(dataArray[i*largeur + j] == '1') {
                    line += "-";
                }
            }

            System.out.println(line);

        }
    }

    // get the 4 bytes corresponding to the size in a bloc
    public static int getSize(FileInputStream file) {
        byte[] buffer = new byte[4];
        try {
            file.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int size = ByteBuffer.wrap(buffer).getInt();
        if(size < 1) {
            System.err.println("invalid block size");
            System.exit(1);
        }
        return size;
    }

    public static char[] flattenArray(char[][] arrayToFlatten) {
        int sizeOfBlocks = arrayToFlatten[0].length;
        char[] arrayFlattened = new char[arrayToFlatten.length*sizeOfBlocks];
        for (int i = 0; i < arrayToFlatten.length; i++) {
            for (int j = 0; j < sizeOfBlocks; j++) { 
                arrayFlattened[i*sizeOfBlocks + j] = arrayToFlatten[i][j];
            }
        }
        return arrayFlattened;
    }
}