import { useDraggable } from "@dnd-kit/core";
import { maskStyle } from "../../util/puzzleMasks.js";
import { canonicalizePiece } from "../../util/puzzleGeometry.js";
import "./DroppedPiece.css";

const BASE_KEYS = [
    "1000","2000",
    "1010","1020","1100","1200","2020","2200",
    "1110","1120","1210","2120","2210","2220",
    "1111","1112","1122","1212","1222","2222",
];

export default function DroppedPiece({ id, label, type, index, onRemove, onHover, onLeave }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id: `dropped-${type}-${index}-${id}`,
        data: {
            label,
            type,
            originalId: id,
            fromType: type,
            fromIndex: index,
        },
    });

    function hashStr(s) {
        let h = 2166136261;
        for (let i = 0; i < s.length; i++) {
            h ^= s.charCodeAt(i);
            h = Math.imul(h, 16777619);
        }
        return h >>> 0;
    }

    const seedStr = `${type}:${index}:${id}`;
    const h = hashStr(seedStr);

    const key = BASE_KEYS[h % BASE_KEYS.length];
    const rotation = ((h >>> 8) % 4) * 90;
    const mirrorX = ((h >>> 10) & 1) === 1;

    const transform = `rotate(${rotation}deg)${mirrorX ? " scaleX(-1)" : ""}`;

    return (
        <div
            ref={setNodeRef}
            className={`dropped-puzzle-piece ${isDragging ? "dragging" : ""}`}
            {...listeners}
            {...attributes}
            onMouseEnter={() => onHover?.(type, id)}
            onMouseLeave={() => onLeave?.()}
        >
            <div
                className="dropped-puzzle-shape"
                style={{
                    ...maskStyle(key),
                    transform,
                    transformOrigin: "center",
                }}
            />
            <div className="dropped-puzzle-content">
                <div className="dropped-puzzle-piece-title">{label}</div>
            </div>
        </div>
    );

}
