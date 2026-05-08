/**
 * Display and edit instance metadata fields for TSP and VRP instances.
 * @author s235257
 */

import FieldRow from "../FieldRow.jsx";
import ParamField from "../ParamField.jsx";
import { EDGE_WEIGHT_TYPE } from "./instanceModel.js";

export default function InstanceFieldsSection({
  view,
  dimension,
  instanceType,
  onTypeChange,
  onCapacityChange,
  onVehicleChange,
}) {
  const isTsp = instanceType === "TSP";

  return (
    <div className="instance-fields">
      <span className="instance-summary-label">Name:</span>
      <span className="instance-summary-value">
        {view.name || "Custom Instance"}
      </span>

      <span className="instance-summary-label">Comment:</span>
      <span className="instance-summary-value">
        {view.comment || "No comment"}
      </span>

      <span className="instance-summary-label">Dimension:</span>
      <span className="instance-summary-value">{dimension}</span>

      <FieldRow label="Instance Type">
        <select
          className="field-input"
          value={instanceType}
          onChange={(event) => onTypeChange(event.target.value)}
        >
          <option value="TSP">TSP</option>
          <option value="VRP">VRP</option>
        </select>
      </FieldRow>

      {/* Capacity and vehicle count only apply to VRP instances. */}
      <ParamField
        def={{
          key: "capacity",
          label: "Capacity",
          type: "double",
          min: 0,
          defaultValue: 0,
        }}
        value={view.capacity}
        disabled={isTsp}
        onValueChange={onCapacityChange}
      />

      <ParamField
        def={{
          key: "numberOfVehicles",
          label: "Vehicle Amount",
          type: "int",
          min: 1,
          defaultValue: 1,
        }}
        value={view.numberOfVehicles}
        disabled={isTsp}
        onValueChange={onVehicleChange}
      />

      {/* SCOUT currently supports EUC_2D instances only. */}
      <FieldRow label="Edge Weight Type">
        <input
          className="field-input"
          value={EDGE_WEIGHT_TYPE}
          readOnly
          disabled
        />
      </FieldRow>
    </div>
  );
}