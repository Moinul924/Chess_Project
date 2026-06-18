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
    public int[] WhiteKingSquareLocation = new int[2];
    public int[] BlackKingSquareLocation = new int[2];
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
        // Pawns
        for(int i = 0; i < 8; i++){
            getSquare(1, i).addPiece(new Pawn(PieceColour.WHITE));
            getSquare(6, i).addPiece(new Pawn(PieceColour.BLACK));
        }
        getSquare(2, 4).addPiece(new Pawn(PieceColour.BLACK));
        
        // Kings
        getSquare(0, 4).addPiece(new King(PieceColour.WHITE));
        WhiteKingSquareLocation[0] = 0;
        WhiteKingSquareLocation[1] = 4;
        getSquare(7, 4).addPiece(new King(PieceColour.BLACK));
        BlackKingSquareLocation[0] = 7;
        BlackKingSquareLocation[1] = 4;
        // Queens
        getSquare(0, 3).addPiece(new Queen(PieceColour.WHITE));
        getSquare(7, 3).addPiece(new Queen(PieceColour.BLACK));
        // Rooks
        getSquare(0, 0).addPiece(new Rook(PieceColour.WHITE));
        getSquare(0, 7).addPiece(new Rook(PieceColour.WHITE));
        getSquare(7, 0).addPiece(new Rook(PieceColour.BLACK));
        getSquare(7, 7).addPiece(new Rook(PieceColour.BLACK));
        // Knights
        getSquare(0, 1).addPiece(new Knight(PieceColour.WHITE));
        getSquare(0, 6).addPiece(new Knight(PieceColour.WHITE));
        getSquare(7, 1).addPiece(new Knight(PieceColour.BLACK));
        getSquare(7, 6).addPiece(new Knight(PieceColour.BLACK));
        // Bishops
        getSquare(0, 2).addPiece(new Bishop(PieceColour.WHITE));
        getSquare(0, 5).addPiece(new Bishop(PieceColour.WHITE));
        getSquare(7, 2).addPiece(new Bishop(PieceColour.BLACK));
        getSquare(7, 5).addPiece(new Bishop(PieceColour.BLACK));
        

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
        resetPinPieceList();
        currentWhiteTurn = !currentWhiteTurn;
        CheckIsKingInCheck();
        IsCheckMate();
        StaleMate();

    }

    public void CheckKingAction(Piece piece,BoardSquare fromSquare, BoardSquare toSquare){
        if(!piece.getName().equals("King")){
           return;
        }

        ((King)piece).PieceMoved = true;
        
        if(Math.abs(toSquare.getCol() - fromSquare.getCol()) == 2){
            
        }
        
    }

    public void resetPinPieceList() {

        for(Piece pinnedPiece : currentlyPinnedPieces) {
            pinnedPiece.removePin();        
        }
        currentlyPinnedPieces.clear();
    }


    
}
