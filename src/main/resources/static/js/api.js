// Thin wrapper around every backend endpoint under /api.
// Nothing outside this file should call fetch() directly.

const BASE_URL = '/api';

export async function getBoard() {
    const response = await fetch(`${BASE_URL}/board`);
    return response.json();
}

export async function getLegalMoves(row, col, name) {
    const response = await fetch(`${BASE_URL}/click?row=${row}&col=${col}&name=${name}`, { method: 'POST' });
    return response.json();
}

export async function sendMove(row, col, name) {
    const response = await fetch(`${BASE_URL}/moved?row=${row}&col=${col}&name=${name}`, { method: 'POST' });
    return response.json();
}

export async function getEngineMove() {
    const response = await fetch(`${BASE_URL}/EngineMove`, { method: 'POST' });
    return response.json();
}

export async function undoMove() {
    const response = await fetch(`${BASE_URL}/undo`, { method: 'POST' });
    return response.text();
}

export async function getLastMoveCastling() {
    const response = await fetch(`${BASE_URL}/castle`);
    return response.text();
}

export async function getLastMovePromotion() {
    const response = await fetch(`${BASE_URL}/promotion`);
    return response.text();
}

export async function getLastMoveEnPassant() {
    const response = await fetch(`${BASE_URL}/EnPassant`);
    return response.text();
}

export async function promotePawn(row, col, newPiece) {
    await fetch(`${BASE_URL}/promote_for_user?row=${row}&col=${col}&newPiece=${newPiece}`, { method: 'POST' });
}

export async function loadFen(fen) {
    const response = await fetch(`${BASE_URL}/load-fen?fen=${encodeURIComponent(fen)}`, { method: 'POST' });
    return response.json();
}

export async function resetGame() {
    const response = await fetch(`${BASE_URL}/reset`, { method: 'GET' });
    return response.json();
}