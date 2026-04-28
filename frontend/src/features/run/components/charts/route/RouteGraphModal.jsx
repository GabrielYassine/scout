/**
 * Modal wrapper for viewing a TSP/VRP route visualization in a larger graph view.
 * Used when route data should be inspected separately from the normal chart panel.
 */
import TSPVisualization from "./RouteVisualization.jsx";
import "@/features/run/styles/RouteGraphModal.css";

const toInt = (value) => Math.round(Number(value) || 0);

export default function TSPGraphModal({
  isOpen,
  onClose,
  tspInstance,
  onCitiesUpdate,
  nodes = [],
  instanceType = "TSP",
  onCityChange,
  onAddCity,
  onRemoveCity,
  onDepotToggle,
}) {

  const cities = nodes.length
    ? nodes.map((node) => ({
        id: node.id,
        nodeId: node.nodeId,
        x: node.x,
        y: node.y,
        demand: node.demand ?? 0,
        isDepot: node.isDepot === true,
      }))
    : (tspInstance?.cities ?? []).map((city) => ({
        ...city,
        id: city.id,
        nodeId: city.nodeId,
        isDepot: city.isDepot === true,
      }));

  const depotSignature = nodes.map((node) => (node.isDepot ? "1" : "0")).join("");

  const handleCitiesChange = (updatedCities) => {
    onCitiesUpdate?.(
      updatedCities.map((city) => ({
        ...city,
        x: toInt(city.x),
        y: toInt(city.y),
      }))
    );
  };

  const handleCityChange = (index, field, value) => {
    onCityChange?.(index, field, field === "x" || field === "y" || field === "demand" ? toInt(value) : value);
  };

  if (!isOpen) return null;

  const tspData = {
    tour: null,
    cities,
    tourLength: 0,
  };

  return (
    <div className="tsp-modal-overlay" onClick={onClose}>
      <div className="tsp-modal-container" onClick={(e) => e.stopPropagation()}>
        <div className="tsp-modal-header">
          <h3>Route Graph Editor</h3>
          <button className="tsp-modal-close" onClick={onClose}><span>×</span></button>
        </div>

        <div className="tsp-modal-content">
          <div className="tsp-modal-split">
            <div className="tsp-modal-graph">
              <TSPVisualization
                key={`tsp-editor-${tspInstance?.name ?? "tsp"}-${cities.length}-${depotSignature}`}
                tspData={tspData}
                width={600}
                height={400}
                editable={true}
                onCitiesChange={handleCitiesChange}
              />
            </div>

            <div className="tsp-modal-list">
              <div className="cities-list">
                <div className="cities-header">
                  <span className="city-col-id">ID</span>
                  <span className="city-col-action">Depot</span>
                  <span className="city-col-coord">X</span>
                  <span className="city-col-coord">Y</span>
                  <span className="city-col-coord">Demand</span>
                  <span className="city-col-action"></span>
                </div>

                <div className="cities-scroll">
                  {nodes.map((node, index) => (
                    <div key={node.key ?? `node-${index}`} className="city-row">
                      <span className="city-col-id">{node.nodeId}</span>
                      <button
                          type="button"
                          className={`depot-toggle ${node.isDepot ? "active" : ""}`}
                          onClick={() => onDepotToggle?.(index)}
                          title={node.isDepot ? "Depot" : "Customer"}
                          disabled={instanceType !== "VRP"}
                      />
                      <input
                        type="number"
                        step="1"
                        className="city-input"
                        value={node.x}
                        onChange={(e) => handleCityChange(index, "x", e.target.value)}
                      />
                      <input
                        type="number"
                        step="1"
                        className="city-input"
                        value={node.y}
                        onChange={(e) => handleCityChange(index, "y", e.target.value)}
                      />
                      <input
                        type="number"
                        step="1"
                        className="city-input"
                        value={node.demand ?? 0}
                        onChange={(e) => handleCityChange(index, "demand", e.target.value)}
                        disabled={instanceType !== "VRP" || node.isDepot}
                      />
                      <button
                        className="city-remove-btn"
                        onClick={() => onRemoveCity?.(index)}
                        title="Remove city"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>

                <button className="add-city-btn" onClick={() => onAddCity?.()}>
                  + Add City
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
