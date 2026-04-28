/*
 * SelectedPieceSection renders the parameter controls
 * for all currently selected pieces of one type.
 */
import SidebarSection from "../SidebarSection.jsx";
import ParamField from "../ParamField.jsx";

export default function SelectedPieceSection({
  type,
  title,
  open,
  setOpen,
  pieces,
  params,
  catalogLoading,
  disabled,
  findPieceDef,
  setParam,
}) {
  if (!Array.isArray(pieces) || pieces.length === 0) return null;

  return (
    <SidebarSection
      title={title}
      isOpen={open[type]}
      onToggle={() => setOpen((current) => ({ ...current, [type]: !current[type] }))}
    >
      {pieces.map((piece, index) => {
        const pieceDef = findPieceDef(type, piece.id);
        const pieceParams = params[type] ?? {};

        return (
          <div key={`${piece.id}-${index}`} className="ll-piece-container">
            <div className="ll-selected-piece">
              {pieces.length > 1 && (
                <span className="ll-piece-number">{index + 1}.</span>
              )}
              {piece.label}
            </div>

            {!catalogLoading && pieceDef?.params?.length > 0 && (
              <div className="ll-subsection">
                {pieceDef.params.map((def) => (
                  <ParamField
                    key={def.key}
                    def={def}
                    disabled={disabled}
                    value={
                      pieceParams[def.key] !== undefined
                        ? pieceParams[def.key]
                        : def.defaultValue
                    }
                    onValueChange={(value) => setParam(type, def, value)}
                  />
                ))}
              </div>
            )}
          </div>
        );
      })}
    </SidebarSection>
  );
}