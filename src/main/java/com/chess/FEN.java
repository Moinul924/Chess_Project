package com.chess;
import java.util.HashMap;
import java.util.Map;


/*  Not nessart to create the chess Engine, but it is a good idea to have a FEN string 
    representation of the board for testing purposes.
    TODO: Later or never 

*/

public class FEN {

    public static String ChessFenStringW = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR - W 0 C c";
    public static String ChessFenStringB = "RNBKQBNR/PPPPPPPP/8/8/8/8/pppppppp/rnbkqbnr - W 1 C c";

    private String fenString;

    private Map<Character, Integer> FENStringSymbolMap = new HashMap<>(Map.of(
        'k',1,
        'p',2,
        'n',3,
        'b',4,
        'r',5,
        'q',6
    ));


    public FEN(Board board) {
        this.fenString = createFenString(board);
    }

    public String createFenString(Board board) {


        return "";
    }




    
}
