package com.chess.chessMove;

import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessPiece.*;

public class Move {
    protected BoardSquare startSquare;
    protected BoardSquare endSquare;
    protected Piece pieceMoved;
    

    public Move(BoardSquare start, BoardSquare end, Piece piece) {
        this.startSquare = start;
        this.endSquare = end;
        this.pieceMoved = piece;
    }

    public BoardSquare getEndSquare() {
        return endSquare;
    }

    public BoardSquare getStartSquare() {
        return startSquare;
    }

    public Piece getPiece(){
        return pieceMoved;
    }

    public void SetKingAndRookPieceMovedToTrue() {
        if(pieceMoved.getName().equals("Rook")){
            ((Rook)pieceMoved).PieceMoved = true;
        }
        else if(pieceMoved.getName().equals("King")){
            ((King)pieceMoved).PieceMoved = true;
        }
    }

    private void UpdateKingSquareLocation(Board board){
        if(pieceMoved.getName().equals("King")){
            if(pieceMoved.getColour() == PieceColour.WHITE){
                board.WhiteKingSquareLocation[0] = endSquare.getRow();
                board.WhiteKingSquareLocation[1] = endSquare.getCol();
            } else {
                board.BlackKingSquareLocation[0] = endSquare.getRow();
                board.BlackKingSquareLocation[1] = endSquare.getCol();
            }
        }
    }

    protected void performNormalMove(Board board) {
        startSquare.removePiece();
        if (endSquare.isOccupied()) {
            endSquare.removePiece();
        }
        endSquare.addPiece(pieceMoved);
        SetKingAndRookPieceMovedToTrue();
        UpdateKingSquareLocation(board);
    }

    public void execute(Board board) {
        performNormalMove(board);
        board.moveHistory.add(this);
    }


    
}
