import DroppedPiece from "./DroppedPiece.jsx";
import "./TypeColumn.css";

export default function TypeColumn({ type, pieces, onRemovePiece }) {
    return (
        <div className="type-column">
            <div className="type-column-pieces">
                {pieces.map((piece, index) => (
                    <DroppedPiece
                        key={`${piece.id}-${index}`}
                        id={piece.id}
                        label={piece.label}
                        type={type}
                        index={index}
                        onRemove={() => onRemovePiece(type, index)}
                    />
                ))}
            </div>
        </div>
    );
}
