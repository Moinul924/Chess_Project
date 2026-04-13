package com.chess;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController // Notice this is RESTController, not just Controller
@RequestMapping("/api")
public class GameController {

    private Board gameBoard = new Board(); 
    private BoardSquare selectedSquare = null;
    private BoardSquare targetSquare = null;
    private List<BoardSquare> currentPieceLegalMoves = null;
    private boolean CastlingMove = false;


    public GameController() {
        gameBoard.initialisePieces(); 
    }

    // When your JavaScript asks for "/api/board", Spring Boot intercepts it here
    @GetMapping("/board")
    public Board getBoardState() {
        // Spring Boot is so smart that when you return a Java object here,
        // it automatically translates it into JSON for your JavaScript to read!
        return gameBoard; 
    }

    public void movePiece(BoardSquare fromSquare, BoardSquare toSquare) {
        CastlingMove = false;
        if (toSquare.isOccupied()) {
            toSquare.removePiece();
        }
        Piece movingPiece = fromSquare.getPiece();
        fromSquare.removePiece();
        toSquare.addPiece(movingPiece);
        resetPinPieceList();
        Piece piece = toSquare.getPiece();
        if(piece.getName().equals("King")){
            ((King)piece).PieceMoved = true;
            if(piece.getColour() == PieceColour.WHITE){
                gameBoard.WhiteKingSquareLocation[0] = toSquare.getRow();
                gameBoard.WhiteKingSquareLocation[1] = toSquare.getCol();
            } else {
                gameBoard.BlackKingSquareLocation[0] = toSquare.getRow();
                gameBoard.BlackKingSquareLocation[1] = toSquare.getCol();
            }
            if(Math.abs(toSquare.getCol() - fromSquare.getCol()) == 2){
                CastlingMove = true;
                if(toSquare.getCol() > fromSquare.getCol()){
                    BoardSquare RookFromSquare = gameBoard.getSquare(fromSquare.getRow(), 7);
                    BoardSquare RookToSquare = gameBoard.getSquare(fromSquare.getRow(), toSquare.getCol()-1);
                    Piece RookPiece = RookFromSquare.getPiece();
                    RookFromSquare.removePiece();
                    RookToSquare.addPiece(RookPiece);
                    ((Rook)RookPiece).PieceMoved = true;
                } else {
                    BoardSquare RookFromSquare = gameBoard.getSquare(fromSquare.getRow(), 0);
                    BoardSquare RookToSquare = gameBoard.getSquare(fromSquare.getRow(), toSquare.getCol()+1);
                    Piece RookPiece = RookFromSquare.getPiece();
                    RookFromSquare.removePiece();
                    RookToSquare.addPiece(RookPiece);
                    ((Rook)RookPiece).PieceMoved = true;
                }
            }
        }

        if(piece.getName().equals("Rook")){
            ((Rook)piece).PieceMoved = true;
        }

        gameBoard.currentWhiteTurn = !gameBoard.currentWhiteTurn;
        if(gameBoard.currentWhiteTurn){
            BoardSquare whiteKingSquare = gameBoard.getSquare(gameBoard.WhiteKingSquareLocation[0], gameBoard.WhiteKingSquareLocation[1]);
            ((King)whiteKingSquare.getPiece()).checkKingInCheck(whiteKingSquare, gameBoard);
        }
        else{
            BoardSquare blackKingSquare = gameBoard.getSquare(gameBoard.BlackKingSquareLocation[0], gameBoard.BlackKingSquareLocation[1]);
            ((King)blackKingSquare.getPiece()).checkKingInCheck(blackKingSquare, gameBoard);
        }

    }

    public void resetPinPieceList() {
        gameBoard.currentlyPinnedPieces.clear();
    }



    @PostMapping("/click")
    public List<BoardSquare> handlePieceClick(@RequestParam int row, @RequestParam int col, @RequestParam String name) {
        System.out.println("--- NEW CLICK RECEIVED FROM BROWSER ---");
        System.out.println("Piece: " + name);
        System.out.println("Coordinates: Row " + row + ", Col " + col); 
        BoardSquare clickedSquare = gameBoard.getSquare(row, col);
        selectedSquare = clickedSquare;
        
        currentPieceLegalMoves = clickedSquare.getPiece().getLegalMoves(clickedSquare, gameBoard);
        return currentPieceLegalMoves;
    }

    @PostMapping("/moved")
    public boolean handleCheckPieceMove(@RequestParam int row, @RequestParam int col, @RequestParam String name) {
        System.out.println("--- PIECE MOVED ---");
        System.out.println("Piece: " + name);
        System.out.println("New Coordinates: Row " + row + ", Col " + col); 
        targetSquare = gameBoard.getSquare(row, col);


        if (targetSquare == selectedSquare) {
            
            return false; 
        }
        for(BoardSquare LegalSquare : currentPieceLegalMoves){
            if(LegalSquare.getRow() == row && LegalSquare.getCol() == col){
                movePiece(selectedSquare, targetSquare); 
                return true;
            }
        }  
        return false;
    }


    @GetMapping("/castle")
    public PairOfData<Boolean,int [][]> handleCastlingMove() {
        System.out.println("--- CASTLING MOVE CHECK ---");
        System.out.println("Castling Move: " + CastlingMove);
        PairOfData<Boolean, int[][]> response = new PairOfData<>(CastlingMove,new int [][] {new int[2], new int[2]});

        if(!CastlingMove){
            return response;
        }
        
        int StartRookRow = !gameBoard.currentWhiteTurn ? gameBoard.WhiteKingSquareLocation[0] : gameBoard.BlackKingSquareLocation[0];
        int StartRookCol = targetSquare.getCol() > selectedSquare.getCol() ? 7 : 0;
        int step = targetSquare.getCol() > selectedSquare.getCol() ? -1 : 1;
        int EndRookCol = targetSquare.getCol()+step;
        response.second[0][0] = StartRookRow;
        response.second[0][1] = StartRookCol;
        response.second[1][0] = StartRookRow;
        response.second[1][1] = EndRookCol;
        return response;
    }
}