class ChessUI {
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
        this.loadFenButton.addEventListener('click', () => this.handleLoadFen());
        this.flipButton = document.getElementById('flip-board-button');
        
        // --- Game State ---
        this.activePiece = null;
        this.floatingPiece = null;
        this.isGameOver = false;
        this.isPromoting = false;
        this.soundMade = false;
        this.playEngine = false;
        this.showMade = false;
        this.flipBoard = false;

        // --- Bind Event Contexts ---
        this.mouseDownHandler = this.mouseDownHandler.bind(this);
        this.mouseMoveHandler = this.mouseMoveHandler.bind(this);
        this.mouseUpHandler = this.mouseUpHandler.bind(this);
        this.undoButton.addEventListener('click', () => this.handleUndo());
        this.engineButton.addEventListener('click', () => this.handlePlayEngine());
        this.flipButton.addEventListener('click', () => this.toggleFlip());
        this.setGameModeLabel('1 v 1');
        
    }

    // ==========================================
    // INITIALIZATION & BOARD SETUP
    // ==========================================

    createGrid() {
        for (let i = 0; i < 64; i++) {
            const square = document.createElement('div');
            square.className = 'square';
            
            let row = String.fromCharCode(97 + Math.floor(i / 8)); 
            let col = i % 8; 
            
            square.classList.add((Math.floor(i / 8) + col) % 2 === 0 ? 'light-square' : 'dark-square');
            square.id = row + col;
           
            this.boardElement.appendChild(square);
        }
    }

    async fetchBoard() {
        const response = await fetch('/api/board');
        const boardData = await response.json();
        
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
    }

    addPiece(piece, position) {
        const square = document.getElementById(position);
        const img = document.createElement('img');
        const colorFolder = piece[0] === 'B' ? 'Black' : 'White';
        
        img.src = `./piece_images/Images-80px/${colorFolder}/${piece}-80px.png`;
        img.className = 'piece';
        img.id = `${piece}-${position}`;
        img.draggable = false; 
        
        img.addEventListener('mousedown', this.mouseDownHandler);    
        square.appendChild(img);
    }

    // ==========================================
    // DRAG AND DROP EVENT HANDLERS
    // ==========================================

    async mouseDownHandler(e) {
        if (this.isGameOver || this.isPromoting || this.activePiece || this.floatingPiece) return;
        e.preventDefault();

        this.activePiece = e.target;
        this.activePiece.style.opacity = '0'; 

        // Highlight the square we just picked the piece up from
        this.clearSelectedHighlight();
        this.activePiece.parentElement.classList.add('selected-square');

        // Create the enlarged floating piece
        this.floatingPiece = document.createElement('img');
        this.floatingPiece.src = this.activePiece.src.replaceAll('80px', '128px');    
        this.floatingPiece.className = 'dragging-piece';
        this.moveAt(e.pageX, e.pageY);
        document.body.appendChild(this.floatingPiece);

        document.addEventListener('mousemove', this.mouseMoveHandler);
        document.addEventListener('mouseup', this.mouseUpHandler);

        await this.showLegalMoves();
    }

    moveAt(pageX, pageY) {
        if (this.floatingPiece) {
            this.floatingPiece.style.left = `${pageX}px`;
            this.floatingPiece.style.top = `${pageY}px`;
        }
    }

    mouseMoveHandler(e) {
        this.moveAt(e.pageX, e.pageY);
    }

    async mouseUpHandler(e) {
        if (this.isGameOver) return;

        document.removeEventListener('mousemove', this.mouseMoveHandler);
        document.removeEventListener('mouseup', this.mouseUpHandler);

        if (!this.floatingPiece || !this.activePiece) return;

        // Save the start square before we change the DOM
        const startSquareElement = this.activePiece.parentElement;

        // Clean up UI
        this.floatingPiece.remove();
        this.floatingPiece = null;
        this.activePiece.style.opacity = '1';
        this.clearLegalMoveIndicators();
        this.clearSelectedHighlight(); // Remove the "picked up" highlight

        // Determine target square
        let targetElement = document.elementFromPoint(e.clientX, e.clientY);
        let targetSquareElement = targetElement?.classList.contains('piece') ? targetElement.parentElement : targetElement;

        if (targetSquareElement?.classList.contains('square')) {
            await this.processMoveAttempt(targetElement, targetSquareElement, startSquareElement);
            if (this.playEngine && !this.isGameOver && !this.isPromoting && this.moveMade) {
                await this.sleep(500);
                await this.handleEngineMove();
                this.moveMade = false; // Reset after engine move
            }
        }
        
        this.activePiece = null;
    }


    async handleEngineMove() 
    {
        const response = await fetch('/api/EngineMove', { method: 'POST' });
        const engineMoveData = await response.json();
        if (!engineMoveData) return;
        const startRow = engineMoveData.startSquare.row;
        const startCol = engineMoveData.startSquare.col;
        const endRow = engineMoveData.endSquare.row;
        const endCol = engineMoveData.endSquare.col;

        // 2. Convert to your HTML IDs (e.g., "e2" and "e4")
        const startId = this.convertIndicesToPosition(startRow, startCol);
        const endId = this.convertIndicesToPosition(endRow, endCol);

        // 3. Grab the Square Elements from the DOM
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
        const position = this.convertPositionToIndices(targetSquareElement.id);
        
        const response = await fetch(`/api/moved?row=${position.row}&col=${position.col}&name=${pieceName}`, { method: 'POST' });
        const isMoveLegal = await response.json(); 
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
            this.clearLastMoveHighlights();
            startSquareElement.classList.add('last-move-highlight');
            targetSquareElement.classList.add('last-move-highlight');

            // 1. Update DOM
            if (isCapture) targetSquareElement.removeChild(targetElement);
            targetSquareElement.appendChild(this.activePiece);
            this.activePiece.id = `${pieceName}-${targetSquareElement.id}`;

            // 2. Check Game State (Check, Special Moves, Game Over)
            await this.checkKingInCheck();
            await this.handleSpecialMoves(pieceName, targetSquareElement, position);
            
            if (!this.isPromoting) {
                await this.updateGameOverState();
            }

            // 3. Play Default Sounds if special sound wasn't triggered
            if (!this.soundMade) {
                this.playSound(isCapture ? 'capture' : 'move');
            }
    }

    async handleSpecialMoves(pieceName, targetSquareElement, position) {

        const promotionRes = await fetch('/api/promotion');
        const promotionText = await promotionRes.text();
        if (promotionText) {
            const data = JSON.parse(promotionText);
            console.log("Promotion Data:", data);
            this.executePromotionUI(data.endSquare, data.promotedPiece);
            this.playSound('promote');
            return;
        }
        // Pawn Promotion
        

        // Castling
        const castleRes = await fetch('/api/castle');
        const castleText = await castleRes.text(); 
        if (castleText) {
            const data = JSON.parse(castleText); 
            this.executeCastlingUI(data.RookStartSquare, data.RookEndSquare);
            this.playSound('castal');
            return; 
        }

        // En Passant
        const enPassantRes = await fetch('/api/EnPassant');
        const enPassantText = await enPassantRes.text();
        if (enPassantText) {
            const data = JSON.parse(enPassantText);
            this.executeEnPassantUI(data.capturedPawnSquare);
            this.playSound('capture');
            return;
        }

        // Pawn Promotion
        if (pieceName.includes('Pawn') && (position.row === 0 || position.row === 7)) {
            this.showPromotionMenu(pieceName, targetSquareElement, position.row, position.col);
            return; // Exit early so game over isn't checked until promotion finishes
        }


    }

    // ==========================================
    // UI UPDATES & SPECIAL MOVES
    // ==========================================

    async showLegalMoves() {
        this.clearLegalMoveIndicators();
        
        const [pieceName, position] = this.activePiece.id.split('-'); 
        let indices = this.convertPositionToIndices(position);
        
        const response = await fetch(`/api/click?row=${indices.row}&col=${indices.col}&name=${pieceName}`, { method: 'POST' });
        const legalMoves = await response.json(); 
    
        legalMoves.forEach(move => {
            const squareId = String.fromCharCode(97 + move.endSquare.row) + move.endSquare.col;
            const squareElement = document.getElementById(squareId);
            
            if (squareElement) {
                const indicator = document.createElement('div'); 
                indicator.className = squareElement.querySelector('.piece') ? 'legal-capture-indicator' : 'legal-move-indicator';
                squareElement.appendChild(indicator);
            }
        });
    }

    // --- NEW: Highlight Clearing Helpers ---
    clearSelectedHighlight() {
        document.querySelectorAll('.selected-square').forEach(el => el.classList.remove('selected-square'));
    }

    clearLastMoveHighlights() {
        document.querySelectorAll('.last-move-highlight').forEach(el => el.classList.remove('last-move-highlight'));
    }

    clearLegalMoveIndicators() {
        document.querySelectorAll('.legal-move-indicator, .legal-capture-indicator').forEach(el => el.remove());
    }

    showPromotionMenu(pieceName, targetSquareElement, row, col) {
        this.isPromoting = true;
        const colorLetter = pieceName[0]; 
        const folderColor = colorLetter === 'W' ? 'White' : 'Black';

        const menu = document.createElement('div');
        menu.className = 'promotion-menu';
        menu.style[row === 0 ? 'bottom' : 'top'] = '100%';

        ['Queen', 'Knight', 'Rook', 'Bishop'].forEach(option => {
            const img = document.createElement('img');
            const newPieceName = colorLetter + option; 
            img.src = `./piece_images/Images-80px/${folderColor}/${newPieceName}-80px.png`;
            img.className = 'promotion-option';
            
            img.addEventListener('click', async () => {
                this.isPromoting = false;
                this.playSound('promotion');
                menu.remove(); 

                const pawnOnBoard = targetSquareElement.querySelector('.piece');
                if (pawnOnBoard) {
                    pawnOnBoard.src = img.src;
                    pawnOnBoard.id = `${newPieceName}-${targetSquareElement.id}`;
                }
                
                await fetch(`/api/promote_for_user?row=${row}&col=${col}&newPiece=${option}`, { method: 'POST' });
                await this.updateGameOverState();
                if (this.playEngine && !this.isGameOver) {
                    await this.handleEngineMove();
                }
                
            });

            menu.appendChild(img);
        });

        targetSquareElement.appendChild(menu);
    }

    executeCastlingUI(startSquare, endSquare) {
        const startId = this.convertIndicesToPosition(startSquare.row, startSquare.col);
        const endId = this.convertIndicesToPosition(endSquare.row, endSquare.col);

        const startDOM = document.getElementById(startId);
        const endDOM = document.getElementById(endId);

        if (startDOM && endDOM) {
            const rookPiece = startDOM.querySelector('.piece');
            if (rookPiece) {
                endDOM.appendChild(rookPiece);
                rookPiece.id = `${rookPiece.id.split('-')[0]}-${endId}`;
            }
        }
    }

    executeEnPassantUI(capturedSquare) {
        const captureId = this.convertIndicesToPosition(capturedSquare.row, capturedSquare.col);
        document.getElementById(captureId)?.querySelector('.piece')?.remove();
    }

    executePromotionUI(promotionSquare, newPiece) {
        const pieceName = newPiece.name;
        const pieceColor = newPiece.colour[0]; 
        const PawnID = this.convertIndicesToPosition(promotionSquare.row, promotionSquare.col);
        document.getElementById(PawnID)?.querySelector('.piece')?.remove();
        this.addPiece(pieceColor + pieceName, String.fromCharCode(97 + promotionSquare.row) + promotionSquare.col);
    }

    // ==========================================
    // GAME STATE & UTILITIES
    // ==========================================

    async checkKingInCheck() {
        const response = await fetch('/api/board');
        const boardData = await response.json();
        if (boardData.KingInCheck) {
            this.playSound('check');  
        }
    }

    async updateGameOverState() {
        const response = await fetch('/api/board');
        const boardData = await response.json();

        if (boardData.CheckMate || boardData.StaleMate) {
            this.isGameOver = true;
            this.playSound('gameover');
            
            const message = boardData.CheckMate 
                ? `${boardData.currentWhiteTurn ? 'Black' : 'White'} wins by checkmate.` 
                : 'The game ends in a stalemate. No winner.';
                
            this.showGameOverMessage(message);
        }
    }

    showGameOverMessage(message) {
        if (this.gameOverOverlay && this.gameOverTitle && this.gameOverDetail) {
            this.gameOverTitle.textContent = 'Game Over';
            this.gameOverDetail.textContent = message;
            this.gameOverOverlay.classList.add('visible');
        }
    }

    playSound(type) {
        new Audio(`./chess_sounds/${type}.wav`).play();
        this.soundMade = true;
    }

    convertIndicesToPosition(row, col) {
        return String.fromCharCode(97 + row) + col;
    }
 
    convertPositionToIndices(position) {
        return {
            row: position.charCodeAt(0) - 97,
            col: parseInt(position[1])
        };
    }


    async handleUndo() {
        // 1. Prevent undo if a piece is actively being dragged or promoted
        if (this.activePiece || this.floatingPiece || this.isPromoting) return;

        // 2. Call the backend API
        const response = await fetch('/api/undo', { method: 'POST' });
        const responseText = await response.text();

        // 3. If a move was successfully undone (backend didn't return null)
        if (responseText) {
            // A. Remove all current pieces from the board
            document.querySelectorAll('.piece').forEach(piece => piece.remove());

            // B. Clear any lingering visual highlights
            this.clearLastMoveHighlights();
            this.clearSelectedHighlight();
            this.clearLegalMoveIndicators();

            // C. Re-fetch the board state directly from the backend
            await this.fetchBoard();

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
            // Send the string to the backend, encoding it so special characters (like '/') don't break the URL
            const response = await fetch(`/api/load-fen?fen=${encodeURIComponent(fenString)}`, { method: 'POST' });
            const success = await response.json();

            if (success) {
                // 1. Clear the physical board DOM
                document.querySelectorAll('.piece').forEach(piece => piece.remove());
                this.clearLastMoveHighlights();
                this.clearSelectedHighlight();
                this.clearLegalMoveIndicators();

                // 2. Reset frontend state
                this.activePiece = null;
                if (this.floatingPiece) {
                    this.floatingPiece.remove();
                    this.floatingPiece = null;
                }
                this.isGameOver = false;
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

    toggleFlip() {
        this.flipBoard = !this.flipBoard;
        
        if (this.flipBoard) {
            this.boardElement.classList.add('flipped');
        } else {
            this.boardElement.classList.remove('flipped');
        }
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}
    




// Initialize the Game
const game = new ChessUI();
game.createGrid();
game.fetchBoard().then(() => game.updateGameOverState());