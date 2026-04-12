import "./LabLeftbar.css";
import FieldRow from "./FieldRow.jsx";

export default function ParamField({ def, value, onValueChange, disabled }) {
  const { key, label, type, min, max } = def;

  const resolvedValue = value ?? "";
  const fieldLabel = label ?? key;

  if (type === "boolean") {
    return (
      <FieldRow label={fieldLabel}>
        <input
          className="field-input"
          type="checkbox"
          checked={Boolean(value)}
          disabled={disabled}
          onChange={(e) => onValueChange(e.target.checked)}
        />
      </FieldRow>
    );
  }

  if (type === "int" || type === "long" || type === "double") {
    return (
      <FieldRow label={fieldLabel}>
        <input
          className="field-input"
          type="number"
          value={resolvedValue}
          min={min ?? undefined}
          max={max ?? undefined}
          disabled={disabled}
          onChange={(e) => onValueChange(e.target.value)}
          onBlur={() => {
            if (resolvedValue === "" || resolvedValue == null) {
              onValueChange("0");
            }
          }}
        />
      </FieldRow>
    );
  }

  return (
    <FieldRow label={fieldLabel}>
      <input
        className="field-input"
        type="text"
        value={resolvedValue}
        disabled={disabled}
        onChange={(e) => onValueChange(e.target.value)}
        onBlur={() => {
          if (resolvedValue === "" || resolvedValue == null) {
            onValueChange("0");
          }
        }}
      />
    </FieldRow>
  );
}