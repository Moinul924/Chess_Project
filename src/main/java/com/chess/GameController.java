package com.chess;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileOutputStream;
import java.io.IOException;

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

    private final Map<String, GameSession> games = new ConcurrentHashMap<>();

    private GameSession getGame(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("gameId is required");
        }
        return games.computeIfAbsent(gameId, ignored -> new GameSession());
    }

    // When your JavaScript asks for "/api/board", Spring Boot intercepts it here
    @PostMapping ("/start-new-game")
    public boolean startNewGame(@RequestParam String gameId){
        getGame(gameId).startNewGame();
        return true;
    }

    @PostMapping("/close-game")
    public void closeGame(@RequestParam String gameId) {
        games.remove(gameId);
        System.out.println("Game with ID " + gameId + " has been closed.");
        System.out.println("Current number of games: " + games.size());
    }

    @GetMapping("/board")
    public Board getBoardState(@RequestParam String gameId) {
        // Spring Boot is so smart that when you return a Java object here,
        // it automatically translates it into JSON for your JavaScript to read!
        return getGame(gameId).gameBoard; 
    }

    @GetMapping("/current-turn")
    public boolean getCurrentTurn(@RequestParam String gameId) {
        return getGame(gameId).gameBoard.currentWhiteTurn;
    }

    @GetMapping ("/reset")
    public boolean resetGame(@RequestParam String gameId) {
        getGame(gameId).gameBoard.resetBoard();
        return true;
    }



    @PostMapping("/click")
    public List<Move> handlePieceClick(@RequestParam String gameId, @RequestParam int row, @RequestParam int col, @RequestParam String name) {
        // System.out.println("--- NEW CLICK RECEIVED FROM BROWSER ---");
        // System.out.println("Piece: " + name);
        // System.out.println("Coordinates: Row " + row + ", Col " + col); 
        GameSession game = getGame(gameId);
        BoardSquare clickedSquare = game.gameBoard.getSquare(row, col);
        game.selectedSquare = clickedSquare;

        game.gameBoard.currentPieceLegalMoves = clickedSquare.getPiece().getLegalMoves(clickedSquare, game.gameBoard);
        return game.gameBoard.currentPieceLegalMoves;
    }

    @PostMapping("/moved")
    public boolean handleCheckPieceMove(@RequestParam String gameId, @RequestParam int row, @RequestParam int col, @RequestParam String name) {
        // System.out.println("--- PIECE MOVED ---");
        // System.out.println("Piece: " + name);
        // System.out.println("New Coordinates: Row " + row + ", Col " + col); 
        GameSession game = getGame(gameId);
        game.targetSquare = game.gameBoard.getSquare(row, col);


        if (game.targetSquare == game.selectedSquare) {
            
            return false; 
        }
        for(Move LegalSquare : game.gameBoard.currentPieceLegalMoves){
            BoardSquare endSquare = LegalSquare.getEndSquare();
            if(endSquare== game.targetSquare){
                game.gameBoard.movePiece(LegalSquare); 
                return true;
            }
        }  
        return false;
    }

    public void storeTranspositionTable(@RequestParam String gameId) {
        BoardSquare[][] board = getGame(gameId).gameBoard.getBoard();
        try(FileOutputStream outputStream = new FileOutputStream("src/main/resources/static/transpositionTableRecords.txt", true)) {
            for(int row = 0; row < 8; row++) {
                for(int col = 0; col < 8; col++) {
                    BoardSquare square = board[row][col];
                    long[][] zobristNumbers = square.getZobristNumbers();
                    for(int colour = 0; colour < 2; colour++) {
                        for(int pieceType = 0; pieceType < 6; pieceType++) {
                            outputStream.write((zobristNumbers[colour][pieceType] + "\n").getBytes());
                        }
                    }
                    outputStream.write("\n".getBytes());
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing transposition table: " + e.getMessage());
        }
        
    }


    @GetMapping("/castle")
    public Move IsLastMoveCastlingMove(@RequestParam String gameId) {
        // System.out.println("--- CASTLING MOVE CHECK ---");

        Board gameBoard = getGame(gameId).gameBoard;
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
    public Move IsLastMovePawnPromotion(@RequestParam String gameId){

        // System.out.println("--- PAWN PROMOTION MOVE CHECK ---");

        Board gameBoard = getGame(gameId).gameBoard;
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
    public Move IsLastMoveEnPassant(@RequestParam String gameId){
        // System.out.println("--- EnPassant MOVE CHECK ---");

        Board gameBoard = getGame(gameId).gameBoard;
        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }
        
        Move lastMove = gameBoard.moveHistory.getLast();
        if(lastMove instanceof EnPassantMove){
            return (EnPassantMove)lastMove;
        }

        return null;

    }


    @PostMapping("/promote-for-user")
    public void handlePawnPromotion(@RequestParam String gameId, @RequestParam int row, @RequestParam int col, @RequestParam String newPiece) {
        System.out.println("--- PAWN PROMOTED TO " + newPiece + " ---");
        GameSession game = getGame(gameId);
        Board gameBoard = game.gameBoard;
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
    public Move handleUndoMove(@RequestParam String gameId) {
        // System.out.println("--- UNDO MOVE ---");
        Board gameBoard = getGame(gameId).gameBoard;
        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }
        Move lastMove = gameBoard.moveHistory.getLast();
        gameBoard.UndoMove();
        return lastMove;
    }

    @PostMapping("/EngineMove")
    public Move handleEngineMove(@RequestParam String gameId) {    
        // System.out.println("--- ENGINE MOVE ---");
        GameSession game = getGame(gameId);
        // Move randomMove = engine.getRandomMove();
        // if (randomMove != null) {
        //     gameBoard.movePiece(randomMove);
        //     return randomMove;
        // }
        // return null;
        Move bestMove = game.engine.getBestMove(4); // You can adjust the depth as needed
        if (bestMove != null) {
            game.gameBoard.movePiece(bestMove);
            return bestMove;
        }
        return null;


    }

    @PostMapping("/load-fen")
    public boolean loadFenString(@RequestParam String gameId, @RequestParam String fen) {
        // System.out.println("--- LOADING NEW FEN ---");
        try {
            Board newBoard = new Board();
            
            FEN fenParser = new FEN();
            fenParser.CreateBoard(newBoard, fen);
            
            // Replace only the requested session's board and engine.
            GameSession game = getGame(gameId);
            game.selectedSquare = null;
            game.targetSquare = null;
            game.gameBoard = newBoard;
            game.engine = new Engine(newBoard); 
            
            return true; 
        } catch (Exception e) {
            System.out.println("Failed to load FEN: " + e.getMessage());
            return false; 
        }
    }



}