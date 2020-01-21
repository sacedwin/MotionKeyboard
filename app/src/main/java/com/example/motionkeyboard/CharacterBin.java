package com.example.motionkeyboard;

public class CharacterBin {
    private static String[] contents = new String[5];

    CharacterBin(){
        initialiseContents();
    }

    //A B C D E F G H I, J K L M N O P Q R, S T U V W X Y Z
    private void initialiseContents(){
        contents[0] = "0123456789";
        contents[1] = "ABCDEFGHI";
        contents[2] = "JKLMNOPQR";
        contents[3] = "STUVWXYZ";
        contents[4] = ".,!@#$%^&*()-_=+{}[]|\\:;\"'<>?/";
        //contents[5] = ""
    }

    //returns a char from a given bin and position in bin
    char getChar(int bin, int binPos){
        return contents[bin].charAt(binPos);
    }

    //returns a bin at a given position
    public String getBin(int bin){
        return contents[bin];
    }

    //returns the number of bins
    int getBinTotal(){
        return contents.length;
    }

    String binText(int bin){
        return contents[bin].charAt(0)+"-"+contents[bin].charAt(contents[bin].length()-1);
    }
}
