package com.chess;
import com.chess.chessPiece.Piece;

public class BoardSquare {

    private int row;
    private int col;
    private Piece piece;

    BoardSquare(int row,int col){
        this.row = row;
        this.col = col;
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

    public void removePiece(){
        piece = null;
    }
    public void addPiece(Piece piece){
        this.piece = piece;
    }

    public Piece getPiece() {
        return piece;
    }
    
}
