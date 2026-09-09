// Pure sound playback - no game state lives here.

export function playSound(type) {
    new Audio(`./chess_sounds/${type}.wav`).play();
}
