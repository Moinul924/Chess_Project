package com.chess;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chess.chessMove.CastlingMove;
import com.chess.chessMove.EnPassantMove;
import com.chess.chessMove.Move;
import com.chess.chessMove.PawnPromotionMove;
import com.chess.chessPiece.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController // Notice this is RESTController, not just Controller
@RequestMapping("/api")
public class GameController {

    private Board gameBoard = new Board(); 
    private Engine engine = new Engine(gameBoard);
    private BoardSquare selectedSquare = null;
    private BoardSquare targetSquare = null;
    


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


    @PostMapping("/click")
    public List<Move> handlePieceClick(@RequestParam int row, @RequestParam int col, @RequestParam String name) {
        System.out.println("--- NEW CLICK RECEIVED FROM BROWSER ---");
        System.out.println("Piece: " + name);
        System.out.println("Coordinates: Row " + row + ", Col " + col); 
        BoardSquare clickedSquare = gameBoard.getSquare(row, col);
        selectedSquare = clickedSquare;

        gameBoard.currentPieceLegalMoves = clickedSquare.getPiece().getLegalMoves(clickedSquare, gameBoard);
        return gameBoard.currentPieceLegalMoves;
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
        for(Move LegalSquare : gameBoard.currentPieceLegalMoves){
            BoardSquare endSquare = LegalSquare.getEndSquare();
            if(endSquare== targetSquare){
                gameBoard.movePiece(LegalSquare); 
                return true;
            }
        }  
        return false;
    }


    @GetMapping("/castle")
    public Move IsLastMoveCastlingMove() {
        System.out.println("--- CASTLING MOVE CHECK ---");

        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }

        Move lastMove = gameBoard.moveHistory.getLast();
        if(lastMove instanceof CastlingMove){
            return lastMove;
        }

        return null;
        
    }


    @GetMapping("/promotion")
    public Move IsLastMovePawnPromotion(){

        System.out.println("--- PAWN PROMOTION MOVE CHECK ---");

        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }
        
        Move lastMove = gameBoard.moveHistory.getLast();
        if(lastMove instanceof PawnPromotionMove){
            return (PawnPromotionMove)lastMove;
        }

        return null;


    }

    @GetMapping("/EnPassant")
    public Move IsLastMoveEnPassant(){
        System.out.println("--- EnPassant MOVE CHECK ---");

        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }
        
        Move lastMove = gameBoard.moveHistory.getLast();
        if(lastMove instanceof EnPassantMove){
            return (EnPassantMove)lastMove;
        }

        return null;

    }


    @PostMapping("/promote_for_user")
    public void handlePawnPromotion(@RequestParam int row, @RequestParam int col, @RequestParam String newPiece) {
        System.out.println("--- PAWN PROMOTED TO " + newPiece + " ---");
        
        BoardSquare currentPawnSquare = gameBoard.getSquare(row, col);
        Piece currentPawn = currentPawnSquare.getPiece();
        PieceColour Pawncolor = currentPawn.getColour();
        Move LastMove = gameBoard.moveHistory.getLast();
        
        Piece NewPromotionPiece; 
        
        // Add the new piece based on what the user clicked
        if (newPiece.equals("Queen")) NewPromotionPiece = new Queen(Pawncolor);
        else if (newPiece.equals("Rook")) NewPromotionPiece = new Rook(Pawncolor);
        else if (newPiece.equals("Bishop")) NewPromotionPiece = new Bishop(Pawncolor);
        else if (newPiece.equals("Knight")) NewPromotionPiece = new Knight(Pawncolor);
        else NewPromotionPiece = null;

        PawnPromotionMove move = new PawnPromotionMove(LastMove.getStartSquare(), currentPawnSquare,currentPawn, NewPromotionPiece);
        gameBoard.movePiece(move);  
    }



    @PostMapping("/undo")
    public Move handleUndoMove() {
        System.out.println("--- UNDO MOVE ---");
        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }
        Move lastMove = gameBoard.moveHistory.getLast();
        gameBoard.UndoMove();
        return lastMove;
    }

    @PostMapping("/EngineMove")
    public Move handleEngineMove() {    
        System.out.println("--- ENGINE MOVE ---");
        
        // Move randomMove = engine.getRandomMove();
        // if (randomMove != null) {
        //     gameBoard.movePiece(randomMove);
        //     return randomMove;
        // }
        // return null;
        Move bestMove = engine.getMoveUsingMinMax(3); // You can adjust the depth as needed
        if (bestMove != null) {
            gameBoard.movePiece(bestMove);
            return bestMove;
        }
        return null;


    }



}