export function rotateCW([N, E, S, W]) {
    return [W, N, E, S];
}

export function mirrorX([N, E, S, W]) {
    return [N, W, S, E];
}

function toKey(arr) {
    return arr.join("");
}

export function canonicalizePiece(neswInput) {
    const start =
        typeof neswInput === "string"
            ? neswInput.split("").map(Number)
            : [...neswInput];

    let best = null;
    let current = [...start];

    for (let rot = 0; rot < 4; rot++) {
        {
            const key = toKey(current);
            if (!best || key < best.key) best = { key, rotation: rot * 90, mirrorX: false };
        }
        {
            const mirrored = mirrorX(current);
            const key = toKey(mirrored);
            if (!best || key < best.key) best = { key, rotation: rot * 90, mirrorX: true };
        }

        current = rotateCW(current);
    }

    return best;
}