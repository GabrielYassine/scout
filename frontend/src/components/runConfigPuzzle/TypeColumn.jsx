import DroppedPiece from "./DroppedPiece.jsx";
import "./TypeColumn.css";

export default function TypeColumn({ type, pieces, index, onRemovePiece, onPieceHover, onPieceLeave }) {
    const pieceArray = Array.isArray(pieces) ? pieces : [];

    return (
        <div className="type-column" style={{ "--i": index }}>
            <div className="type-column-pieces">
                {pieceArray.map((piece, idx) => (
                    <DroppedPiece
                        key={`${piece.id}-${idx}`}
                        id={piece.id}
                        label={piece.label}
                        type={type}
                        index={idx}
                        puzzleData={piece.puzzleData}
                        onRemove={() => onRemovePiece(type, idx)}
                        onHover={onPieceHover}
                        onLeave={onPieceLeave}
                    />
                ))}
            </div>
        </div>
    );
}

