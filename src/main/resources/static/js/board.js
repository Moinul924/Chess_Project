const board = document.querySelector('.grid-board');

for (let i = 0; i<81; i++)
{
    const square = document.createElement('div')
    square.className = 'square';
    let row = String.fromCharCode(97 + Math.floor(i / 9)); 
    let col = i % 9; 
    if((Math.floor(i / 9) + col) % 2 == 0) 
    {   
        square.classList.add('light-square');
    }
    else
    {
        square.classList.add('dark-square');
    }
    square.id = row + col;
   
    board.appendChild(square);

}


addPiece('BKing', 'e0');
addPiece('BQueen', 'd0');
addPiece('WBishop', 'f0');

// Global variables to keep track of what we are dragging
let activePiece = null;
let floatingPiece = null;

function addPiece(piece, position) {
    const square = document.getElementById(position);
    const img = document.createElement('img');
    let color = (piece[0] == 'B') ? 'Black' : 'White';
    
    img.src = './piece_images/Images-80px/' + color + "/" + piece + '-80px.png';
    img.className = 'piece';
    img.id = piece + '-' + position;
    
    img.draggable = false; 
    
    img.addEventListener('mousedown', mouseDownHandler);    
    square.appendChild(img);
}

function mouseDownHandler(e) {
    e.preventDefault();
    
    activePiece = e.target;
    
    // 1. Hide the original 80px piece while we drag
    activePiece.style.opacity = '0'; 

    floatingPiece = document.createElement('img');
    
    // Swap the 80px filename for the 128px filename
    floatingPiece.src = activePiece.src.replaceAll('80px', '128px');    
    floatingPiece.className = 'dragging-piece';
    
    // 3. Put the floating piece exactly where the mouse is
    moveAt(e.pageX, e.pageY);
    document.body.appendChild(floatingPiece);

    // 4. Listen for mouse movement and mouse release anywhere on the screen
    document.addEventListener('mousemove', mouseMoveHandler);
    document.addEventListener('mouseup', mouseUpHandler);
}

// Helper function to move the floating piece
function moveAt(pageX, pageY) {
    if (floatingPiece) {
        floatingPiece.style.left = pageX + 'px';
        floatingPiece.style.top = pageY + 'px';
    }
}

function mouseMoveHandler(e) {
    moveAt(e.pageX, e.pageY);
}

function mouseUpHandler(e) {
    document.removeEventListener('mousemove', mouseMoveHandler);
    document.removeEventListener('mouseup', mouseUpHandler);

    if (!floatingPiece || !activePiece) return;

    
    let targetElement = document.elementFromPoint(e.clientX, e.clientY);

    floatingPiece.remove();
    floatingPiece = null;
    activePiece.style.opacity = '1';

    if (targetElement) {
        // If they dropped it directly onto another piece, find the square underneath it
        if (targetElement.classList.contains('piece')) {
            targetElement = targetElement.parentElement;
        }

        // If the target is a valid square on the board, move the 80px piece there
        if (targetElement.classList.contains('square')) {
             targetElement.appendChild(activePiece);
             
             // Update the piece's ID to reflect its new position (optional but recommended)
             let pieceName = activePiece.id.split('-')[0];
             activePiece.id = pieceName + '-' + targetElement.id;
        }
    }

    activePiece = null;
}