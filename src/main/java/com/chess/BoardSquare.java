package com.chess;


import com.chess.chessPiece.*;

public class BoardSquare {

    
    private int row;
    private int col;
    private Piece piece;
    private final long[][] zobristNumbers = new long[2][6];


    BoardSquare(int row,int col){
        this.row = row;
        this.col = col;
        for (int colour = 0; colour < 2; colour++) {
            for (int pieceType = 0; pieceType < 6; pieceType++) {
                zobristNumbers[colour][pieceType] = Board.ZOBRIST_RANDOM.nextLong();
            }
        }
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }   

    public Boolean isOccupied(){
        return piece != null;
    }

    public void setPiece(Piece piece){
        this.piece = piece;
    }

    public Piece getPiece() {
        return piece;
    }

    public long[][] getZobristNumbers() {
        return zobristNumbers;
    }

    public long getZobristNumber(Piece piece) {
        int colourIndex = piece.getColour() == PieceColour.WHITE ? 0 : 1;
        int pieceIndex = switch (piece.getName()) {
            case "Pawn" -> 0;
            case "Knight" -> 1;
            case "Bishop" -> 2;
            case "Rook" -> 3;
            case "Queen" -> 4;
            case "King" -> 5;
            default -> throw new IllegalArgumentException("Unknown chess piece: " + piece.getName());
        };
        return zobristNumbers[colourIndex][pieceIndex];
    }
    
}
