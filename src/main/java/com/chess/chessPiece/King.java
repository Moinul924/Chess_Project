package com.chess.chessPiece;

import java.util.ArrayList;
import java.util.List;
import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessMove.*;
import com.chess.PairOfData;


public class King extends Piece {

    public boolean PieceMoved = false;

    private int[][] moveOffsets = {
        {1,0}, {-1,0}, {0,1}, {0,-1},  
        {1,1}, {1,-1}, {-1,1}, {-1,-1} 
    };

    private int [][] slidingOffsets = {
        {1,0}, {-1,0}, {0,1}, {0,-1},  
        {1,1}, {1,-1}, {-1,1}, {-1,-1} 
    };

    private int [][] knightOffsets = {
        {2,1}, {2,-1}, {-2,1}, {-2,-1},
        {1,2}, {1,-2}, {-1,2}, {-1,-2}
    };

    public King(PieceColour KingColour) {
        super(KingColour, "King");
    }

    
    public boolean checkCastlingSquareForCastling(int Row, int Col, Board board) {
        if(PieceMoved || board.KingInCheck){
            return false;
        }

        BoardSquare CastlingSquare = board.getSquare(Row, Col);
        if(CastlingSquare.isOccupied()){
            return false;
        }
        
        int [] KingSquareLocation;
        if (getColour() == PieceColour.WHITE){
            KingSquareLocation = board.WhiteKingSquareLocation;     
        }else{
            KingSquareLocation = board.BlackKingSquareLocation; 
        }
        
        int difCol = Col - KingSquareLocation[1];
        int RookCol = difCol > 0 ? 7 : 0;
        int step = difCol < 0 ? 1 : -1;
        BoardSquare RookSquare = board.getSquare(Row, RookCol);
        Piece RookPiece = RookSquare.getPiece();
        if(RookSquare.isOccupied() && RookPiece.getName().equals("Rook") && RookPiece.getColour() == getColour()){
            if(((Rook)RookPiece).PieceMoved){
                return false;
            }
            for(int c = Math.min(RookCol, KingSquareLocation[1]) + 1; c < Math.max(RookCol, KingSquareLocation[1]); c++){
                if(board.isSquareOccupied(Row, c)){
                    return false;
                }
            }
            if(isTheSquareUnderAttack(Row, Col+step, board).firstData){
                return false;
            }
            if(isTheSquareUnderAttack(Row, Col, board).firstData){
                return false;
            }
            
            
            
        }
        return true;
        
    }
    
    public PairOfData<Boolean, List<int[]>> isTheSquareUnderAttack(int targetRow, int targetCol, Board board) {
        
        PairOfData<Boolean, List<int[]>> checkInfo = new PairOfData<>(false, new ArrayList<>());
        
        for (int[] direction : slidingOffsets) {
            int step = 1; 
            
            
            while (true) {
                int checkRow = targetRow + (direction[0] * step);
                int checkCol = targetCol + (direction[1] * step);
                
                if (!isWithinBounds(checkRow, checkCol)) {
                    break; 
                }
                
                if (board.isSquareOccupied(checkRow, checkCol)) {
                    Piece piece = board.getSquare(checkRow, checkCol).getPiece();
                    if(!piece.getName().equals("King")){
                        
                        if (getColour() == piece.getColour()) {
                            determineIfPiecePined(checkRow, checkCol, direction, piece, board);
                            break; 
                        }
                        
                        int [][] pieceOffsets = piece.getMoveOffsets();
                        for (int[] offset : pieceOffsets) {
                            if (offset[0] == direction[0] && offset[1] == direction[1]) {
                                if((piece.getName().equals("King") || piece.getName().equals("Pawn")) && step >1) {
                                    break;
                                }
                                checkInfo.firstData = true;
                                checkInfo.secondData.add(new int[]{direction[0], direction[1]});
                                break;
                            }
                        } 
                        break;  
                    }
                    
                }
                step++;
            }
        }
        
        for (int[] offset : knightOffsets) {
            int checkRow = targetRow + offset[0];
            int checkCol = targetCol + offset[1];
            
            if (isWithinBounds(checkRow, checkCol) && board.isSquareOccupied(checkRow, checkCol)) {
                Piece piece = board.getSquare(checkRow, checkCol).getPiece();
                if (getColour() != piece.getColour()) {
                    
                    if(piece.getName().equals("Knight")) {
                        checkInfo.firstData = true;
                        checkInfo.secondData.add(new int[]{offset[0], offset[1]});
                        break;
                    }
                }
            }
        }
        return checkInfo; 
    }
    
    
    public void checkKingInCheck(BoardSquare kingCurrentSquare ,Board board) {
        
        int currentRow = kingCurrentSquare.getRow();
        int currentCol = kingCurrentSquare.getCol();
        
        PairOfData<Boolean, List<int[]>> checkInfo = isTheSquareUnderAttack(currentRow, currentCol, board);
        
        if (checkInfo.firstData) {
            board.KingInCheck = true;
            GetSquaresToBlockCheck(currentRow, currentCol, board, checkInfo.secondData);    
        }else {
            board.KingInCheck = false;
            board.squaresToBlockCheck.clear();
        }
    }
    
    public void GetSquaresToBlockCheck(int Row, int Col, Board board,List<int[]> attackDirections) {
        if(attackDirections.size() > 1){
            return;
        }
        int step = 1; 
        while(true){
            int checkRow = Row + (attackDirections.get(0)[0] * step);
            int checkCol = Col + (attackDirections.get(0)[1] * step);
            board.squaresToBlockCheck.add(board.getSquare(checkRow, checkCol));
            if(board.isSquareOccupied(checkRow, checkCol)){
                return;
            }
            step++;
        } 
    }
    
    public void determineIfPiecePined(int Row, int Col, int [] direction, Piece pinedPiece,Board board){
        int step = 1; 
        while (true) {
            int checkRow = Row + (direction[0] * step);
            int checkCol = Col + (direction[1] * step);
            
            if (!isWithinBounds(checkRow, checkCol)) {
                break; 
            }
            
            if (board.isSquareOccupied(checkRow, checkCol)) {
                Piece piece = board.getSquare(checkRow, checkCol).getPiece();
                if (getColour() == piece.getColour()) {
                    return; 
                }
                
                if(piece.getName().equals("Knight")|| piece.getName().equals("Pawn")|| piece.getName().equals("King")) {
                    return; 
                }
                int [][] pieceOffsets = piece.getMoveOffsets();
                for (int[] offset : pieceOffsets) {
                    if (offset[0] == direction[0] && offset[1] == direction[1]) {
                        int[][] pinDirection = {{direction[0], direction[1]}, {-direction[0], -direction[1]}};
                        pinedPiece.setPinDirection(pinDirection);
                        board.currentlyPinnedPieces.add(pinedPiece);
                        return;
                        
                    }
                } 
                return; 
            }
            step++;            
        }
        
    }

    @Override
    public int[][] getMoveOffsets() {
        return moveOffsets;
    }
    
    @Override
    public List<Move> getLegalMoves(BoardSquare currentSquare , Board board) {  
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        }  
        List<Move> legalMoves = new ArrayList<>();
        int currentRow = currentSquare.getRow();
        int currentCol = currentSquare.getCol();
    
        for (int[] offset : moveOffsets) {
            int newRow = currentRow + offset[0];
            int newCol = currentCol + offset[1];
    
            if (!isWithinBounds(newRow, newCol)) {
                continue;
            }
    
            if(board.isSquareOccupied(newRow, newCol)){
                
                if(getColour() == board.getSquare(newRow, newCol).getPiece().getColour()){
                    continue; 
                }
    
            }
            
            if (!isTheSquareUnderAttack(newRow, newCol, board).firstData) {
                BoardSquare targetSquare = board.getSquare(newRow, newCol);
                Move move = new Move(currentSquare, targetSquare, this);
                legalMoves.add(move);
            }
        }
    
        if(checkCastlingSquareForCastling(currentRow, currentCol-2, board)){
            BoardSquare targetSquare = board.getSquare(currentRow, currentCol-2);
            CastlingMove move = new CastlingMove(currentSquare, targetSquare, this,board);
            legalMoves.add(move);
        }
        if(checkCastlingSquareForCastling(currentRow, currentCol+2, board)){
            BoardSquare targetSquare = board.getSquare(currentRow, currentCol+2);
            CastlingMove move = new CastlingMove(currentSquare, targetSquare, this,board);
            legalMoves.add(move);
        }
        return legalMoves;
    }
}