const BASE_URL = '/api';

let gameId =sessionStorage.getItem('chess-game-id');;
if (!gameId) {
    gameId = crypto.randomUUID();
    sessionStorage.setItem('chess-game-id', gameId);
}

function gameUrl(endpoint, parameters = {}) {
    const query = new URLSearchParams({ ...parameters, gameId });
    return `${BASE_URL}/${endpoint}?${query}`;
}

export async function startNewGame(){
    const response = await fetch(gameUrl('start-new-game'), { method: 'POST' });
    return response.ok;
}

export function closeGameOnExit() {
    const emptyBody = new Blob([], { type: 'application/octet-stream' });
    navigator.sendBeacon(gameUrl('close-game'), emptyBody);
}

export async function getBoard() {
    const response = await fetch(gameUrl('board'));
    return response.json();
}

export async function getLegalMoves(row, col, name) {
    const response = await fetch(gameUrl('click', { row, col, name }), { method: 'POST' });
    return response.json();
}

export async function sendMove(row, col, name) {
    const response = await fetch(gameUrl('moved', { row, col, name }), { method: 'POST' });
    return response.json();
}

export async function getEngineMove() {
    const response = await fetch(gameUrl('EngineMove'), { method: 'POST' });
    return response.json();
}

export async function undoMove() {
    const response = await fetch(gameUrl('undo'), { method: 'POST' });
    return response.text();
}

export async function getLastMoveCastling() {
    const response = await fetch(gameUrl('castle'));
    return response.text();
}

export async function getLastMovePromotion() {
    const response = await fetch(gameUrl('promotion'));
    return response.text();
}

export async function getLastMoveEnPassant() {
    const response = await fetch(gameUrl('EnPassant'));
    return response.text();
}

export async function promotePawn(row, col, newPiece) {
    await fetch(gameUrl('promote-for-user', { row, col, newPiece }), { method: 'POST' });
}

export async function loadFen(fen) {
    const response = await fetch(gameUrl('load-fen', { fen }), { method: 'POST' });
    return response.json();
}

export async function resetGame() {
    const response = await fetch(gameUrl('reset'), { method: 'GET' });
    return response.json();
}

export async function getCurrentTurn() {
    const response = await fetch(gameUrl('current-turn'), { method: 'GET' });
    return response.json();
}