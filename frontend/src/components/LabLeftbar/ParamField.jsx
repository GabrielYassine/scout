import "./LabLeftbar.css";

// parses a raw input value according to the parameter type
// @author s235257
export function parseValue(type, raw) {
  if (raw == null) return raw;
  if (type === "boolean") return Boolean(raw);
  if (type === "int" || type === "long" || type === "double") {
    if (raw === "") return "";
    return Number(raw);
  }

  return raw;
}

// takes a parameter definition and a raw input value, and makes sure the value becomes of the correct type
// @author s235257
export default function ParamField({ def, value, onValueChange, disabled }) {
  const { key, label, type, min, max } = def;

  if (type === "boolean") {
    return (
      <label className="ll-field" key={key}>
        <span className="ll-label">{label ?? key}</span>
        <input
          type="checkbox"
          checked={Boolean(value)}
          disabled={disabled}
          onChange={(e) => onValueChange(e.target.checked)}
        />
      </label>
    );
  }

  if (type === "int" || type === "long" || type === "double") {
    return (
      <label className="ll-field" key={key}>
        <span className="ll-label">{label ?? key}</span>
        <input
          type="number"
          value={value ?? ""}
          min={min ?? undefined}
          max={max ?? undefined}
          disabled={disabled}
          onChange={(e) => onValueChange(e.target.value)}
        />
      </label>
    );
  }

  return (
    <label className="ll-field" key={key}>
      <span className="ll-label">{label ?? key}</span>
      <input
        type="text"
        value={value ?? ""}
        disabled={disabled}
        onChange={(e) => onValueChange(e.target.value)}
      />
    </label>
  );
}