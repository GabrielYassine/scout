import TSPVisualization from "./TSPVisualization/TSPVisualization.jsx";
import "./TSPGraphModal.css";

export default function TSPGraphModal({ isOpen, onClose, tspInstance, onCitiesUpdate }) {
  const cities = tspInstance?.cities ?? [];

  const handleCitiesChange = (updatedCities) => {
    onCitiesUpdate?.(updatedCities);
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
          <h3>TSP Graph Editor</h3>
          <button className="tsp-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="tsp-modal-content">
          <div className="tsp-modal-graph">
            <TSPVisualization
              key={`tsp-editor-${tspInstance?.name ?? "tsp"}-${cities.length}`}
              tspData={tspData}
              width={600}
              height={400}
              editable={true}
              onCitiesChange={handleCitiesChange}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
