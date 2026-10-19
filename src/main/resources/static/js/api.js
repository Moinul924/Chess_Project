const BASE_URL = '/api';

// Single-player games (bot / pass-and-play) use an id this tab makes up and keeps in sessionStorage.
// Online games use the id the SERVER created when it paired two players. It's kept in the address bar
// as ?game=... (so a refresh gets back into the game) and deliberately not stored, so it never gets
// reused for a single-player game.
const onlineGameIdFromLink = new URLSearchParams(window.location.search).get('game');
let gameId = onlineGameIdFromLink ?? sessionStorage.getItem('chess-game-id');
if (!gameId) {
    gameId = crypto.randomUUID();
    sessionStorage.setItem('chess-game-id', gameId);
}

export function getOnlineGameIdFromLink() {
    return onlineGameIdFromLink;
}

export function useOnlineGame(id) {
    gameId = id;
}

function gameUrl(endpoint, parameters = {}) {
    const query = new URLSearchParams({ ...parameters, gameId });
    return `${BASE_URL}/${endpoint}?${query}`;
}

let csrf = null;
const csrfReady = fetch(`${BASE_URL}/csrf`)
    .then(response => response.json())
    .then(token => { csrf = token; });

// The game API needs a login. If the player isn't signed in (or their session expired),
// the server replies 401, so send them to the sign-in page instead of showing a broken board.
function redirectIfLoggedOut(response) {
    if (response.status === 401) {
        window.location.href = '/singInOrRegister.html';
    }
    return response;
}

async function get(url) {
    return redirectIfLoggedOut(await fetch(url));
}

async function post(url) {
    await csrfReady;
    return redirectIfLoggedOut(
        await fetch(url, { method: 'POST', headers: { [csrf.headerName]: csrf.token } }));
}

// mode is 'ENGINE' (vs bot) or 'LOCAL' (pass and play); colour is the side the player takes against the bot.
export async function startNewGame(mode = 'LOCAL', colour = 'WHITE'){
    const response = await post(gameUrl('start-new-game', { mode, colour }));
    return response.ok;
}

// ----- Online games -----

// Matchmaking. All three return { status, gameId }, where status is
// 'WAITING', 'MATCHED' (gameId is set) or 'NOT_SEARCHING'. They return null if the request failed.
export async function joinQueue() {
    const response = await post(`${BASE_URL}/pvp/queue`);
    return response.ok ? response.json() : null;
}

export async function checkQueue() {
    const response = await get(`${BASE_URL}/pvp/queue`);
    return response.ok ? response.json() : null;
}

export async function leaveQueue() {
    await post(`${BASE_URL}/pvp/queue/leave`);
}

// Used when the page is closed while searching. sendBeacon still gets sent as the page goes away,
// but it can't set headers, so the CSRF token goes in the URL instead.
export function leaveQueueOnExit() {
    if (!csrf) return;
    const query = new URLSearchParams({ [csrf.parameterName]: csrf.token });
    navigator.sendBeacon(`${BASE_URL}/pvp/queue/leave?${query}`, new Blob([], { type: 'application/octet-stream' }));
}

// { myColour, whiteName, blackName, opponentJoined, version, whiteToMove, gameOver }
export async function getOnlineState() {
    const response = await get(gameUrl('pvp/state'));
    return response.ok ? response.json() : null;
}

export function closeGameOnExit() {
    if (!csrf) return;
    // sendBeacon can't set headers, so the token goes in the URL as a parameter instead.
    const emptyBody = new Blob([], { type: 'application/octet-stream' });
    navigator.sendBeacon(gameUrl('close-game', { [csrf.parameterName]: csrf.token }), emptyBody);
}

export async function getBoard() {
    const response = await get(gameUrl('board'));
    return response.json();
}

export async function getLegalMoves(row, col, name) {
    const response = await post(gameUrl('click', { row, col, name }));
    // A refused click (e.g. not your turn) means no legal moves to show.
    return response.ok ? response.json() : [];
}

export async function getCapturedPieces() {
    const response = await get(gameUrl('get-captured-pieces'));
    return response.json();
}

export async function sendMove(row, col, name) {
    const response = await post(gameUrl('moved', { row, col, name }));
    // A refused move (e.g. not your turn) is treated the same as an illegal one.
    return response.ok ? response.json() : false;
}

export async function getEngineMove() {
    const response = await post(gameUrl('EngineMove'));
    return response.ok ? response.json() : null;
}

export async function undoMove() {
    const response = await post(gameUrl('undo'));
    return response.text();
}

export async function getLastMoveCastling() {
    const response = await get(gameUrl('castle'));
    return response.text();
}

export async function getLastMovePromotion() {
    const response = await get(gameUrl('promotion'));
    return response.text();
}

export async function getLastMoveEnPassant() {
    const response = await get(gameUrl('EnPassant'));
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
    const response = await get(gameUrl('reset'));
    return response.json();
}

export async function getCurrentTurn() {
    const response = await get(gameUrl('current-turn'));
    return response.json();
}

