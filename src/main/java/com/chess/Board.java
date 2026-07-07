package com.chess;

import java.util.ArrayList;
import java.util.List;

import com.chess.chessMove.Move;
import com.chess.chessPiece.*;

public class Board {
    private BoardSquare[][] board;
    public boolean currentWhiteTurn = true;
    public boolean KingInCheck = false;
    public List<Piece> currentlyPinnedPieces = new ArrayList<>();
    public List<BoardSquare> squaresToBlockCheck = new ArrayList<>(); 
    public List<BoardSquare> locationOfWhitePieces = new ArrayList<>();
    public List<BoardSquare> locationOfBlackPieces = new ArrayList<>();
    public int[] WhiteKingSquareLocation = {7, 4};
    public int[] BlackKingSquareLocation = {0, 4};
    public List<Move> currentPieceLegalMoves = null;
    public List<Move> moveHistory = new ArrayList<>();
    public boolean CheckMate = false;
    public boolean StaleMate = false;

    Board(){
        this.board = new BoardSquare[8][8];
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                board[i][j] = new BoardSquare(i, j);
            }
        }
    }

    public void initialisePieces(){
       FEN fen = new FEN();
       fen.CreateBoard(this, FEN.ChessFenString);
        
    }

    public void IsCheckMate(){
        if(!KingInCheck){
            return;
        }
        if(CanAnyPieceMove(currentWhiteTurn)){
            return;
        }
        CheckMate = true;
        
        
    }

    public void StaleMate(){
        if(KingInCheck){
            return;
        }
        if(CanAnyPieceMove(currentWhiteTurn)){
            return;
        }
        StaleMate = true;
    }

    public boolean CanAnyPieceMove(boolean WhiteTurn){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                BoardSquare square = getSquare(i, j);
                if(square.isOccupied()){
                    Piece piece = square.getPiece();
                    if(piece.getColour() == (WhiteTurn ? PieceColour.WHITE : PieceColour.BLACK)){
                        if(!piece.getLegalMoves(square, this).isEmpty()){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void addPieceToSquare(Piece piece,int row, int col){
        getSquare(row, col).addPiece(piece, this);
    }


    public BoardSquare getSquare(int row, int col){
        return board[row][col];
    }

    public BoardSquare[][] getBoard() {
        return board;
    }

    public Boolean isSquareOccupied(int row, int col){
        return board[row][col].isOccupied();
    }

    public void CheckIsKingInCheck(){
        if(currentWhiteTurn){
            BoardSquare whiteKingSquare = getSquare(WhiteKingSquareLocation[0], WhiteKingSquareLocation[1]);
            ((King)whiteKingSquare.getPiece()).checkKingInCheck(whiteKingSquare, this);
        }
        else{
            BoardSquare blackKingSquare = getSquare(BlackKingSquareLocation[0],BlackKingSquareLocation[1]);
            ((King)blackKingSquare.getPiece()).checkKingInCheck(blackKingSquare, this);
        }
    } 

    public void movePiece(Move PerformMove) {
        PerformMove.execute(this);
        CheckIsKingInCheck();
        IsCheckMate();
        StaleMate();
    }


    public void UndoMove(){
        if(moveHistory.isEmpty()) return;
        Move lastMove = moveHistory.remove(moveHistory.size() - 1);
        lastMove.undo(this);
        CheckIsKingInCheck();
        CheckMate = false;
        StaleMate = false;
    }

    public void resetPinPieceList() {

        for(Piece pinnedPiece : currentlyPinnedPieces) {
            pinnedPiece.removePin();        
        }
        currentlyPinnedPieces.clear();
    }


    public void UpdateKingSquareLocation(BoardSquare targetSquare, Piece pieceMoved){
        if(pieceMoved.getName().equals("King")){
            if(pieceMoved.getColour() == PieceColour.WHITE){
                WhiteKingSquareLocation[0] = targetSquare.getRow();
                WhiteKingSquareLocation[1] = targetSquare.getCol();
            } else {
                BlackKingSquareLocation[0] = targetSquare.getRow();
                BlackKingSquareLocation[1] = targetSquare.getCol();
            }
        }
    }

    
}
