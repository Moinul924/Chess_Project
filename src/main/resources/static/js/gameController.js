import * as api from './api.js';
import * as render from './boardRender.js';
import * as drag from './drag.js';
import { playSound as playAudio } from './sound.js'

export class GameController {
    constructor() {
        // --- DOM Elements ---
        this.boardElement = document.querySelector('.grid-board');
        this.gameOverOverlay = document.getElementById('game-over-overlay');
        this.gameOverTitle = document.getElementById('game-over-title');
        this.gameOverDetail = document.getElementById('game-over-detail');
        this.gameModeLabel = document.getElementById('game-mode-label');
        this.undoButton = document.getElementById('undo-button');
        this.engineButton = document.getElementById('play-engine-button');
        this.fenInput = document.getElementById('fen-input');
        this.loadFenButton = document.getElementById('load-fen-button');
        this.loadFenButton?.addEventListener('click', () => this.handleLoadFen());
        this.playAginButton = document.getElementById('play-again-button');
        this.menuButton = document.getElementById('navbar-menu-button');
        this.navigationMenu = document.getElementById('navbar-links');
        this.clockPlayer1 = document.getElementById('clock1');
        this.clockPlayer2 = document.getElementById('clock2');
        this.blackMaterial = document.getElementById('black-material');
        this.whiteMaterial = document.getElementById('white-material');
        this.blackMaterialScore = document.getElementById('black-material-score');
        this.whiteMaterialScore = document.getElementById('white-material-score');
        // --- Game State ---
        this.activePiece = null;
        this.floatingPiece = null;
        this.isGameOver = false;
        this.isPromoting = false;
        this.soundMade = false;
        this.playEngine = false;
        this.player1time = 20;
        this.player2time = 20;
        this.player2White = Math.random() < 0.5;
        this.currentWhiteTurn = true
        this.clockInterval = null;
        // --- Online play ---
        this.onlineColour = null;      // 'WHITE' or 'BLACK' in an online game, null otherwise
        this.lastSeenVersion = null;   // the server's game version this page has drawn
        this.pollTimer = null;
        this.refreshing = false;
        this.onOnlineState = null;     // called with each new state, to update names and the status line
        this.renderClocks();
        this.startClock();

        // --- Bind Event Contexts (delegated to drag.js, which reads/writes this instance) ---
        this.mouseDownHandler = (e) => drag.mouseDownHandler(this, e);
        this.mouseMoveHandler = (e) => drag.mouseMoveHandler(this, e);
        this.mouseUpHandler = (e) => drag.mouseUpHandler(this, e);
        this.undoButton?.addEventListener('click', () => this.handleUndo());
        this.engineButton?.addEventListener('click', () => this.handlePlayEngine());
        this.playAginButton?.addEventListener('click', () => this.resetGame());
        this.setGameModeLabel('1 v 1');
    }

    // ==========================================
    // INITIALIZATION & BOARD SETUP
    // ==========================================

    createGrid() {
        render.createGrid(this.boardElement);
    }

    async fetchBoard() {
        const boardData = await api.getBoard();
        this.currentWhiteTurn = boardData.currentWhiteTurn;
        for (let row = 0; row < 8; row++) {
            for (let col = 0; col < 8; col++) {
                let currentSquare = boardData.board[row][col];
                if (currentSquare.piece !== null) {
                    const pieceName = currentSquare.piece.name;
                    const pieceColor = currentSquare.piece.colour[0];
                    this.addPiece(pieceColor + pieceName, String.fromCharCode(97 + row) + col);
                }
            }
        }
        console.log("Player 2 is white:", this.player2White);
        if (!this.player2White) { this.flipBoard(); }
        return boardData;
    }

    async updateCapturedPieces() {
        const capturedMoves = await api.getCapturedPieces();
        this.blackMaterial.innerHTML = '';
        this.whiteMaterial.innerHTML = '';
        this.blackMaterialScore.textContent = '';
        this.whiteMaterialScore.textContent = '';

        const player1Colour = this.player2White ? 'BLACK' : 'WHITE';
        let player1CapturedValue = 0;
        let player2CapturedValue = 0;

        capturedMoves.forEach(move => {
            const capturedPiece = move.pieceCaptured;
            if (!capturedPiece) return;

            if (move.piece.colour === player1Colour) {
                player1CapturedValue += capturedPiece.pieceValue;
            } else {
                player2CapturedValue += capturedPiece.pieceValue;
            }

            const image = document.createElement('img');
            const colourName = capturedPiece.colour === 'BLACK' ? 'Black' : 'White';
            const pieceName = capturedPiece.name;
            const pieceId = capturedPiece.colour[0] + pieceName;
            image.src = `./piece_images/Images-80px/${colourName}/${pieceId}-80px.png`;
            image.className = 'captured-piece';
            image.alt = `Captured ${colourName.toLowerCase()} ${pieceName}`;

            const container = move.piece.colour === player1Colour
                ? this.blackMaterial
                : this.whiteMaterial;
            container.appendChild(image);
        });

        const materialDifference = Math.round(
            Math.abs(player1CapturedValue - player2CapturedValue) / 100
        );

        if (materialDifference > 0) {
            const leadingScore = player1CapturedValue > player2CapturedValue
                ? this.blackMaterialScore
                : this.whiteMaterialScore;
            leadingScore.textContent = `+${materialDifference}`;
        }
    }

    addPiece(piece, position) {
        render.addPiece(piece, position, this.mouseDownHandler);
    }

    flipBoard() {
        this.boardElement.classList.toggle('flipped', !this.player2White);
    }

    // ==========================================
    // ENGINE MOVES
    // ==========================================

    async handleEngineMove() {
        const engineMoveData = await api.getEngineMove();
        if (!engineMoveData) return;
        const startRow = engineMoveData.startSquare.row;
        const startCol = engineMoveData.startSquare.col;
        const endRow = engineMoveData.endSquare.row;
        const endCol = engineMoveData.endSquare.col;

        const startId = render.convertIndicesToPosition(startRow, startCol);
        const endId = render.convertIndicesToPosition(endRow, endCol);

        const startSquareElement = document.getElementById(startId);
        const targetSquareElement = document.getElementById(endId);
        const pieceOnTarget = targetSquareElement.querySelector('.piece');
        const targetElement = pieceOnTarget ? pieceOnTarget : targetSquareElement;
        this.activePiece = startSquareElement.querySelector('.piece');
        const pieceName = this.activePiece.id.split('-')[0];
        const position = { row: endRow, col: endCol };
        this.soundMade = false;
        await this.makeMove(targetElement, targetSquareElement, startSquareElement, pieceName, position);
        this.activePiece = null; // Clear the active piece so the human player can pick up pieces again
    }

    // ==========================================
    // MOVE LOGIC & API COMMUNICATION
    // ==========================================

    async processMoveAttempt(targetElement, targetSquareElement, startSquareElement) {
        const pieceName = this.activePiece.id.split('-')[0];
        const position = render.convertPositionToIndices(targetSquareElement.id);

        const isMoveLegal = await api.sendMove(position.row, position.col, pieceName);
        this.moveMade = false;
        if (isMoveLegal) {
            this.moveMade = true;
            this.soundMade = false;
            // Our own move bumped the server's version by one; count it so the next check
            // doesn't mistake it for an opponent move and redraw the board.
            if (this.isOnline()) this.lastSeenVersion++;
            await this.makeMove(targetElement, targetSquareElement, startSquareElement, pieceName, position);
        }
    }

    async makeMove(targetElement, targetSquareElement, startSquareElement, pieceName, position) {
        const isCapture = targetElement.classList.contains('piece');

        // --- Apply Last Move Highlights ---
        render.clearLastMoveHighlights();
        startSquareElement.classList.add('last-move-highlight');
        targetSquareElement.classList.add('last-move-highlight');

        // 1. Update DOM
        if (isCapture) targetSquareElement.removeChild(targetElement);
        targetSquareElement.appendChild(this.activePiece);
        this.activePiece.id = `${pieceName}-${targetSquareElement.id}`;

        // 2. Check Game State (Check, Special Moves, Game Over)
        await this.checkKingInCheck();
        await this.handleSpecialMoves(pieceName, targetSquareElement, position);
        await this.updateCapturedPieces();
        await this.syncClockTurn();

        if (!this.isPromoting) {
            await this.updateGameOverState();
        }

        // 3. Play Default Sounds if special sound wasn't triggered
        if (!this.soundMade) {
            this.playSound(isCapture ? 'capture' : 'move');
        }
    }

    async handleSpecialMoves(pieceName, targetSquareElement, position) {
        // Pawn Promotion made by the engine/opponent (auto-resolved server-side)
        const promotionText = await api.getLastMovePromotion();
        if (promotionText) {
            const data = JSON.parse(promotionText);
            render.executePromotionUI(data.endSquare, data.promotedPiece, this.mouseDownHandler);
            this.playSound('promote');
            return;
        }

        // Castling
        const castleText = await api.getLastMoveCastling();
        if (castleText) {
            const data = JSON.parse(castleText);
            render.executeCastlingUI(data.RookStartSquare, data.RookEndSquare);
            this.playSound('castal');
            return;
        }

        // En Passant
        const enPassantText = await api.getLastMoveEnPassant();
        if (enPassantText) {
            const data = JSON.parse(enPassantText);
            render.executeEnPassantUI(data.capturedPawnSquare);
            this.playSound('capture');
            return;
        }

        // Pawn Promotion chosen by the human player
        if (pieceName.includes('Pawn') && (position.row === 0 || position.row === 7)) {
            this.showPromotionMenu(pieceName, targetSquareElement, position.row, position.col);
            return; // Exit early so game over isn't checked until promotion finishes
        }
    }

    // ==========================================
    // UI UPDATES & SPECIAL MOVES
    // ==========================================

    async showLegalMoves() {
        render.clearLegalMoveIndicators();

        const [pieceName, position] = this.activePiece.id.split('-');
        let indices = render.convertPositionToIndices(position);

        const legalMoves = await api.getLegalMoves(indices.row, indices.col, pieceName);
        render.renderLegalMoveIndicators(legalMoves);
    }

    showPromotionMenu(pieceName, targetSquareElement, row, col) {
        this.isPromoting = true;

        render.showPromotionMenu(
            pieceName,
            targetSquareElement,
            row,
            async (option) => {
            this.isPromoting = false;
            this.playSound('promotion');

            this.refreshing = true;   // don't let an online check redraw the board mid-promotion
            await api.promotePawn(row, col, option);
            if (this.isOnline()) this.lastSeenVersion++;   // the promotion is one more server change
            await this.syncClockTurn();   // the turn only passes once the promotion is done
            this.refreshing = false;
            await this.updateGameOverState();
            if (this.playEngine && !this.isGameOver) {
                await this.handleEngineMove();
            }
            },
            this.boardElement.classList.contains('flipped')
        );
    }

    // ==========================================
    // GAME STATE & UTILITIES
    // ==========================================

    async checkKingInCheck() {
        const boardData = await api.getBoard();
        if (boardData.KingInCheck) {
            this.playSound('check');
        }
    }

    async updateGameOverState() {
        const boardData = await api.getBoard();

        if (boardData.CheckMate || boardData.StaleMate) {
            this.isGameOver = true;
            drag.cancelActiveDrag(this);
            this.stopClock();
            this.playSound('gameover');

            const message = boardData.CheckMate
                ? `${boardData.currentWhiteTurn ? 'Black' : 'White'} wins by checkmate.`
                : 'The game ends in a stalemate. No winner.';

            render.showGameOverMessage(
                { overlay: this.gameOverOverlay, title: this.gameOverTitle, detail: this.gameOverDetail },
                message
            );
        }
    }

    playSound(type) {
        playAudio(type);
        this.soundMade = true;
    }

    async handleUndo() {
        // 1. Prevent undo if a piece is actively being dragged or promoted (and never in online games)
        if (this.activePiece || this.floatingPiece || this.isPromoting || this.isOnline()) return;

        // 2. Call the backend API
        const responseText = await api.undoMove();

        // 3. If a move was successfully undone (backend didn't return null)
        if (responseText) {
            // A. Remove all current pieces from the board
            document.querySelectorAll('.piece').forEach(piece => piece.remove());

            // B. Clear any lingering visual highlights
            render.clearLastMoveHighlights();
            render.clearSelectedHighlight();
            render.clearLegalMoveIndicators();

            // C. Re-fetch the board state directly from the backend
            await this.fetchBoard();
            await this.updateCapturedPieces();
            this.renderClocks();

            // D. Reset the Game Over state if the game was finished
            this.isGameOver = false;
            if (this.gameOverOverlay) {
                this.gameOverOverlay.classList.remove('visible');
            }

            // E. Play a sound to confirm the action
            this.playSound('move');
        }
    }

    async handlePlayEngine() {
        this.playEngine = true;
        this.setGameModeLabel('playing agent bot');
    }

    setGameModeLabel(text) {
        if (this.gameModeLabel) {
            this.gameModeLabel.textContent = text;
        }
    }

    async handleLoadFen() {
        const fenString = this.fenInput.value.trim();
        if (!fenString) return;

        try {
            const success = await api.loadFen(fenString);

            if (success) {
                // 1. Clear the physical board DOM
                document.querySelectorAll('.piece').forEach(piece => piece.remove());
                render.clearLastMoveHighlights();
                render.clearSelectedHighlight();
                render.clearLegalMoveIndicators();

                // 2. Reset frontend state
                this.activePiece = null;
                if (this.floatingPiece) {
                    this.floatingPiece.remove();
                    this.floatingPiece = null;
                }
                this.isGameOver = false;
                this.resetClocks();
                if (this.gameOverOverlay) {
                    this.gameOverOverlay.classList.remove('visible');
                }

                // 3. Fetch the newly generated board from the backend
                await this.fetchBoard();
                await this.updateCapturedPieces();
                await this.updateGameOverState();
                this.playSound('move');
            } else {
                alert("Invalid FEN string format! Check the backend console for the exact error.");
            }
        } catch (error) {
            console.error("Error loading FEN:", error);
        }
    }

    async resetGame(){
        // An online game can't be restarted from one side, so "Play Again" goes back to the game menu.
        if (this.isOnline()) {
            window.location.href = '/play.html';
            return;
        }
        // 1. Call the backend API to reset the game
        const reset = await api.resetGame();
        if (reset) {
            // Clear the physical board DOM
            document.querySelectorAll('.piece').forEach(piece => piece.remove());
            render.clearLastMoveHighlights();
            render.clearSelectedHighlight();
            this.isGameOver = false;
            this.resetClocks();
            if (this.gameOverOverlay) {
                this.gameOverOverlay.classList.remove('visible');
            }

            await this.fetchBoard();
            await this.updateCapturedPieces();
            await this.updateGameOverState();
            this.startClock();
        }
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    startClock() {
        if (!this.clockPlayer1 && !this.clockPlayer2) return;
        this.stopClock();
        this.renderClocks();
        this.clockInterval = setInterval(() => this.updateClock(), 1000);
    }

    stopClock() {
        if (this.clockInterval) {
            clearInterval(this.clockInterval);
            this.clockInterval = null;
        }
    }

    updateClock() {
        if (this.isGameOver || this.isPromoting) return;

        if (this.currentWhiteTurn === this.player2White) {
            this.player2time = Math.max(0, this.player2time - 1);
        } else {
            this.player1time = Math.max(0, this.player1time - 1);
        }

        this.renderClocks();

        if (this.player1time === 0 || this.player2time === 0) {
            this.isGameOver = true;
            drag.cancelActiveDrag(this);
            this.stopClock();
            const winner = this.player1time === 0 ? 'Player2' :  this.playEngine === true ? 'Bot' : 'Player1';
            render.showGameOverMessage(
                { overlay: this.gameOverOverlay, title: this.gameOverTitle, detail: this.gameOverDetail },
                `${winner} wins on time.`
            );
        }
    }

    async syncClockTurn() {
        this.currentWhiteTurn = await api.getCurrentTurn();
    }

    resetClocks() {
        this.player1time = 600;
        this.player2time = 600;
        this.currentWhiteTurn = true;
        this.renderClocks();
    }

    renderClocks() {
        if (this.clockPlayer1) this.clockPlayer1.textContent = this.formatTime(this.player1time);
        if (this.clockPlayer2) this.clockPlayer2.textContent = this.formatTime(this.player2time);
    }

    formatTime(totalSeconds) {
        const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
        const seconds = (totalSeconds % 60).toString().padStart(2, '0');
        return `${minutes}:${seconds}`;
    }

    // ==========================================
    // ONLINE PLAY (two accounts)
    // ==========================================

    isOnline() {
        return this.onlineColour !== null;
    }

    isMyTurn() {
        return (this.currentWhiteTurn ? 'WHITE' : 'BLACK') === this.onlineColour;
    }

    // May this piece be picked up right now? Online, only your own pieces on your turn.
    // This just stops pointless drags; the server checks it again and has the final say.
    canPickUp(pieceElement) {
        if (!this.isOnline()) return true;
        const pieceColour = pieceElement.id[0] === 'W' ? 'WHITE' : 'BLACK';
        return this.opponentJoined && this.isMyTurn() && pieceColour === this.onlineColour;
    }

    // Call before createGrid()/fetchBoard() so the board is drawn from this player's side.
    // onLostGame is called if the server stops recognising this page as a player in the game.
    enableOnline(state, onOnlineState, onLostGame) {
        this.onLostGame = onLostGame;
        this.onlineColour = state.myColour;
        this.player2White = state.myColour === 'WHITE';   // "player 2" is the bottom of the screen: you
        this.lastSeenVersion = state.version;
        this.opponentJoined = state.opponentJoined;
        this.onOnlineState = onOnlineState;
        // The clocks only run in this browser, so the two players' clocks would disagree.
        // They stay off until clocks are kept on the server.
        this.stopClock();
    }

    // Asks the server every 1.5 seconds whether anything changed. This stands in for WebSockets,
    // which will push the opponent's move instantly instead of the page having to ask.
    startPolling() {
        this.pollTimer = setInterval(() => this.pollOnlineState(), 1500);
    }

    stopPolling() {
        clearInterval(this.pollTimer);
        this.pollTimer = null;
    }

    async pollOnlineState() {
        // Don't redraw the board underneath a player who is dragging a piece or picking a promotion.
        if (this.refreshing || this.activePiece || this.floatingPiece || this.isPromoting) return;

        const state = await api.getOnlineState();
        if (!state) {
            // Refused: this browser is now signed in as someone who isn't in this game
            // (another tab logged in as a different account), or the game is gone (server restart).
            this.stopPolling();
            this.isGameOver = true;   // stops pieces being picked up
            this.onLostGame?.();
            return;
        }
        this.opponentJoined = state.opponentJoined;

        if (state.version !== this.lastSeenVersion) {
            const turnChanged = state.whiteToMove !== this.currentWhiteTurn;
            this.lastSeenVersion = state.version;
            await this.redrawFromServer(turnChanged);
        }
        this.onOnlineState?.(state, this);
        if (state.gameOver) this.stopPolling();
    }

    // Redraws the whole board from the server's copy, e.g. after the opponent moved.
    async redrawFromServer(playMoveSound) {
        this.refreshing = true;
        try {
            document.querySelectorAll('.piece').forEach(piece => piece.remove());
            render.clearLegalMoveIndicators();
            render.clearSelectedHighlight();

            const boardData = await this.fetchBoard();
            this.highlightLastMove(boardData);
            await this.updateCapturedPieces();

            this.soundMade = false;
            await this.checkKingInCheck();
            await this.updateGameOverState();
            if (playMoveSound && !this.soundMade) this.playSound('move');
        } finally {
            this.refreshing = false;
        }
    }

    highlightLastMove(boardData) {
        render.clearLastMoveHighlights();
        const lastMove = boardData.moveHistory?.at(-1);
        if (!lastMove?.startSquare || !lastMove?.endSquare) return;
        const start = document.getElementById(render.convertIndicesToPosition(lastMove.startSquare.row, lastMove.startSquare.col));
        const end = document.getElementById(render.convertIndicesToPosition(lastMove.endSquare.row, lastMove.endSquare.col));
        start?.classList.add('last-move-highlight');
        end?.classList.add('last-move-highlight');
    }


}
