package com.chess;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chess.account.AccountService;
import com.chess.account.GameRecordService;
import com.chess.account.GameResult;
import com.chess.account.GameTermination;
import com.chess.account.OpponentType;
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
    private final GameRecordService gameRecords;
    private final AccountService accounts;
    private final MatchmakingQueue matchmaking;

    public GameController(GameRecordService gameRecords, AccountService accounts, MatchmakingQueue matchmaking) {
        this.gameRecords = gameRecords;
        this.accounts = accounts;
        this.matchmaking = matchmaking;
    }

    // What /api/pvp/state sends back, so each browser knows its colour, the names, and whether anything changed.
    public record OnlineGameState(String myColour, String whiteName, String blackName,
                                  boolean opponentJoined, int version, boolean whiteToMove, boolean gameOver) {}

    // ==========================================
    // WHO IS CALLING, AND ARE THEY ALLOWED?
    // ==========================================

    // Login stores the account id as the principal (see AccountController.login).
    private Long caller(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    // Finds a game the caller is playing in. It no longer creates a game for any id it's given.
    private GameSession getGame(String gameId, Long accountId) {
        if (gameId == null || gameId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId is required");
        }
        GameSession game = games.get(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such game");
        }
        if (!game.isPlayer(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not playing in this game");
        }
        return game;
    }

    // The core rule of multiplayer: only the account sitting in the seat whose turn it is may move.
    // Never trust the browser about whose turn it is; the board on the server decides.
    private void requireTurn(GameSession game, Long accountId) {
        if (game.mode == GameMode.ONLINE && game.blackAccountId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiting for an opponent to join");
        }
        if (!accountId.equals(game.accountToMove())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It is not your turn");
        }
    }

    // Undo, reset, load FEN and engine moves are single-player conveniences.
    private void requireNotOnline(GameSession game) {
        if (game.mode == GameMode.ONLINE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed in an online game");
        }
    }

    // Saves the game once it ends by checkmate or stalemate. LOCAL games aren't saved:
    // one account playing both sides isn't a real game between two players.
    private void recordIfFinished(GameSession game) {
        Board board = game.gameBoard;
        if (game.recorded || game.mode == GameMode.LOCAL || !(board.CheckMate || board.StaleMate)) {
            return;
        }
        game.recorded = true;

        GameResult result;
        GameTermination termination;
        if (board.CheckMate) {
            // The side whose turn it is has been checkmated, so the other side won.
            result = board.currentWhiteTurn ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
            termination = GameTermination.CHECKMATE;
        } else {
            result = GameResult.DRAW;
            termination = GameTermination.STALEMATE;
        }
        OpponentType opponent = game.mode == GameMode.ENGINE ? OpponentType.ENGINE : OpponentType.HUMAN;

        try {
            gameRecords.recordFinishedGame(game.whiteAccountId, game.blackAccountId, opponent, result, termination);
        } catch (RuntimeException e) {
            // The move itself already happened; a database problem shouldn't turn it into an error for the player.
            System.err.println("Could not save finished game: " + e.getMessage());
        }
    }

    // ==========================================
    // STARTING AND CLOSING GAMES
    // ==========================================

    // Starts (or restarts) a single-player game: ENGINE against the bot, or LOCAL pass-and-play.
    // colour is which side the player takes against the engine.
    @PostMapping ("/start-new-game")
    public boolean startNewGame(@RequestParam String gameId,
                                @RequestParam(defaultValue = "LOCAL") GameMode mode,
                                @RequestParam(defaultValue = "WHITE") PieceColour colour,
                                Authentication authentication){
        Long me = caller(authentication);
        if (mode == GameMode.ONLINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Online games are started through matchmaking (/api/pvp/queue)");
        }
        GameSession existing = games.get(gameId);
        if (existing != null && (!me.equals(existing.ownerAccountId) || existing.mode == GameMode.ONLINE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That game id belongs to another game");
        }
        games.put(gameId, new GameSession(mode, me, colour));
        return true;
    }

    @PostMapping("/close-game")
    public void closeGame(@RequestParam String gameId, Authentication authentication) {
        GameSession game = games.get(gameId);
        // Online games stay open when one player leaves or refreshes; the other player is still in them.
        if (game == null || game.mode == GameMode.ONLINE || !game.isPlayer(caller(authentication))) {
            return;
        }
        games.remove(gameId);
        System.out.println("Game with ID " + gameId + " has been closed.");
        System.out.println("Current number of games: " + games.size());
    }

    // ==========================================
    // ONLINE GAMES (two accounts)
    // ==========================================

    // "Find me an opponent." Pairs the caller with whoever has waited longest, or puts them in the queue.
    @PostMapping("/pvp/queue")
    public MatchmakingQueue.QueueStatus joinQueue(Authentication authentication) {
        return matchmaking.join(caller(authentication), this::startOnlineGame);
    }

    // A waiting browser asks this every 1.5 seconds: am I still waiting, or have I been paired?
    @GetMapping("/pvp/queue")
    public MatchmakingQueue.QueueStatus checkQueue(Authentication authentication) {
        return matchmaking.check(caller(authentication));
    }

    // Cancel button, or the page being closed while searching.
    @PostMapping("/pvp/queue/leave")
    public void leaveQueue(Authentication authentication) {
        matchmaking.leave(caller(authentication));
    }

    // Creates the online game for two paired players, with colours picked at random.
    // The SERVER picks the game id, so it can't be guessed or reused.
    private String startOnlineGame(Long playerA, Long playerB) {
        boolean aIsWhite = ThreadLocalRandom.current().nextBoolean();
        Long white = aIsWhite ? playerA : playerB;
        Long black = aIsWhite ? playerB : playerA;

        String gameId = UUID.randomUUID().toString();
        GameSession game = new GameSession(GameMode.ONLINE, white, PieceColour.WHITE);
        game.blackAccountId = black;
        games.put(gameId, game);
        return gameId;
    }

    // Each player's browser asks this every 1.5 seconds; when version changes, it reloads the board.
    // It's also how a page that was refreshed mid-game gets back into it.
    @GetMapping("/pvp/state")
    public OnlineGameState getOnlineState(@RequestParam String gameId, Authentication authentication) {
        Long me = caller(authentication);
        GameSession game = getGame(gameId, me);
        synchronized (game) {
            return stateFor(game, me);
        }
    }

    private OnlineGameState stateFor(GameSession game, Long me) {
        PieceColour colour = game.colourOf(me);
        return new OnlineGameState(
            colour == null ? null : colour.name(),
            game.whiteAccountId == null ? null : accounts.displayNameOf(game.whiteAccountId),
            game.blackAccountId == null ? null : accounts.displayNameOf(game.blackAccountId),
            game.whiteAccountId != null && game.blackAccountId != null,
            game.version,
            game.gameBoard.currentWhiteTurn,
            game.gameBoard.CheckMate || game.gameBoard.StaleMate);
    }

    // ==========================================
    // READING THE BOARD
    // ==========================================

    @GetMapping("/board")
    public Board getBoardState(@RequestParam String gameId, Authentication authentication) {
        // Spring Boot is so smart that when you return a Java object here,
        // it automatically translates it into JSON for your JavaScript to read!
        return getGame(gameId, caller(authentication)).gameBoard;
    }

    @GetMapping("/current-turn")
    public boolean getCurrentTurn(@RequestParam String gameId, Authentication authentication) {
        return getGame(gameId, caller(authentication)).gameBoard.currentWhiteTurn;
    }

    @GetMapping ("/reset")
    public boolean resetGame(@RequestParam String gameId, Authentication authentication) {
        GameSession game = getGame(gameId, caller(authentication));
        requireNotOnline(game);
        synchronized (game) {
            game.startNewGame();
        }
        return true;
    }

    @GetMapping ("/get-captured-pieces")
    public List<Move> getCapturedPieces(@RequestParam String gameId, Authentication authentication) {
        return getGame(gameId, caller(authentication)).gameBoard.getCapturedMoves();
    }

    // ==========================================
    // MAKING MOVES
    // ==========================================

    @PostMapping("/click")
    public List<Move> handlePieceClick(@RequestParam String gameId, @RequestParam int row, @RequestParam int col, @RequestParam String name,
                                       Authentication authentication) {
        Long me = caller(authentication);
        GameSession game = getGame(gameId, me);
        // Selecting a piece is part of moving, so it's also turn-checked. Otherwise the waiting player's
        // clicks would overwrite the selection of the player who is actually moving.
        synchronized (game) {
            requireTurn(game, me);   // checked inside the lock, so two requests can't both pass it
            BoardSquare clickedSquare = game.gameBoard.getSquare(row, col);
            Piece piece = clickedSquare.getPiece();
            PieceColour sideToMove = game.gameBoard.currentWhiteTurn ? PieceColour.WHITE : PieceColour.BLACK;
            // Only the side to move may pick up a piece (and clicking an empty square selects nothing).
            if (piece == null || piece.getColour() != sideToMove) {
                game.selectedSquare = null;
                game.gameBoard.currentPieceLegalMoves = List.of();
                return List.of();
            }
            game.selectedSquare = clickedSquare;
            game.gameBoard.currentPieceLegalMoves = piece.getLegalMoves(clickedSquare, game.gameBoard);
            return game.gameBoard.currentPieceLegalMoves;
        }
    }

    @PostMapping("/moved")
    public boolean handleCheckPieceMove(@RequestParam String gameId, @RequestParam int row, @RequestParam int col, @RequestParam String name,
                                        Authentication authentication) {
        Long me = caller(authentication);
        GameSession game = getGame(gameId, me);
        synchronized (game) {
            requireTurn(game, me);   // checked inside the lock, so two requests can't both pass it
            game.targetSquare = game.gameBoard.getSquare(row, col);

            if (game.targetSquare == game.selectedSquare || game.gameBoard.currentPieceLegalMoves == null) {
                return false;
            }
            for(Move LegalSquare : game.gameBoard.currentPieceLegalMoves){
                BoardSquare endSquare = LegalSquare.getEndSquare();
                if(endSquare== game.targetSquare){
                    game.gameBoard.movePiece(LegalSquare);
                    game.gameBoard.currentPieceLegalMoves = List.of();   // a selection can only be used once
                    game.version++;
                    recordIfFinished(game);
                    return true;
                }
            }
            return false;
        }
    }

    public void storeTranspositionTable(@RequestParam String gameId) {
        BoardSquare[][] board = games.get(gameId).gameBoard.getBoard();
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
    public Move IsLastMoveCastlingMove(@RequestParam String gameId, Authentication authentication) {
        // System.out.println("--- CASTLING MOVE CHECK ---");

        Board gameBoard = getGame(gameId, caller(authentication)).gameBoard;
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
    public Move IsLastMovePawnPromotion(@RequestParam String gameId, Authentication authentication){

        // System.out.println("--- PAWN PROMOTION MOVE CHECK ---");

        Board gameBoard = getGame(gameId, caller(authentication)).gameBoard;
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
    public Move IsLastMoveEnPassant(@RequestParam String gameId, Authentication authentication){
        // System.out.println("--- EnPassant MOVE CHECK ---");

        Board gameBoard = getGame(gameId, caller(authentication)).gameBoard;
        if (gameBoard.moveHistory.isEmpty()) {
            return null;
        }

        Move lastMove = gameBoard.moveHistory.getLast();
        if(lastMove instanceof EnPassantMove){
            return (EnPassantMove)lastMove;
        }

        return null;

    }


    // After a pawn reaches the last rank the turn does NOT pass yet (see Move.performNormalMove),
    // so the player who moved it is still "to move" here and passes the turn check.
    @PostMapping("/promote-for-user")
    public void handlePawnPromotion(@RequestParam String gameId, @RequestParam int row, @RequestParam int col, @RequestParam String newPiece,
                                    Authentication authentication) {
        System.out.println("--- PAWN PROMOTED TO " + newPiece + " ---");
        Long me = caller(authentication);
        GameSession game = getGame(gameId, me);
        synchronized (game) {
            requireTurn(game, me);   // checked inside the lock, so two requests can't both pass it
            Board gameBoard = game.gameBoard;
            BoardSquare currentPawnSquare = gameBoard.getSquare(row, col);
            Piece currentPawn = currentPawnSquare.getPiece();
            if (currentPawn == null || !currentPawn.getName().equals("Pawn")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no pawn to promote on that square");
            }
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
            game.version++;
            recordIfFinished(game);
        }
    }



    @PostMapping("/undo")
    public Move handleUndoMove(@RequestParam String gameId, Authentication authentication) {
        // System.out.println("--- UNDO MOVE ---");
        GameSession game = getGame(gameId, caller(authentication));
        requireNotOnline(game);
        synchronized (game) {
            Board gameBoard = game.gameBoard;
            if (gameBoard.moveHistory.isEmpty()) {
                return null;
            }
            Move lastMove = gameBoard.moveHistory.getLast();
            gameBoard.UndoMove();
            game.version++;
            return lastMove;
        }
    }

    @PostMapping("/EngineMove")
    public Move handleEngineMove(@RequestParam String gameId, Authentication authentication) {
        // System.out.println("--- ENGINE MOVE ---");
        GameSession game = getGame(gameId, caller(authentication));
        requireNotOnline(game);
        synchronized (game) {
            // In a game against the bot, the engine may only move on the bot's turn (its seat is empty).
            if (game.mode == GameMode.ENGINE && game.accountToMove() != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It is not the engine's turn");
            }
            Move bestMove = game.engine.getBestMove(4); // You can adjust the depth as needed
            if (bestMove != null) {
                game.gameBoard.movePiece(bestMove);
                game.version++;
                recordIfFinished(game);
                return bestMove;
            }
            return null;
        }
    }

    @PostMapping("/load-fen")
    public boolean loadFenString(@RequestParam String gameId, @RequestParam String fen, Authentication authentication) {
        // System.out.println("--- LOADING NEW FEN ---");
        GameSession game = getGame(gameId, caller(authentication));
        requireNotOnline(game);
        try {
            Board newBoard = new Board();

            FEN fenParser = new FEN();
            fenParser.CreateBoard(newBoard, fen);

            // Replace only the requested session's board and engine.
            synchronized (game) {
                game.selectedSquare = null;
                game.targetSquare = null;
                game.gameBoard = newBoard;
                game.engine = new Engine(newBoard);
                game.recorded = false;
                game.version++;
            }

            return true;
        } catch (Exception e) {
            System.out.println("Failed to load FEN: " + e.getMessage());
            return false;
        }
    }



}
