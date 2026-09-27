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

let csrf = null;
const csrfReady = fetch(`${BASE_URL}/csrf`)
    .then(response => response.json())
    .then(token => { csrf = token; });

async function post(url) {
    await csrfReady;
    return fetch(url, { method: 'POST', headers: { [csrf.headerName]: csrf.token } });
}

export async function startNewGame(){
    const response = await post(gameUrl('start-new-game'));
    return response.ok;
}

export function closeGameOnExit() {
    if (!csrf) return;
    // sendBeacon can't set headers, so the token goes in the URL as a parameter instead.
    const emptyBody = new Blob([], { type: 'application/octet-stream' });
    navigator.sendBeacon(gameUrl('close-game', { [csrf.parameterName]: csrf.token }), emptyBody);
}

export async function getBoard() {
    const response = await fetch(gameUrl('board'));
    return response.json();
}

export async function getLegalMoves(row, col, name) {
    const response = await post(gameUrl('click', { row, col, name }));
    return response.json();
}

export async function getCapturedPieces() {
    const response = await fetch(gameUrl('get-captured-pieces'));
    return response.json();
}

export async function sendMove(row, col, name) {
    const response = await post(gameUrl('moved', { row, col, name }));
    return response.json();
}

export async function getEngineMove() {
    const response = await post(gameUrl('EngineMove'));
    return response.json();
}

export async function undoMove() {
    const response = await post(gameUrl('undo'));
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
    await post(gameUrl('promote-for-user', { row, col, newPiece }));
}

export async function loadFen(fen) {
    const response = await post(gameUrl('load-fen', { fen }));
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

