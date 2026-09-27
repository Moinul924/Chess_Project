# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A chess engine and playable web app built in Java 25 / Spring Boot 4. The Java backend owns all game rules, legal-move generation, and AI search (minimax + alpha-beta pruning with opening book theory); a vanilla JS/HTML/CSS frontend (`src/main/resources/static/`) renders the board and talks to the backend over a REST API. There is no separate frontend build step — static assets are served directly by Spring Boot.

The long-term goal is a real website where people register, play the engine, and play each other 1v1 in real time, with ratings and game history. See **Roadmap** below for what is done, what is half-done, and what still has to be learned and built.

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

The H2 database console is at http://localhost:1000/h2-console (JDBC URL `jdbc:h2:file:./data/chess`, user `sa`, no password). The database file lives in `./data/` and is created on first run.

Note: the test file lives at `src/test/java/com/chess/Chess_Project/ChessProjectApplicationTests.java` under package `com.chess.Chess_Project`, while all main code is under package `com.chess` (no `Chess_Project` subpackage) — this mismatch is pre-existing, not a typo to "fix" incidentally.

## Architecture

### Game engine core (`src/main/java/com/chess/`)

- **`Board`** — the mutable game state: an 8x8 grid of `BoardSquare`, whose turn it is, check/checkmate/stalemate flags, king locations, piece lists per colour, and move history (used for undo). `Board.movePiece()` / `Board.UndoMove()` are the single entry/exit points for applying and reverting a move, and both recompute check/checkmate/stalemate afterward — any new code path that mutates the board should go through these rather than touching squares directly.
- **`BoardSquare`** — one square; holds an optional `Piece` and a pair of Zobrist hash numbers (one per colour/piece-type combination) used by `Board.getZobristHash()` for the transposition table.
- **`chessPiece/`** (`Piece` abstract base + `Pawn`, `Knight`, `Bishop`, `Rook`, `Queen`, `King`, plus the `PieceColour` enum) — each piece implements `getLegalMoves(square, board)`, which is responsible for move generation *and* for filtering out moves that would leave the king in check (pins, checks, etc. are handled inside these implementations, most heavily in `King`). The `getLegalMoves(square, board, generatingMove)` overload exists to avoid infinite recursion when a piece's own move generation needs to ask "is this square attacked?" without re-triggering full pin/check analysis.
- **`chessMove/`** (`Move` base + `CastlingMove`, `EnPassantMove`, `PawnPromotionMove`) — each move type knows how to `execute()` and `undo()` itself against a `Board`, including side effects like updating king location, rook/king "has moved" flags (for castling rights), and whose turn it is. Special moves override `execute`/`undo` to add their extra board effects on top of `Move`'s normal move/undo logic.
- **`Engine`** — all AI logic: minimax search with alpha-beta pruning (`MinMax`), a capture-only quiescence search (`searchAllCaptureMoves`) to avoid the horizon effect, board evaluation via material + piece-square tables (separate middlegame/endgame tables for several pieces), a Zobrist-hash-keyed transposition table, and a dynamic opening book (`findDynamicOpeningMove`) that matches the current game's move history (converted to SAN) as a prefix against lines in `src/main/resources/static/chessOpenings.txt` and plays a random matching continuation. SAN conversion (`convertMoveToSAN`/`convertSANToMove`) is also implemented here and is needed both for the opening book and for storing finished games as `move_list_san`.
- **`FEN`** — parses/serializes the custom-ish FEN-like string format used by this project (board layout + a 2-char metadata suffix: turn indicator `I`/`O` and a perspective/flip flag `0`/`1`), and builds a `Board` from it. Used at startup (`FEN.ChessFenString`, the standard start position), for the "load FEN" UI feature, and as the `final_fen` value when a finished game is recorded.
- **`PairOfData`** — small generic pair helper used by engine/board code.

### Web layer (`src/main/java/com/chess/`)

- **`GameSession`** (package-private) — one live game: its `Board`, its `Engine`, and the currently selected/target squares. Created by `startNewGame()`.
- **`GameController`** (`@RestController`, base path `/api`) — the gameplay HTTP surface. It holds `Map<String, GameSession> games` (a `ConcurrentHashMap`), so **multiple concurrent games are already supported**, keyed by a `gameId` that every endpoint takes as a request parameter. Endpoints: `/start-new-game`, `/close-game`, `/board`, `/current-turn`, `/reset`, `/get-captured-pieces`, `/click` (legal moves for a clicked piece), `/moved` (attempt a move), `/castle`, `/promotion`, `/EnPassant` (so the frontend can pick the right animation/sound), `/promote-for-user`, `/undo`, `/EngineMove` (search at a hardcoded depth of 4 and play the result), and `/load-fen`.
- **`HomeController`** — serves `index.html` at `/`.
- **`config/SecurityConfig`** — the Spring Security filter chain. It currently **permits every request**; it only exempts the H2 console from CSRF and allows same-origin framing so the console works. This is deliberately temporary and is where real authorization rules will go.
- **`config/CsrfController`** — `GET /api/csrf` hands the frontend the CSRF token to send on POSTs.

### Persistence (`src/main/java/com/chess/account/` + `src/main/resources/db/migration/`)

The schema and the JPA entities exist; **nothing reads or writes them yet** — there is no service layer, and no repository is injected into any controller.

- Entities: `Account` (username, email, BCrypt `password_hash`, enabled, createdAt), `PlayerProfile` (display name, country, rating and peak rating defaulting to 1200, W/D/L counters; its primary key is the account id), `UserSettings` (board theme, show-legal-moves, engine depth 1–6), `GameRecord` (both account ids, opponent type, result, termination, `move_list_san`, `final_fen`, ratings before/after, timestamp), and `RatingHistory` (one row per rating change, for a rating-over-time graph).
- Enums: `GameResult`, `OpponentType` (`HUMAN`/`ENGINE`), `GameTermination` (`CHECKMATE`/`STALEMATE`/`RESIGNATION`).
- Repositories: Spring Data JPA interfaces, including `findByUsername`, paged game history (`findByWhiteAccountIdOrBlackAccountIdOrderByPlayedAtDesc`), and rating history by account.
- `V1__create_tables.sql` is the Flyway migration that creates all five tables with foreign keys, CHECK constraints, and indexes. `spring.jpa.hibernate.ddl-auto=validate`, so **Hibernate never creates or alters tables** — every schema change must be a new `V2__....sql`, `V3__....sql` migration file, and entity fields must match the migration exactly or the app refuses to start.

### Frontend (`src/main/resources/static/`)

- `index.html` (landing page) and `play.html` (the board page).
- CSS is split into `variables.css`, `layout.css`, `board.css`, `controls.css`, `overlay.css`.
- JS is split by concern: `api.js` (the fetch wrapper; it **generates a random `gameId` with `crypto.randomUUID()`, keeps it in `sessionStorage`**, and appends it to every request), `boardRender.js`, `drag.js`, `gameController.js` (the bulk of the UI flow), `main.js`, `sound.js`.
- There is no client-side game logic beyond rendering and input handling; legality and state are always resolved server-side.

### Data files

- `chessOpenings.txt` — newline-separated opening lines in SAN, read by `Engine.findDynamicOpeningMove()`.
- `transpositionTableRecords.txt` — appended to by `GameController.storeTranspositionTable()`; not read back in, so treat it as a log rather than a live cache.

## Roadmap: from here to a real multiplayer chess site

Each phase lists what to learn and what to build. The phases are ordered so that each one is useful on its own; don't start a later phase before the one before it works end to end.

### Already done

- Full legal move generation including castling, en passant, promotion, and check/checkmate/stalemate detection.
- Minimax + alpha-beta engine with quiescence search, piece-square tables, a transposition table, and a dynamic opening book.
- FEN load/save, undo, captured-piece list, board flipping, sounds, drag-and-drop UI.
- Multiple simultaneous games in one server process, keyed by `gameId`.
- Database schema designed as a Flyway migration, JPA entities and repositories written, H2 file database configured, and Spring Security plus CSRF plumbed in (currently wide open).

### Phase 1 — Accounts and login

**Learn:** Spring Security's authentication model (`UserDetailsService`, `UserDetails`, `PasswordEncoder`/BCrypt, `AuthenticationManager`); session cookies (`JSESSIONID`) and how they differ from JWTs — for an app shaped like this one, **sessions are the right choice, not JWTs**; CSRF and why `/api/csrf` exists; Bean Validation (`@Valid`, `@NotBlank`, `@Email` — the validation starter is already a dependency); the service-layer pattern (`@Service`, `@Transactional`); and DTOs.

**Build:** an `AccountService` whose register method validates input, checks username/email uniqueness, BCrypt-hashes the password, and creates the `Account`, `PlayerProfile`, and `UserSettings` rows in one transaction; an `AuthController` with `/api/register`, login, logout, and `/api/me`; register and login pages; a `BCryptPasswordEncoder` bean; and real rules replacing the `permitAll()` in `SecurityConfig`. Use `@AuthenticationPrincipal` to know who is calling.

**Gotcha:** today `getGame(gameId)` creates a game for any string anyone sends, and nothing checks **who** is allowed to move in it. Ownership — "this account is White in this game" — is the security foundation that everything in Phase 3 depends on.

### Phase 2 — Saving games, history, and ratings

**Learn:** Spring Data JPA queries and `Pageable`/`Page` (`app.page-size=10` is already in `application.properties` for this); transaction boundaries; writing a follow-up Flyway migration; and the Elo rating formula (expected score, K-factor).

**Build:** a `GameService` that, when a game ends, converts the move history to SAN via `Engine`, builds a `GameRecord`, computes new Elo ratings, updates the `PlayerProfile` counters, and inserts a `RatingHistory` row — all in one `@Transactional` method. Then a profile page (rating, W/D/L, rating graph) and a paged game-history list. Decide explicitly whether games against the engine are rated.

**Also worth doing here:** persist **in-progress** games (store the FEN plus the move list) so that a server restart or a browser refresh doesn't destroy a game. Today `games` is purely in-memory and everything is lost on restart.

### Phase 3 — Human vs human, in real time

This is the biggest new concept, and the reason polling with `fetch()` stops being good enough.

**Learn:** WebSockets and why they beat polling here (the server can push the opponent's move the instant it happens); Spring's WebSocket and **STOMP** support (`spring-boot-starter-websocket`, `@MessageMapping`, `SimpMessagingTemplate`, clients subscribing to a `/topic/game/{id}` destination); securing WebSocket connections with the authenticated HTTP session; and concurrency — two clients can send a move for the same game at the same moment, so moves must be serialized per game (for example by synchronizing on the `GameSession`).

**Build:**

1. **Lobby and matchmaking** — a "play a human" queue that pairs two waiting players, plus private challenge links (`/play/{gameId}`) so friends can play each other directly.
2. **Seats** — each `GameSession` gains a white account id and a black account id, and `/moved` rejects a move if the caller isn't the account whose turn it is. Never trust the client about whose move it is.
3. **Push updates** — after a validated move, broadcast the new state to both players over the socket instead of having them poll.
4. **Clocks** — time controls such as 5+3. Clocks must be **server-authoritative**: store a deadline timestamp per side, count down on the server, and never trust a clock value sent by a browser. Handle flag-fall as a game end.
5. **Game protocol** — resign, offer and accept draws, threefold repetition and the fifty-move rule (note that `GameTermination` currently only allows `CHECKMATE`, `STALEMATE`, and `RESIGNATION`, so this needs a new migration to widen the CHECK constraint), disconnect and reconnect handling, and an abandonment timeout.

**Note:** undo and load-FEN must be disabled in rated human games — they are single-player conveniences.

### Phase 4 — Making it production-quality

**Learn:** DTOs versus entities in JSON responses (`/board` currently serializes the whole `Board` object, which leaks internals and is fragile); Spring profiles (`application-dev.properties` / `application-prod.properties`); PostgreSQL and Docker/`docker-compose`; asynchronous work in Spring (`@Async`, `ExecutorService`) — the engine currently searches on the HTTP request thread, which ties up a server thread for the whole search; iterative deepening with a time limit instead of the hardcoded depth 4 in `/EngineMove`, which is what makes difficulty levels possible; and SLF4J logging instead of the `System.out.println` calls scattered through the controller.

**Build:** swap H2 for PostgreSQL in production while keeping H2 for local development, externalize secrets as environment variables, add rate limiting on the login and move endpoints, put the app behind HTTPS, containerize it, and deploy it (Fly.io, Railway, Render, or a plain VPS all work for a Spring Boot fat jar). Add CI with GitHub Actions running `mvnw test`.

### Phase 5 — Testing (start earlier than feels necessary)

**Learn:** JUnit 5 and AssertJ; `@WebMvcTest` with `MockMvc` for controllers; `@DataJpaTest` for repositories; Testcontainers for testing against a real PostgreSQL; and **perft**, the standard chess technique of counting leaf nodes at a given depth from a known position and comparing the totals against published numbers.

**Build:** perft tests for the move generator first — they find rule bugs that nothing else does — then tests for the `GameService` rating maths and for the move-authorization rules from Phase 3.

### Optional or later

PGN import and export; spectators watching a live game; a move-list panel with clickable navigation; puzzles from a tactics database; adjustable engine difficulty exposed through `UserSettings.engineDepth`; optionally shelling out to Stockfish over UCI as a stronger opponent alongside the homemade engine; and a responsive/mobile layout with keyboard accessibility.

## Conventions and constraints to respect

- All rule logic lives in the backend. The frontend never decides legality.
- Board mutation goes through `Board.movePiece()` / `Board.UndoMove()`, never direct square edits.
- Schema changes are new Flyway migration files. `ddl-auto=validate` means Hibernate refuses to start if the entities and the migrations disagree — that is intentional, so don't "fix" it by switching to `update`.
- `GameController` state is in-memory only; anything that must survive a restart belongs in the database.
- Passwords are only ever stored as BCrypt hashes.
