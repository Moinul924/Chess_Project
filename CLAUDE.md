# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A chess engine and playable web app built in Java 25 / Spring Boot. The Java backend owns all game rules, legal-move generation, and AI search (minimax + alpha-beta pruning with opening book theory); a vanilla JS/HTML/CSS frontend (`src/main/resources/static/`) renders the board and talks to the backend over a REST API. There is no separate frontend build step — static assets are served directly by Spring Boot.

## Common commands

Run from the repo root (Windows: use `mvnw.cmd`, not the empty `mvnw` file).

```powershell
# Run the app (serves at http://localhost:1000/)
./mvnw.cmd spring-boot:run

# Compile
./mvnw.cmd compile

# Run all tests
./mvnw.cmd test

# Run a single test class
./mvnw.cmd test -Dtest=ChessProjectApplicationTests

# Build the jar
./mvnw.cmd package
```

Note: the test file lives at `src/test/java/com/chess/Chess_Project/ChessProjectApplicationTests.java` under package `com.chess.Chess_Project`, while all main code is under package `com.chess` (no `Chess_Project` subpackage) — this mismatch is pre-existing, not a typo to "fix" incidentally.

## Architecture

### Backend (`src/main/java/com/chess/`)

- **`Board`** — the mutable game state: an 8x8 grid of `BoardSquare`, whose turn it is, check/checkmate/stalemate flags, king locations, piece lists per colour, and move history (used for undo). `Board.movePiece()` / `Board.UndoMove()` are the single entry/exit points for applying and reverting a move, and both recompute check/checkmate/stalemate afterward — any new code path that mutates the board should go through these rather than touching squares directly.
- **`BoardSquare`** — one square; holds an optional `Piece` and a pair of Zobrist hash numbers (one per colour/piece-type combination) used by `Board.getZobristHash()` for the transposition table.
- **`chessPiece/`** (`Piece` abstract base + `Pawn`, `Knight`, `Bishop`, `Rook`, `Queen`, `King`) — each piece implements `getLegalMoves(square, board)`, which is responsible for move generation *and* for filtering out moves that would leave the king in check (pins, checks, etc. are handled inside these implementations, most heavily in `King`). The `getLegalMoves(square, board, generatingMove)` overload exists to avoid infinite recursion when a piece's own move generation needs to ask "is this square attacked?" without re-triggering full pin/check analysis.
- **`chessMove/`** (`Move` base + `CastlingMove`, `EnPassantMove`, `PawnPromotionMove`) — each move type knows how to `execute()` and `undo()` itself against a `Board`, including side effects like updating king location, rook/king "has moved" flags (for castling rights), and whose turn it is. Special moves override `execute`/`undo` to add their extra board effects on top of `Move`'s normal move/undo logic.
- **`Engine`** — all AI logic: minimax search with alpha-beta pruning (`MinMax`), a capture-only quiescence search (`searchAllCaptureMoves`) to avoid the horizon effect, board evaluation via material + piece-square tables (separate middlegame/endgame tables for several pieces), a Zobrist-hash-keyed transposition table, and a dynamic opening book (`findDynamicOpeningMove`) that matches the current game's move history (converted to SAN) as a prefix against lines in `src/main/resources/static/chessOpenings.txt` and plays a random matching continuation. SAN conversion (`convertMoveToSAN`/`convertSANToMove`) is also implemented here and is needed both for the opening book and could be reused for move display/logging.
- **`FEN`** — parses/serializes the custom-ish FEN-like string format used by this project (board layout + a 2-char metadata suffix: turn indicator `I`/`O` and a perspective/flip flag `0`/`1`), and builds a `Board` from it. Used both at startup (`FEN.ChessFenString`, the standard start position) and for the "load FEN" UI feature.
- **`GameController`** (`@RestController`, base path `/api`) — the only HTTP surface for gameplay: `/board` (state), `/click` (get legal moves for a clicked piece), `/moved` (attempt to execute a move), `/undo`, `/EngineMove` (ask the `Engine` for a move at a fixed search depth and play it), `/load-fen`, plus endpoints to check whether the last move was a castle/en passant/promotion so the frontend can play the right animation/sound. `GameController` holds the live `Board`/`Engine` as instance state (not session-scoped) — there is effectively one shared game per running server instance.
- **`HomeController`** — serves `index.html` at `/`.

### Frontend (`src/main/resources/static/`)

- `index.html` + `css/board.css` + `js/board.js` implement a single-page board UI: drag-and-drop piece movement, move highlighting, sound effects, pawn promotion picker, undo, board flipping, and FEN loading — all driven by `fetch()` calls against the `/api/*` endpoints above. There's no client-side game logic beyond rendering and input handling; legality and state are always resolved server-side.

### Data files

- `chessOpenings.txt` — newline-separated opening lines in SAN, read by `Engine.findDynamicOpeningMove()`.
- `transpositionTableRecords.txt` — appended to by `GameController.storeTranspositionTable()` when a game ends (checkmate/stalemate); not currently read back in, so treat it as a log rather than a live cache.
