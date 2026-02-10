// puzzleGenerator.js
/**
 * Puzzle piece generation with wall and adjacency constraints
 * Edge encoding: 0 = FLAT, 1 = TAB, 2 = HOLE
 * Key format: "NESW" (North, East, South, West)
 */

const FLAT = 0;
const TAB = 1;
const HOLE = 2;

/**
 * Mapping from logical keys to render keys and transforms.
 * renderKey = which base PNG/mask to use
 * rotation/mirrorX = how to transform it to match the logicalKey
 */
const PNG_MAP = {
    "1000": { renderKey: "1000", rotation: 0, mirrorX: false },
    "1001": { renderKey: "1100", rotation: 0, mirrorX: true },
    "1002": { renderKey: "1200", rotation: 0, mirrorX: true },
    "1010": { renderKey: "1010", rotation: 0, mirrorX: false },
    "1011": { renderKey: "1110", rotation: 0, mirrorX: true },
    "1012": { renderKey: "1210", rotation: 0, mirrorX: true },
    "1020": { renderKey: "1020", rotation: 0, mirrorX: false },
    "1021": { renderKey: "1120", rotation: 0, mirrorX: true },
    "1022": { renderKey: "2210", rotation: 180, mirrorX: false },
    "1100": { renderKey: "1100", rotation: 0, mirrorX: false },
    "1101": { renderKey: "1110", rotation: 270, mirrorX: false },
    "1102": { renderKey: "1120", rotation: 270, mirrorX: true },
    "1110": { renderKey: "1110", rotation: 0, mirrorX: false },
    "1111": { renderKey: "1111", rotation: 0, mirrorX: false },
    "1112": { renderKey: "1112", rotation: 0, mirrorX: false },
    "1120": { renderKey: "1120", rotation: 0, mirrorX: false },
    "1121": { renderKey: "1112", rotation: 270, mirrorX: false },
    "1122": { renderKey: "1122", rotation: 0, mirrorX: false },
    "1200": { renderKey: "1200", rotation: 0, mirrorX: false },
    "1201": { renderKey: "1120", rotation: 270, mirrorX: false },
    "1202": { renderKey: "2120", rotation: 270, mirrorX: false },
    "1210": { renderKey: "1210", rotation: 0, mirrorX: false },
    "1211": { renderKey: "1112", rotation: 0, mirrorX: true },
    "1212": { renderKey: "1212", rotation: 0, mirrorX: false },
    "1220": { renderKey: "2210", rotation: 180, mirrorX: true },
    "1221": { renderKey: "1122", rotation: 0, mirrorX: true },
    "1222": { renderKey: "1222", rotation: 0, mirrorX: false },
    "2000": { renderKey: "2000", rotation: 0, mirrorX: false },
    "2001": { renderKey: "1200", rotation: 270, mirrorX: false },
    "2002": { renderKey: "2200", rotation: 0, mirrorX: true },
    "2010": { renderKey: "1020", rotation: 180, mirrorX: false },
    "2011": { renderKey: "1120", rotation: 180, mirrorX: false },
    "2012": { renderKey: "2210", rotation: 0, mirrorX: true },
    "2020": { renderKey: "2020", rotation: 0, mirrorX: false },
    "2021": { renderKey: "2120", rotation: 0, mirrorX: true },
    "2022": { renderKey: "2220", rotation: 0, mirrorX: true },
    "2100": { renderKey: "1200", rotation: 270, mirrorX: true },
    "2101": { renderKey: "1210", rotation: 270, mirrorX: false },
    "2102": { renderKey: "2210", rotation: 270, mirrorX: false },
    "2110": { renderKey: "1120", rotation: 180, mirrorX: true },
    "2111": { renderKey: "1112", rotation: 90, mirrorX: false },
    "2112": { renderKey: "1122", rotation: 90, mirrorX: false },
    "2120": { renderKey: "2120", rotation: 0, mirrorX: false },
    "2121": { renderKey: "1212", rotation: 90, mirrorX: false },
    "2122": { renderKey: "1222", rotation: 90, mirrorX: false },
    "2200": { renderKey: "2200", rotation: 0, mirrorX: false },
    "2201": { renderKey: "2210", rotation: 270, mirrorX: true },
    "2202": { renderKey: "2220", rotation: 270, mirrorX: false },
    "2210": { renderKey: "2210", rotation: 0, mirrorX: false },
    "2211": { renderKey: "1122", rotation: 90, mirrorX: true },
    "2212": { renderKey: "1222", rotation: 180, mirrorX: false },
    "2220": { renderKey: "2220", rotation: 0, mirrorX: false },
    "2221": { renderKey: "1222", rotation: 90, mirrorX: true },
    "2222": { renderKey: "2222", rotation: 0, mirrorX: false },
    "0000": { renderKey: "0000", rotation: 0, mirrorX: false },
    "0100": { renderKey: "1000", rotation: 90, mirrorX: false },
    "0001": { renderKey: "1000", rotation: 90, mirrorX: true },
    "0010": { renderKey: "1000", rotation: 180, mirrorX: false },
    "0200": { renderKey: "2000", rotation: 90, mirrorX: false },
    "0002": { renderKey: "2000", rotation: 90, mirrorX: true },
    "0020": { renderKey: "2000", rotation: 180, mirrorX: false },
    "0101": { renderKey: "1010", rotation: 90, mirrorX: false },
    "0102": { renderKey: "1020", rotation: 90, mirrorX: false },
    "0201": { renderKey: "1020", rotation: 90, mirrorX: true },
    "0110": { renderKey: "1100", rotation: 90, mirrorX: false },
    "0011": { renderKey: "1100", rotation: 90, mirrorX: true },
    "0120": { renderKey: "1200", rotation: 90, mirrorX: false },
    "0021": { renderKey: "1200", rotation: 90, mirrorX: true },
    "0012": { renderKey: "1200", rotation: 180, mirrorX: false },
    "0210": { renderKey: "1200", rotation: 180, mirrorX: true },
    "0202": { renderKey: "2020", rotation: 90, mirrorX: false },
    "0220": { renderKey: "2200", rotation: 90, mirrorX: false },
    "0022": { renderKey: "2200", rotation: 90, mirrorX: true },
    "0111": { renderKey: "1110", rotation: 90, mirrorX: false },
    "0112": { renderKey: "1120", rotation: 90, mirrorX: false },
    "0211": { renderKey: "1120", rotation: 90, mirrorX: true },
    "0121": { renderKey: "1210", rotation: 90, mirrorX: false },
    "0212": { renderKey: "2120", rotation: 90, mirrorX: false },
    "0221": { renderKey: "2210", rotation: 90, mirrorX: false },
    "0122": { renderKey: "2210", rotation: 90, mirrorX: true },
    "0222": { renderKey: "2220", rotation: 90, mirrorX: false },
};

// -------------------- shared-edge hashing (prevents regen + order dependency) --------------------

function complement(edge) {
    if (edge === TAB) return HOLE;
    if (edge === HOLE) return TAB;
    return FLAT;
}

function randomEdge() {
    return Math.random() < 0.5 ? TAB : HOLE;
}


export function generatePuzzleKey({col, row, totalCols, neighbors = {} }) {
    console.log("  Left:", neighbors.left);
    console.log("  Right:", neighbors.right);
    console.log("  Top:", neighbors.top);
    console.log("  Bottom:", neighbors.bottom);

    let N = null,
        E = null,
        S = null,
        W = null;

    // If there is a wall N/E/W, that edge must be FLAT
    if (col === 0 || neighbors.left?.kind === "wall") W = FLAT;
    if (col === totalCols - 1 || neighbors.right?.kind === "wall") E = FLAT;
    if (row === 0 || neighbors.top?.kind === "wall") N = FLAT;

    // For each neighbor if its a tab we must have a hole, if its a hole we must have a tab
    if (neighbors.left?.kind === "piece" && neighbors.left.edge != null) {
        W = complement(neighbors.left.edge);
    }
    if (neighbors.right?.kind === "piece" && neighbors.right.edge != null) {
        E = complement(neighbors.right.edge);
    }
    if (neighbors.top?.kind === "piece" && neighbors.top.edge != null) {
        N = complement(neighbors.top.edge);
    }
    if (neighbors.bottom?.kind === "piece" && neighbors.bottom.edge != null) {
        S = complement(neighbors.bottom.edge);
    }

    console.log("After pair-fill:", { N, E, S, W });

    // For any remaining edges, we compute each edge randomly for Tab or Hole
    if (N === null) N = randomEdge();
    if (E === null) E = randomEdge();
    if (S === null) S = randomEdge();
    if (W === null) W = randomEdge();

    const logicalKey = `${N}${E}${S}${W}`;
    console.log("Final logical key:", logicalKey); // e.g. "1202"

    const mapping = PNG_MAP[logicalKey] ?? PNG_MAP["0000"];
    console.log("Mapped to:", mapping);

    return {
        logicalKey,
        renderKey: mapping.renderKey,
        rotation: mapping.rotation,
        mirrorX: mapping.mirrorX,
    };
}