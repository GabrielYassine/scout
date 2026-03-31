import "./LabLeftbar.css";
import { useState, useEffect } from "react";

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

  const [draft, setDraft] = useState(value ?? "");

  useEffect(() => {
    setDraft(value ?? "");
  }, [value]);

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
          value={draft}
          min={min ?? undefined}
          max={max ?? undefined}
          disabled={disabled}
          onChange={(e) => {
            setDraft(e.target.value);
            onValueChange(e.target.value);
          }}
          onBlur={() => {
            if (draft === "" || draft == null) {
              setDraft("0");
              onValueChange("0");
            }
          }}
        />
      </label>
    );
  }

  return (
    <label className="ll-field" key={key}>
      <span className="ll-label">{label ?? key}</span>
      <input
        type="text"
        value={draft}
        disabled={disabled}
        onChange={(e) => {
          setDraft(e.target.value);
          onValueChange(e.target.value);
        }}
        onBlur={() => {
          if (draft === "" || draft == null) {
            setDraft("0");
            onValueChange("0");
          }
        }}
      />
    </label>
  );
}