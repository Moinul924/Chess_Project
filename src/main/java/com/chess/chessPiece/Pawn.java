package com.chess.chessPiece;

import java.util.ArrayList;
import java.util.List;
import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.PairOfData;
import com.chess.chessMove.*;


public class Pawn extends Piece {

    
    protected int[][] whiteOffsets = { {1,0}, {2,0}, {1,1}, {1,-1} };
    protected int[][] blackOffsets = { {-1,0}, {-2,0}, {-1,1}, {-1,-1} };

    public Pawn(PieceColour PawnColour) {
        super(PawnColour, "Pawn");
    }

    
    private boolean IsMoveLegal(int startRow, int startCol, int endRow, int endCol, Board board) {
        if (!isWithinBounds(endRow, endCol)) {
            return false;
        }
        
        int direction = (getColour() == PieceColour.WHITE) ? 1 : -1;
        int PawnstartRow = (getColour() == PieceColour.WHITE) ? 1 : 6;

        int deltaRow = endRow - startRow; 
        int deltaCol = Math.abs(endCol - startCol); 
        
        if (deltaCol == 0 && deltaRow == direction) {
            return !board.isSquareOccupied(endRow, endCol);
        }
        if (deltaCol == 0 && deltaRow == direction * 2 && startRow == PawnstartRow) {
            boolean isIntermediateSquareEmpty = !board.isSquareOccupied(startRow + direction, startCol );
            boolean isDestinationEmpty = !board.isSquareOccupied(endRow, endCol);
            return isIntermediateSquareEmpty && isDestinationEmpty;
        }
        
        if (deltaCol == 1 && deltaRow == direction) {
            if (board.isSquareOccupied(endRow, endCol)) {
                return board.getSquare(endRow, endCol).getPiece().getColour() != getColour();
            }
        }

        
        
        return false;
    }
    

    public PairOfData<Boolean,BoardSquare> EnPassantCheck(int startRow, int startCol, int endRow, int endCol, Board board){

        if (board.moveHistory.isEmpty()) {
            return new PairOfData<>(false, null);
        }
        
        int direction = (getColour() == PieceColour.WHITE) ? 1 : -1;
        Move lastMove = board.moveHistory.getLast();
        if(lastMove.getPiece().getName().equals("Pawn")){
            int lastStartRow = lastMove.getStartSquare().getRow();
            int lastEndRow = lastMove.getEndSquare().getRow();
            int lastEndCol = lastMove.getEndSquare().getCol();

            boolean wasDoubleStep = Math.abs(lastEndRow - lastStartRow) == 2;

            boolean isAdjacent = (lastEndRow == startRow) && (Math.abs(lastEndCol - startCol) == 1);

            boolean isCorrectTargetSquare = (endCol == lastEndCol) && (endRow == startRow + direction);

            if (wasDoubleStep && isAdjacent && isCorrectTargetSquare) {
                BoardSquare capturedPawnSquare = lastMove.getEndSquare();
                return new PairOfData<Boolean,BoardSquare>(true,capturedPawnSquare );
            }

        }
        return new PairOfData<Boolean,BoardSquare>(false, null);

    }
    
    @Override
    public int[][] getMoveOffsets() {
        return new int[][] { 
            {1,0}, {2,0}, {1,1}, {1,-1},
            {-1,0}, {-2,0}, {-1,1}, {-1,-1} 
        };
    }

    
    
    @Override
    public List<Move> getLegalMoves(BoardSquare currentSquare, Board board) {   
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        } 
        int currentRow = currentSquare.getRow();
        int currentCol = currentSquare.getCol(); 
        List<Move> legalMoves = new ArrayList<>(); 
    
        int[][] activeOffsets = (getColour() == PieceColour.WHITE) ? whiteOffsets : blackOffsets;
    
        for (int[] offset : activeOffsets) {
            
            if (isPiecePinned) {
                if (!isMoveInPinDirection(offset, pinDirection)) {
                    continue; 
                }
            }
            int newRow = currentRow + offset[0];
            int newCol = currentCol + offset[1];
            if (IsMoveLegal(currentRow, currentCol, newRow, newCol, board)) {
                BoardSquare targetSquare = board.getSquare(newRow, newCol);
                Move move = new Move(currentSquare, targetSquare, this);
                legalMoves.add(move);   
                
            }
            if(offset[1] != 0){

                PairOfData <Boolean,BoardSquare> Check = EnPassantCheck(currentRow, currentCol,newRow,newCol,board);
                if(Check.firstData){
                    BoardSquare targetSquare = board.getSquare(newRow, newCol);
                    EnPassantMove move = new EnPassantMove(currentSquare,targetSquare,this,Check.secondData);
                    legalMoves.add(move);

                }
                
            }
        }
        
        if(board.KingInCheck){
            legalMoves.removeIf(move -> !board.squaresToBlockCheck.contains(move.getEndSquare()));
        }
    
        return legalMoves;
    }


    public List<Move> getLegalMoves(BoardSquare currentSquare, Board board, boolean generatingMove) {
        List<Move> standardMoves = getLegalMoves(currentSquare, board);
        
        if (!generatingMove) {
            return standardMoves;
        }
        //add promotion moves if the pawn reaches the last rank

        List<Move> movesWithPromotions = new ArrayList<>();
        for(Move move : standardMoves) {
            BoardSquare endSquare = move.getEndSquare();
            if(endSquare.getRow() == 0 || endSquare.getRow() == 7) {
                // Replace the standard move with promotion moves
                movesWithPromotions.add(new PawnPromotionMove(currentSquare, endSquare, this, new Queen(this.getColour())));
                movesWithPromotions.add(new PawnPromotionMove(currentSquare, endSquare, this, new Rook(this.getColour())));
                movesWithPromotions.add(new PawnPromotionMove(currentSquare, endSquare, this, new Bishop(this.getColour())));
                movesWithPromotions.add(new PawnPromotionMove(currentSquare, endSquare, this, new Knight(this.getColour())));
                continue; // Skip adding the standard move since it's replaced by promotion moves
            }
            movesWithPromotions.add(move); // Add the standard move as well
        }



        return movesWithPromotions;
    }

}
