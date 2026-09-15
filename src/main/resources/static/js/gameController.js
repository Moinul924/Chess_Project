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

        render.showPromotionMenu(pieceName, targetSquareElement, row, async (option) => {
            this.isPromoting = false;
            this.playSound('promotion');

            await api.promotePawn(row, col, option);
            await this.updateGameOverState();
            if (this.playEngine && !this.isGameOver) {
                await this.handleEngineMove();
            }
        });
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
        // 1. Prevent undo if a piece is actively being dragged or promoted
        if (this.activePiece || this.floatingPiece || this.isPromoting) return;

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


}
