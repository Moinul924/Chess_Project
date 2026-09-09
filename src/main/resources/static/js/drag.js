// Mouse drag-and-drop input handling.
//
// These are plain functions rather than a class because they need to read
// and write state that belongs to the GameController (activePiece,
// floatingPiece, isGameOver, ...) - each one takes that controller as
// `game` and operates on it directly, the same way the original methods
// operated on `this`. GameController just forwards its bound handlers here.

import { clearSelectedHighlight, clearLegalMoveIndicators } from './boardRender.js';

function moveAt(game, pageX, pageY) {
    if (game.floatingPiece) {
        game.floatingPiece.style.left = `${pageX}px`;
        game.floatingPiece.style.top = `${pageY}px`;
    }
}

export async function mouseDownHandler(game, e) {
    if (game.isGameOver || game.isPromoting || game.activePiece || game.floatingPiece) return;
    e.preventDefault();

    game.activePiece = e.target;
    game.activePiece.style.opacity = '0';

    // Highlight the square we just picked the piece up from
    clearSelectedHighlight();
    game.activePiece.parentElement.classList.add('selected-square');

    // Create the enlarged floating piece
    game.floatingPiece = document.createElement('img');
    game.floatingPiece.src = game.activePiece.src.replaceAll('80px', '128px');
    game.floatingPiece.className = 'dragging-piece';
    moveAt(game, e.pageX, e.pageY);
    document.body.appendChild(game.floatingPiece);

    document.addEventListener('mousemove', game.mouseMoveHandler);
    document.addEventListener('mouseup', game.mouseUpHandler);

    await game.showLegalMoves();
}

export function mouseMoveHandler(game, e) {
    moveAt(game, e.pageX, e.pageY);
}

export async function mouseUpHandler(game, e) {
    if (game.isGameOver) return;

    document.removeEventListener('mousemove', game.mouseMoveHandler);
    document.removeEventListener('mouseup', game.mouseUpHandler);

    if (!game.floatingPiece || !game.activePiece) return;

    // Save the start square before we change the DOM
    const startSquareElement = game.activePiece.parentElement;

    // Clean up UI
    game.floatingPiece.remove();
    game.floatingPiece = null;
    game.activePiece.style.opacity = '1';
    clearLegalMoveIndicators();
    clearSelectedHighlight(); // Remove the "picked up" highlight

    // Determine target square
    let targetElement = document.elementFromPoint(e.clientX, e.clientY);
    let targetSquareElement = targetElement?.classList.contains('piece') ? targetElement.parentElement : targetElement;

    if (targetSquareElement?.classList.contains('square')) {
        await game.processMoveAttempt(targetElement, targetSquareElement, startSquareElement);
        if (game.playEngine && !game.isGameOver && !game.isPromoting && game.moveMade) {
            await game.sleep(500);
            await game.handleEngineMove();
            game.moveMade = false; // Reset after engine move
        }
    }

    game.activePiece = null;
}
