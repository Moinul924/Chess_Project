package com.chess.chessMove;

import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessPiece.*;

public class Move {
    protected BoardSquare startSquare;
    protected BoardSquare endSquare;
    protected Piece pieceMoved;
    protected Piece pieceCaptured;
    

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


    public Piece getPieceCaptured() {
        return pieceCaptured;
    }

    public boolean goodCapture(){
        if(pieceCaptured == null){
            return false;
        }
        if(pieceMoved.getPieceValue() <= pieceCaptured.getPieceValue()){
            return true;
        }
        return false;
    }

    public boolean isCaptureMove() {
        return pieceCaptured != null;
    }

    public void SetKingAndRookPieceMovedToTrue() {
        if(pieceMoved.getName().equals("Rook")){
            ((Rook)pieceMoved).moveCount++;
            ((Rook)pieceMoved).PieceMoved = true;
        }
        else if(pieceMoved.getName().equals("King")){
            ((King)pieceMoved).moveCount++;
            ((King)pieceMoved).PieceMoved = true;
        }
    }

    public void SetKingAndRookPieceMovedToFalse() {
        if(pieceMoved.getName().equals("Rook")){
            ((Rook)pieceMoved).moveCount--;
            if(((Rook)pieceMoved).moveCount == 0){
                ((Rook)pieceMoved).PieceMoved = false;
            }
        }
        else if(pieceMoved.getName().equals("King")){
            ((King)pieceMoved).moveCount--;
            if(((King)pieceMoved).moveCount == 0){
                ((King)pieceMoved).PieceMoved = false;
            }
        }
    }

    

    protected void performNormalMove(Board board) {
        startSquare.removePiece();
        if (endSquare.isOccupied()) {
            pieceCaptured = endSquare.getPiece();
            endSquare.removePiece();
        }
        endSquare.addPiece(pieceMoved);
        SetKingAndRookPieceMovedToTrue();
        board.UpdateKingSquareLocation(endSquare, pieceMoved);
        if(pieceMoved.getName().equals("Pawn") && (endSquare.getRow() == 0 || endSquare.getRow() == 7)){
            return;
        }
        board.currentWhiteTurn = !board.currentWhiteTurn;
    }

  
    public void execute(Board board) {
        performNormalMove(board);
        board.moveHistory.add(this);
    }

    public void performNormalUndo(Board board) {
        endSquare.removePiece();
        startSquare.addPiece(pieceMoved);
        if (pieceCaptured != null) {
            endSquare.addPiece(pieceCaptured);
        }
        UndoBoardStates(board);
        board.currentWhiteTurn = !board.currentWhiteTurn;
    }

    public void undo(Board board) {
        performNormalUndo(board);
    }

    public void UndoBoardStates(Board board) {
        if(board.KingInCheck){
            board.KingInCheck = false;
        }
        if(board.CheckMate){
            board.CheckMate = false;
        }
        if(board.StaleMate){
            board.StaleMate = false;
        }
       
        board.UpdateKingSquareLocation(startSquare, pieceMoved);
        SetKingAndRookPieceMovedToFalse();

    }



    
}
