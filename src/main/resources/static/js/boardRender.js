export function createGrid(boardElement) {
    for (let i = 0; i < 64; i++) {
        const square = document.createElement('div');
        square.className = 'square';

        let row = String.fromCharCode(97 + Math.floor(i / 8));
        let col = i % 8;

        square.classList.add((Math.floor(i / 8) + col) % 2 === 0 ? 'light-square' : 'dark-square');
        square.id = row + col;

        boardElement.appendChild(square);
    }
}

export function addPiece(piece, position, onMouseDown) {
    const square = document.getElementById(position);
    const img = document.createElement('img');
    const colorFolder = piece[0] === 'B' ? 'Black' : 'White';

    img.src = `./piece_images/Images-80px/${colorFolder}/${piece}-80px.png`;
    img.className = 'piece';
    img.id = `${piece}-${position}`;
    img.draggable = false;

    img.addEventListener('mousedown', onMouseDown);
    square.appendChild(img);
}

export function clearSelectedHighlight() {
    document.querySelectorAll('.selected-square').forEach(el => el.classList.remove('selected-square'));
}

export function clearLastMoveHighlights() {
    document.querySelectorAll('.last-move-highlight').forEach(el => el.classList.remove('last-move-highlight'));
}

export function clearLegalMoveIndicators() {
    document.querySelectorAll('.legal-move-indicator, .legal-capture-indicator').forEach(el => el.remove());
}

export function renderLegalMoveIndicators(legalMoves) {
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

export function showPromotionMenu(pieceName, targetSquareElement, row, onSelect) {
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
            menu.remove();

            const pawnOnBoard = targetSquareElement.querySelector('.piece');
            if (pawnOnBoard) {
                pawnOnBoard.src = img.src;
                pawnOnBoard.id = `${newPieceName}-${targetSquareElement.id}`;
            }

            await onSelect(option);
        });

        menu.appendChild(img);
    });

    targetSquareElement.appendChild(menu);
}

export function executeCastlingUI(startSquare, endSquare) {
    const startId = convertIndicesToPosition(startSquare.row, startSquare.col);
    const endId = convertIndicesToPosition(endSquare.row, endSquare.col);

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

export function executeEnPassantUI(capturedSquare) {
    const captureId = convertIndicesToPosition(capturedSquare.row, capturedSquare.col);
    document.getElementById(captureId)?.querySelector('.piece')?.remove();
}

export function executePromotionUI(promotionSquare, newPiece, onMouseDown) {
    const pieceName = newPiece.name;
    const pieceColor = newPiece.colour[0];
    const pawnId = convertIndicesToPosition(promotionSquare.row, promotionSquare.col);
    document.getElementById(pawnId)?.querySelector('.piece')?.remove();
    addPiece(pieceColor + pieceName, String.fromCharCode(97 + promotionSquare.row) + promotionSquare.col, onMouseDown);
}

export function showGameOverMessage({ overlay, title, detail }, message) {
    if (overlay && title && detail) {
        title.textContent = 'Game Over';
        detail.textContent = message;
        overlay.classList.add('visible');

    }
}

export function convertIndicesToPosition(row, col) {
    return String.fromCharCode(97 + row) + col;
}

export function convertPositionToIndices(position) {
    return {
        row: position.charCodeAt(0) - 97,
        col: parseInt(position[1])
    };
}
