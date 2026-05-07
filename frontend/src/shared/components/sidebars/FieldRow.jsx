/**
 * Form row wrapper for label and field content.
 * @author s235257
 */

import "@/shared/components/styles/FormFields.css";

export default function FieldRow({ label, children }) {
  return (
    <label className="field-row">
      <span className="field-label">{label}</span>
      {children}
    </label>
  );
}
