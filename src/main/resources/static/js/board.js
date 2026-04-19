let pawnPromotionInProgress = false;

class ChessUI {
    
    constructor() {
        this.boardElement = document.querySelector('.grid-board');
        this.gameOverOverlay = document.getElementById('game-over-overlay');
        this.gameOverTitle = document.getElementById('game-over-title');
        this.gameOverDetail = document.getElementById('game-over-detail');
        
        this.activePiece = null;
        this.floatingPiece = null;

        this.mouseDownHandler = this.mouseDownHandler.bind(this);
        this.mouseMoveHandler = this.mouseMoveHandler.bind(this);
        this.mouseUpHandler = this.mouseUpHandler.bind(this);
    }

    createGrid() {
        for (let i = 0; i < 64; i++) {
            const square = document.createElement('div')
            square.className = 'square';
            
            let row = String.fromCharCode(97 + Math.floor(i / 8)); 
            let col = i % 8; 
            
            if((Math.floor(i / 8) + col) % 2 == 0) {   
                square.classList.add('light-square');
            } else {
                square.classList.add('dark-square');
            }
            square.id = row + col;
           
            this.boardElement.appendChild(square);
        }
    }

    addPiece(piece, position) {
        const square = document.getElementById(position);
        const img = document.createElement('img');
        let color = (piece[0] == 'B') ? 'Black' : 'White';
        
        img.src = `./piece_images/Images-80px/${color}/${piece}-80px.png`;
        img.className = 'piece';
        img.id = piece + '-' + position;
        img.draggable = false; 
        
        img.addEventListener('mousedown', this.mouseDownHandler);    
        square.appendChild(img);
    }

    async mouseDownHandler(e) {
        if(GameOver) return;
        e.preventDefault();

        if(pawnPromotionInProgress) return;
        if (this.activePiece !== null || this.floatingPiece !== null) return;

        this.activePiece = e.target;
        this.activePiece.style.opacity = '0'; 

        this.floatingPiece = document.createElement('img');
        this.floatingPiece.src = this.activePiece.src.replaceAll('80px', '128px');    
        this.floatingPiece.className = 'dragging-piece';
        
        this.moveAt(e.pageX, e.pageY);
        document.body.appendChild(this.floatingPiece);

        document.addEventListener('mousemove', this.mouseMoveHandler);
        document.addEventListener('mouseup', this.mouseUpHandler);

        this.clearLegalMoveIndicators();

        const pieceId = this.activePiece.id; 
        const [pieceName, position] = pieceId.split('-'); 
        let indices = this.convertPositionToIndices(position);
        
        const url = `/api/click?row=${indices.row}&col=${indices.col}&name=${pieceName}`; 
        const response = await fetch(url, { method: 'POST' });
        const legalMoves = await response.json(); 
        
        if (this.activePiece === null) return; 
    
        for(let move of legalMoves) {
            let row = String.fromCharCode(97 + move.endSquare.row);
            let col = move.endSquare.col;
            let squareId = row + col;
            let squareElement = document.getElementById(squareId);
            
            if (squareElement) {
                const indicator = document.createElement('div'); 
                if (squareElement.querySelector('.piece')) {
                    indicator.className = 'legal-capture-indicator';
                } else {
                    indicator.className = 'legal-move-indicator';
                }
                squareElement.appendChild(indicator);
            }
        }
    }

    clearLegalMoveIndicators() {
        const dots = document.querySelectorAll('.legal-move-indicator');
        dots.forEach(dot => dot.remove());
        const rings = document.querySelectorAll('.legal-capture-indicator');
        rings.forEach(ring => ring.remove());
    }

    moveAt(pageX, pageY) {
        if (this.floatingPiece) {
            this.floatingPiece.style.left = pageX + 'px';
            this.floatingPiece.style.top = pageY + 'px';
        }
    }

    mouseMoveHandler(e) {
        this.moveAt(e.pageX, e.pageY);
    }

    async mouseUpHandler(e) {
        if(GameOver) return;
        document.removeEventListener('mousemove', this.mouseMoveHandler);
        document.removeEventListener('mouseup', this.mouseUpHandler);

        if (!this.floatingPiece || !this.activePiece) return;
        
        let targetElement = document.elementFromPoint(e.clientX, e.clientY);

        this.floatingPiece.remove();
        this.floatingPiece = null;
        this.activePiece.style.opacity = '1';

        let targetSquareElement = targetElement;
        if (targetElement && targetElement.classList.contains('piece')) {
            targetSquareElement = targetElement.parentElement;
        }

        if (targetSquareElement && targetSquareElement.classList.contains('square')) {
            let pieceName = this.activePiece.id.split('-')[0];
            let position = this.convertPositionToIndices(targetSquareElement.id);
        
            const url = `/api/moved?row=${position.row}&col=${position.col}&name=${pieceName}`; 
            let response = await fetch(url, { method: 'POST' });
            let isMoveLegal = await response.json(); 

            if (isMoveLegal) {
                let promotionPending = false;

                // 1. Visual Move Updates
                if (targetElement.classList.contains('piece')) {
                    targetSquareElement.removeChild(targetElement);
                }
                targetSquareElement.appendChild(this.activePiece);
                this.activePiece.id = pieceName + '-' + targetSquareElement.id;

                // 2. Check Promotion
                if (pieceName.includes('Pawn') && (position.row === 0 || position.row === 7)) {
                    promotionPending = true;
                    this.showPromotionMenu(pieceName, targetSquareElement, position.row, position.col);
                }

                // 3. Check Castling
                const response2 = await fetch('/api/castle');
                const text = await response2.text(); 
                if (text) {
                    const result = JSON.parse(text); 
                    let StartRookRow = result.rookStartSquare.row;
                    let StartRookCol = result.rookStartSquare.col;
                    let EndRookRow = result.rookEndSquare.row;
                    let EndRookCol = result.rookEndSquare.col;
                    this.CastlingMoveRook(StartRookRow, StartRookCol, EndRookRow, EndRookCol);
                }

                // 4. Check En Passant
                const response3 = await fetch('/api/EnPassant');
                const text2 = await response3.text();
                if(text2){
                    const result = JSON.parse(text2);
                    let capturedPawnSquareRow = result.capturedPawnSquare.row;
                    let capturedPawnSquareCol = result.capturedPawnSquare.col;
                    this.EnPassantRemoveCapturedPawn(capturedPawnSquareRow, capturedPawnSquareCol);
                }

                if (!promotionPending) {
                    await this.updateGameOverState();
                }
            }
        }
        
        
        this.activePiece = null;
        this.clearLegalMoveIndicators();
    }
    
    async updateGameOverState() {
        const response = await fetch('/api/board');
        const boardData = await response.json();

        if (boardData.CheckMate) {
            GameOver = true;
            const winner = boardData.currentWhiteTurn ? 'Black' : 'White';
            this.showGameOverMessage(`${winner} wins by checkmate.`);
            return true;
        }

        if (boardData.StaleMate) {
            GameOver = true;
            this.showGameOverMessage('The game ends in a stalemate. No winner.');
            return true;
        }

        return false;
    }


    showPromotionMenu(pieceName, targetSquareElement, row, col) {
        pawnPromotionInProgress = true;
        const colorLetter = pieceName[0]; 
        const folderColor = colorLetter === 'W' ? 'White' : 'Black';

        const menu = document.createElement('div');
        menu.className = 'promotion-menu';
        
        if (row === 0) menu.style.bottom = '100%';
        else if (row === 7) menu.style.top = '100%';

        const options = ['Queen', 'Knight', 'Rook', 'Bishop'];

        options.forEach(option => {
            const img = document.createElement('img');
            const newPieceName = colorLetter + option; 
            img.src = `./piece_images/Images-80px/${folderColor}/${newPieceName}-80px.png`;
            img.className = 'promotion-option';
            
            img.addEventListener('click', async () => {
                pawnPromotionInProgress = false;
                menu.remove(); 

                const pawnOnBoard = targetSquareElement.querySelector('.piece');
                if (pawnOnBoard) {
                    pawnOnBoard.src = img.src;
                    pawnOnBoard.id = newPieceName + '-' + targetSquareElement.id;
                }
                await fetch(`/api/promote?row=${row}&col=${col}&newPiece=${option}`, { method: 'POST' });
                await this.updateGameOverState();
            });

            menu.appendChild(img);
        });

        targetSquareElement.appendChild(menu);
    }

    async CastlingMoveRook(StartRookRow,StartRookCol,EndRookRow,EndRookCol) {
        let startSquareId = this.convertIndicesToPosition(StartRookRow,StartRookCol);
        let endSquareId = this.convertIndicesToPosition(EndRookRow,EndRookCol);

        const startSquare = document.getElementById(startSquareId);
        const endSquare = document.getElementById(endSquareId);

        if (startSquare && endSquare) {
            const rookPiece = startSquare.querySelector('.piece');
            if (rookPiece) {
                endSquare.appendChild(rookPiece);
                let pieceName = rookPiece.id.split('-')[0];
                rookPiece.id = pieceName + '-' + endSquareId;
            }
        }
    }

    async EnPassantRemoveCapturedPawn(SquareRow,SquareCol){
        let captureSquareId = this.convertIndicesToPosition(SquareRow,SquareCol);
        const captureSquare = document.getElementById(captureSquareId);
        if(captureSquare){
            const capturedPiece = captureSquare.querySelector('.piece');
            if(capturedPiece){
                capturedPiece.remove();
            }
        }
    }

    convertIndicesToPosition(row, col) {
        const rowChar = String.fromCharCode(97 + row);
        return rowChar + col;
    }
 
    convertPositionToIndices(position) {
        const row = position.charCodeAt(0) - 97;
        const col = parseInt(position[1]);
        return {row, col};
    }

    async clearBoard() {
        const pieces = document.querySelectorAll('.piece');
        pieces.forEach(piece => piece.remove());
    }

    showGameOverMessage(message) {
        if (!this.gameOverOverlay || !this.gameOverTitle || !this.gameOverDetail) {
            return;
        }

        this.gameOverTitle.textContent = 'Game Over';
        this.gameOverDetail.textContent = message;
        this.gameOverOverlay.classList.add('visible');
    }


    async fetchBoard() {
        const response = await fetch('/api/board');
        const boardData = await response.json();
        const gridArray = boardData.board;
        
        for (let row = 0; row < 8; row++) {
            for (let col = 0; col < 8; col++) {
                let currentSquare = gridArray[row][col];
                if (currentSquare.piece !== null) {
                    const PieceName = currentSquare.piece.name;
                    const PieceColor = currentSquare.piece.colour[0]; 
                    const PieceNameWithColor = PieceColor + PieceName; 
                    this.addPiece(PieceNameWithColor, String.fromCharCode(97 + row) + col);
                }
            }
        }
    }
}

let GameOver = false;
const game = new ChessUI();
game.createGrid();
game.fetchBoard().then(() => game.updateGameOverState());