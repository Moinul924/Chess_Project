package com.chess;

import com.chess.chessPiece.*;

public class FEN {

    // Example: "RNBQKBNR/PPPPPPPP/8/8/8/8/pppppppp/rnbqkbnr I0"
    public static String ChessFenString = "RNBQKBNR/PPPPPPPP/8/8/8/8/pppppppp/rnbqkbnr I0";

    private Piece createPieceFromChar(char c) {
        switch (c) {
            case 'K': return new King(PieceColour.WHITE);
            case 'P': return new Pawn(PieceColour.WHITE);
            case 'N': return new Knight(PieceColour.WHITE);
            case 'B': return new Bishop(PieceColour.WHITE);
            case 'R': return new Rook(PieceColour.WHITE);
            case 'Q': return new Queen(PieceColour.WHITE);
            case 'k': return new King(PieceColour.BLACK);
            case 'p': return new Pawn(PieceColour.BLACK);
            case 'n': return new Knight(PieceColour.BLACK);
            case 'b': return new Bishop(PieceColour.BLACK);
            case 'r': return new Rook(PieceColour.BLACK);
            case 'q': return new Queen(PieceColour.BLACK);
            default: return null; 
        }
    }

    public void CreateBoard(Board board, String fenString) {
        if (fenString == null || fenString.trim().isEmpty()) {
            throw new IllegalArgumentException("FEN string cannot be null or empty.");
        }

        String[] parts = fenString.trim().split(" ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid FEN format. Expected layout and metadata separated by space. Got: " + fenString);
        }

        String boardLayout = parts[0];
        String metadata = parts[1];

        if (metadata.length() != 2) {
            throw new IllegalArgumentException("Invalid FEN metadata length. Expected exactly 2 characters (Turn + Perspective). Got: " + metadata);
        }

        char turnIndicator = metadata.charAt(0);
        char perspectiveIndicator = metadata.charAt(1);

        if (turnIndicator == 'I') {
            board.currentWhiteTurn = true;
        } else if (turnIndicator == 'O') {
            board.currentWhiteTurn = false;
        } else {
            throw new IllegalArgumentException("Invalid turn indicator. Expected 'I' or 'O'. Got: " + turnIndicator);
        }

        // Handle the perspective (Flipping)
        if (perspectiveIndicator == '1') {
            boardLayout = flipFENBoard(boardLayout);
        } else if (perspectiveIndicator != '0') {
            throw new IllegalArgumentException("Invalid perspective indicator. Expected '0' or '1'. Got: " + perspectiveIndicator);
        }

        int row = 0;
        int col = 0;

        for (char c : boardLayout.toCharArray()) {
            Piece piece = createPieceFromChar(c); // Create a unique piece here!
            
            if (piece != null) { // Replaces: if (FENStringSymbolMap.containsKey(c))
                if (col >= 8 || row >= 8) {
                    throw new IllegalArgumentException("FEN string contains too many pieces for a standard 8x8 board.");
                }
                
                if (c == 'K') {
                    checkIfKingInStartingSquare(row, col, (King) piece);
                    board.WhiteKingSquareLocation[0] = row;
                    board.WhiteKingSquareLocation[1] = col;
                } else if (c == 'k') {
                    checkIfKingInStartingSquare(row, col, (King) piece);
                    board.BlackKingSquareLocation[0] = row;
                    board.BlackKingSquareLocation[1] = col;
                }
                
                board.addPieceToSquare(piece, row, col);
                col++;
            } 
            else if (c == '/') {
                row++;
                col = 0; // Reset column for the new row
                if (row > 7) {
                    throw new IllegalArgumentException("FEN string contains too many rows (ranks). Max is 8.");
                }
            } 
            else if (Character.isDigit(c)) {
                int emptySquares = Character.getNumericValue(c);
                col += emptySquares;
                if (col > 8) {
                    throw new IllegalArgumentException("FEN string rank exceeds 8 columns at row " + row);
                }
            } 
            else {
                throw new IllegalArgumentException("Unrecognized character in FEN string: " + c);
            }
        }
    }

    public void checkIfKingInStartingSquare(int row, int col, King piece) {
        if (piece.getColour() == PieceColour.WHITE && row != 0 && col != 4
         || piece.getColour() == PieceColour.BLACK && row != 7 && col != 4) {
            piece.PieceMoved = true;
            piece.moveCount++;
        }
    }

    private String flipFENBoard(String boardLayout) {
        return new StringBuilder(boardLayout).reverse().toString();
    }
}