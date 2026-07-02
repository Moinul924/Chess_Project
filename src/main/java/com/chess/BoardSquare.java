package com.chess;
import com.chess.chessPiece.*;

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

    public void removePiece(Board board){
        if(piece.getColour() == PieceColour.WHITE){
            board.locationOfWhitePieces.remove(this);
        } else {
            board.locationOfBlackPieces.remove(this);
        }
        piece = null;
    }
    public void addPiece(Piece piece,Board board){
        this.piece = piece;
        if(piece.getColour() == PieceColour.WHITE){
            board.locationOfWhitePieces.add(this);
        } else {
            board.locationOfBlackPieces.add(this);
        }
    }

    public Piece getPiece() {
        return piece;
    }
    
}
