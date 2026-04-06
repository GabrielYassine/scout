import "./FormFields.css";

export default function FieldRow({ label, children }) {
  return (
    <label className="field-row">
      <span className="field-label">{label}</span>
      {children}
    </label>
  );
}
