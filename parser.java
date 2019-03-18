import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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

            int testEof = file.read();
            boolean eof = (testEof == -1);
            int[] arg = new int[3]; // TODO : gérer si le fichier ne commence pas par un block H
            while (!eof) { // tester si le fichier ne contiennent pas de H
                char type = (char) testEof;

                if (type == 'H') { // reading H block
                    arg = Parser.readHBlock(file);
                }
                
                if (type == 'C') {
                    Parser.readCBlock(file);
                }

                if (type == 'D') {
                    Parser.printData(Parser.readDBlock(file), arg[0], arg[1], arg[2]); // TODO : concatener les resultats et print après le while
                }

                testEof = file.read();
                if (testEof == -1) { eof = true; }
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

    public static int[] readHBlock(FileInputStream file) {
        int[] result = new int[3]; // array containing width | hight | type
        try {
            
            int size = Parser.getSize(file);

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

        // TODO : vérifier qu'il y a bien autant de pixels que largeur x hauteur
        int size = Parser.getSize(file);
        System.out.println("readDblock::size :" + size);
        /* if((hauteur * largeur - size*8) != 0) {
            System.err.println("Invalid format : wrong dimensions");
            System.exit(1);
        } */
        char[][] bitLine = new char[size][8]; // TODO : gerer les tailles differentes de 8

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
        for(int i = 0; i < hauteur; i++) {
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

    public static int getSize(FileInputStream file) {
        byte[] buffer = new byte[4]; // reading size
        try {
            file.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ByteBuffer.wrap(buffer).getInt();
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