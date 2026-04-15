class ChessUI {

    
    
    
    // 1. The constructor runs automatically when you create the class
    constructor() {
        this.boardElement = document.querySelector('.grid-board');
        
        // Global variables are now class properties!
        this.activePiece = null;
        this.floatingPiece = null;

        // CRITICAL: We have to 'bind' event listeners to the class so that 
        // the word 'this' always refers to the ChessUI, not the HTML image.
        this.mouseDownHandler = this.mouseDownHandler.bind(this);
        this.mouseMoveHandler = this.mouseMoveHandler.bind(this);
        this.mouseUpHandler = this.mouseUpHandler.bind(this);
    }

    // 2. We moved the grid generation into its own method
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

        if(pawnPromotionInProgress) {
            return;
        }

        this.activePiece = e.target;
        
        this.activePiece.style.opacity = '0'; 

        this.floatingPiece = document.createElement('img');
        this.floatingPiece.src = this.activePiece.src.replaceAll('80px', '128px');    
        this.floatingPiece.className = 'dragging-piece';
        
        this.moveAt(e.pageX, e.pageY);
        document.body.appendChild(this.floatingPiece);

        document.addEventListener('mousemove', this.mouseMoveHandler);
        document.addEventListener('mouseup', this.mouseUpHandler);

        // Notify Java!
        const pieceId = this.activePiece.id; 
        const [pieceName, position] = pieceId.split('-'); 
        let indices = this.convertPositionToIndices(position);
        
        
        const url = `/api/click?row=${indices.row}&col=${indices.col}&name=${pieceName}`; 
        const response = await fetch(url, { method: 'POST' });
        const legalSquares = await response.json(); 
        console.log("Legal moves received from Java:", legalSquares);
    
        for(let square of legalSquares) {
            let row = String.fromCharCode(97 + square.row);
            let col = square.col;
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
        document.removeEventListener('mousemove', this.mouseMoveHandler);
        document.removeEventListener('mouseup', this.mouseUpHandler);

        if (!this.floatingPiece || !this.activePiece) return;
        
        let targetElement = document.elementFromPoint(e.clientX, e.clientY);

        this.floatingPiece.remove();
        this.floatingPiece = null;
        this.activePiece.style.opacity = '1';

        // 1. Figure out exactly which square the mouse landed on
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
            console.log("Move result received from Java:", isMoveLegal);


            if (isMoveLegal) {

                // If there's an enemy piece there, remove it
                if (targetElement.classList.contains('piece')) {
                    targetSquareElement.removeChild(targetElement);
                }

                // Move our piece to the new square
                targetSquareElement.appendChild(this.activePiece);
                
                // Update the ID of our piece
                this.activePiece.id = pieceName + '-' + targetSquareElement.id;

                if (pieceName.includes('Pawn') && (position.row === 0 || position.row === 7)) {
                    this.showPromotionMenu(pieceName, targetSquareElement, position.row, position.col);
                }
            }

            const response2 = await fetch('/api/castle');
            const result = await response2.json();

            if (result.first === true) {
                let StartRookRow = result.second[0][0];
                let StartRookCol = result.second[0][1];
                let EndRookRow = result.second[1][0];
                let EndRookCol = result.second[1][1];
                this.CastlingMoveRook(StartRookRow,StartRookCol,EndRookRow,EndRookCol);
            }
        }

        this.activePiece = null;
        this.clearLegalMoveIndicators();
        
    }

    showPromotionMenu(pieceName, targetSquareElement, row, col) {
        pawnPromotionInProgress = true;
        const colorLetter = pieceName[0]; 
        const folderColor = colorLetter === 'W' ? 'White' : 'Black';

        const menu = document.createElement('div');
        menu.className = 'promotion-menu';
        
        if (row === 0) { 
            menu.style.bottom = '100%';
        } else if (row === 7) {
            menu.style.top = '100%';
        }

        const options = ['Queen', 'Knight', 'Rook', 'Bishop'];

        options.forEach(option => {
            const img = document.createElement('img');
            const newPieceName = colorLetter + option; // e.g., 'WQueen'
            
            img.src = `./piece_images/Images-80px/${folderColor}/${newPieceName}-80px.png`;
            img.className = 'promotion-option';
            
            // 4. What happens when they click an option!
            img.addEventListener('click', async () => {
                pawnPromotionInProgress = false;
                
                menu.remove(); 

                // B. Update the piece visually on the board
                const pawnOnBoard = targetSquareElement.querySelector('.piece');
                if (pawnOnBoard) {
                    pawnOnBoard.src = img.src;
                    pawnOnBoard.id = newPieceName + '-' + targetSquareElement.id;
                }

                // C. Tell Java to actually change the piece in the backend!
                await fetch(`/api/promote?row=${row}&col=${col}&newPiece=${option}`, { method: 'POST' });
            });

            menu.appendChild(img);
        });

        // Show the menu on the screen!
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






let pawnPromotionInProgress = false;
const game = new ChessUI();
game.createGrid();
game.fetchBoard();