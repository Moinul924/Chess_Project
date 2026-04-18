package com.chess.chessMove;

import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessPiece.*;

public class CastlingMove extends Move {

    protected BoardSquare RookStartSquare;
    protected BoardSquare RookEndSquare;

    public CastlingMove(BoardSquare start, BoardSquare end, Piece piece, Board board) {
        super(start, end, piece);
        FindRookSquaresForCastling(board);
    }

    public void FindRookSquaresForCastling(Board board){
        if(endSquare.getCol() > startSquare.getCol()){
            RookStartSquare = board.getSquare(startSquare.getRow(), 7);
            RookEndSquare = board.getSquare(startSquare.getRow(), endSquare.getCol()-1);
        } else {
            RookStartSquare = board.getSquare(startSquare.getRow(), 0);
            RookEndSquare = board.getSquare(startSquare.getRow(), endSquare.getCol()+1);
        }

    }

    public BoardSquare getRookStartSquare() {
        return RookStartSquare;
    }

    public BoardSquare getRookEndSquare() {
        return RookEndSquare;
    }


    @Override
    public void execute(Board board) {
        performNormalMove(board);
        Piece RookPiece = RookStartSquare.getPiece();
        RookStartSquare.removePiece();
        RookEndSquare.addPiece(RookPiece);
        SetKingAndRookPieceMovedToTrue();
        board.moveHistory.add(this); 
    }




    
}
