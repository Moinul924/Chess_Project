package com.chess.chessPiece;

import java.util.ArrayList;
import java.util.List;
import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessMove.*;

public class King extends Piece {

    public boolean PieceMoved = false;

    private int[][] moveOffsets = {
        {1,0}, {-1,0}, {0,1}, {0,-1},  
        {1,1}, {1,-1}, {-1,1}, {-1,-1} 
    };

    private int[][] slidingOffsets = {
        {1,0}, {-1,0}, {0,1}, {0,-1},  
        {1,1}, {1,-1}, {-1,1}, {-1,-1} 
    };

    private int[][] knightOffsets = {
        {2,1}, {2,-1}, {-2,1}, {-2,-1},
        {1,2}, {1,-2}, {-1,2}, {-1,-2}
    };

    public King(PieceColour KingColour) {
        super(KingColour, "King");
    }

    @Override
    public int[][] getMoveOffsets() {
        return moveOffsets;
    }

    /**
     * Scans the board from the King's perspective to find Checks, Double Checks, and Pins.
     * Updates the board's state arrays directly.
     */
    public void checkKingInCheck(BoardSquare kingCurrentSquare, Board board) {
        board.KingInCheck = false;
        board.squaresToBlockCheck.clear();
        board.resetPinPieceList(); // Ensure old pins are cleared before recalculating

        int kRow = kingCurrentSquare.getRow();
        int kCol = kingCurrentSquare.getCol();
        List<BoardSquare> attackers = new ArrayList<>();

        // 1. Ray-cast outward to find sliding attackers (Queen, Rook, Bishop) and Pins
        for (int[] dir : slidingOffsets) {
            List<BoardSquare> currentRay = new ArrayList<>();
            Piece friendlyPieceInRay = null;
            boolean isOrthogonal = (dir[0] == 0 || dir[1] == 0);

            for (int step = 1; step < 8; step++) {
                int r = kRow + (dir[0] * step);
                int c = kCol + (dir[1] * step);

                if (!isWithinBounds(r, c)) break;

                BoardSquare square = board.getSquare(r, c);
                currentRay.add(square);

                if (board.isSquareOccupied(r, c)) {
                    Piece piece = square.getPiece();

                    if (piece.getColour() == this.getColour()) {
                        if (friendlyPieceInRay == null) {
                            friendlyPieceInRay = piece; // First friendly piece, might be pinned
                        } else {
                            break; // Two friendly pieces block any possible attack or pin. Stop looking.
                        }
                    } else {
                        // Enemy piece encountered! Let's see if it can attack us.
                        String name = piece.getName();
                        boolean canSlideAttack = (isOrthogonal && (name.equals("Rook") || name.equals("Queen"))) ||
                                                 (!isOrthogonal && (name.equals("Bishop") || name.equals("Queen")));

                        if (canSlideAttack) {
                            if (friendlyPieceInRay == null) {
                                // Direct Check!
                                board.KingInCheck = true;
                                attackers.add(square);
                                board.squaresToBlockCheck.addAll(currentRay); 
                            } else {
                                // It's a Pin! The enemy is attacking, but our piece is in the way.
                                int[][] pinDir = {{dir[0], dir[1]}, {-dir[0], -dir[1]}};
                                friendlyPieceInRay.setPinDirection(pinDir);
                                board.currentlyPinnedPieces.add(friendlyPieceInRay);
                            }
                        }
                        break; // Stop looking further in this direction once we hit any enemy piece
                    }
                }
            }
        }

        // 2. Check Knights (Knights jump, so no rays needed)
        for (int[] offset : knightOffsets) {
            int r = kRow + offset[0];
            int c = kCol + offset[1];
            if (isWithinBounds(r, c) && board.isSquareOccupied(r, c)) {
                Piece piece = board.getSquare(r, c).getPiece();
                if (piece.getColour() != this.getColour() && piece.getName().equals("Knight")) {
                    board.KingInCheck = true;
                    attackers.add(board.getSquare(r, c));
                    board.squaresToBlockCheck.add(board.getSquare(r, c)); // Can only block a Knight by capturing it
                }
            }
        }

        // 3. Check Pawns
        int pawnDirection = (getColour() == PieceColour.WHITE) ? 1 : -1;
        int[][] pawnAttacks = {{pawnDirection, 1}, {pawnDirection, -1}};
        for (int[] offset : pawnAttacks) {
            int r = kRow + offset[0];
            int c = kCol + offset[1];
            if (isWithinBounds(r, c) && board.isSquareOccupied(r, c)) {
                Piece piece = board.getSquare(r, c).getPiece();
                if (piece.getColour() != this.getColour() && piece.getName().equals("Pawn")) {
                    board.KingInCheck = true;
                    attackers.add(board.getSquare(r, c));
                    board.squaresToBlockCheck.add(board.getSquare(r, c)); // Can only block a Pawn by capturing it
                }
            }
        }

        // 4. Handle Double Check
        // If there are 2 or more attackers, the King MUST move. You cannot block two pieces at once.
        if (attackers.size() > 1) {
            board.squaresToBlockCheck.clear();
        }
    }

    /**
     * Pure query: Simply returns true if the square is controlled by an enemy piece.
     * Does NOT modify pins or board state. Used for King moves and Castling.
     */
    public boolean isSquareAttacked(int targetRow, int targetCol, Board board) {
        PieceColour defendingColour = this.getColour();

        // 1. Sliding pieces & Enemy King
        for (int[] dir : slidingOffsets) {
            boolean isOrthogonal = (dir[0] == 0 || dir[1] == 0);
            for (int step = 1; step < 8; step++) {
                int r = targetRow + (dir[0] * step);
                int c = targetCol + (dir[1] * step);

                if (!isWithinBounds(r, c)) break;

                if (board.isSquareOccupied(r, c)) {
                    Piece piece = board.getSquare(r, c).getPiece();
                    String name = piece.getName();
                    if (piece.getColour() == defendingColour){
                        if (piece.getName().equals("King")) {
                            continue; // Ignore our own King for attack purposes
                        }
                        break; // Blocked by our own piece
                    } 

                    
                    if ((isOrthogonal && (name.equals("Rook") || name.equals("Queen"))) ||
                        (!isOrthogonal && (name.equals("Bishop") || name.equals("Queen")))) {
                        return true;
                    }
                    if (name.equals("King") && step == 1) {
                        return true; // Enemy King controls adjacent squares
                    }
                    break; // Blocked by some other enemy piece (like a pawn or knight)
                }
            }
        }

        // 2. Knights
        for (int[] offset : knightOffsets) {
            int r = targetRow + offset[0];
            int c = targetCol + offset[1];
            if (isWithinBounds(r, c) && board.isSquareOccupied(r, c)) {
                Piece piece = board.getSquare(r, c).getPiece();
                if (piece.getColour() != defendingColour && piece.getName().equals("Knight")) {
                    return true;
                }
            }
        }

        // 3. Pawns
        int pawnDirection = (defendingColour == PieceColour.WHITE) ? 1 : -1;
        int[][] pawnAttacks = {{pawnDirection, 1}, {pawnDirection, -1}};
        for (int[] offset : pawnAttacks) {
            int r = targetRow + offset[0];
            int c = targetCol + offset[1];
            if (isWithinBounds(r, c) && board.isSquareOccupied(r, c)) {
                Piece piece = board.getSquare(r, c).getPiece();
                if (piece.getColour() != defendingColour && piece.getName().equals("Pawn")) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean checkCastlingSquareForCastling(int Row, int Col, Board board) {
        if(PieceMoved || board.KingInCheck) return false;

        BoardSquare CastlingSquare = board.getSquare(Row, Col);
        if(CastlingSquare.isOccupied()) return false;
        
        int[] KingSquareLocation = (getColour() == PieceColour.WHITE) ? board.WhiteKingSquareLocation : board.BlackKingSquareLocation;
        
        int difCol = Col - KingSquareLocation[1];
        int RookCol = difCol > 0 ? 7 : 0;
        int step = difCol < 0 ? 1 : -1;
        BoardSquare RookSquare = board.getSquare(Row, RookCol);
        
        if (RookSquare.isOccupied()) {
            Piece RookPiece = RookSquare.getPiece();
            if (RookPiece.getName().equals("Rook") && RookPiece.getColour() == getColour()) {
                if (((Rook)RookPiece).PieceMoved) return false;
                
                // Ensure squares between King and Rook are empty
                for(int c = Math.min(RookCol, KingSquareLocation[1]) + 1; c < Math.max(RookCol, KingSquareLocation[1]); c++){
                    if(board.isSquareOccupied(Row, c)) return false;
                }
                
                // Check if castling path is under attack (using our new clean method!)
                if (isSquareAttacked(Row, Col + step, board)) return false;
                if (isSquareAttacked(Row, Col, board)) return false;
                
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Move> getLegalMoves(BoardSquare currentSquare, Board board) {  
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        }  
        List<Move> legalMoves = new ArrayList<>();
        int currentRow = currentSquare.getRow();
        int currentCol = currentSquare.getCol();
    
        for (int[] offset : moveOffsets) {
            int newRow = currentRow + offset[0];
            int newCol = currentCol + offset[1];
    
            if (!isWithinBounds(newRow, newCol)) continue;
    
            if(board.isSquareOccupied(newRow, newCol)){
                if(getColour() == board.getSquare(newRow, newCol).getPiece().getColour()) continue; 
            }
            
            // Simplified check using our new pure boolean method
            if (!isSquareAttacked(newRow, newCol, board)) {
                BoardSquare targetSquare = board.getSquare(newRow, newCol);
                Move move = new Move(currentSquare, targetSquare, this);
                legalMoves.add(move);
            }
        }
    
        if(checkCastlingSquareForCastling(currentRow, currentCol-2, board)){
            BoardSquare targetSquare = board.getSquare(currentRow, currentCol-2);
            CastlingMove move = new CastlingMove(currentSquare, targetSquare, this, board);
            legalMoves.add(move);
        }
        if(checkCastlingSquareForCastling(currentRow, currentCol+2, board)){
            BoardSquare targetSquare = board.getSquare(currentRow, currentCol+2);
            CastlingMove move = new CastlingMove(currentSquare, targetSquare, this, board);
            legalMoves.add(move);
        }
        return legalMoves;
    }
}