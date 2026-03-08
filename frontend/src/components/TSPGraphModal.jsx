import { useState, useEffect } from "react";
import TSPVisualization from "./TSPVisualization/TSPVisualization.jsx";
import "./TSPGraphModal.css";

export default function TSPGraphModal({ isOpen, onClose, tspInstance, onCitiesUpdate }) {
  const [localCities, setLocalCities] = useState([]);

  useEffect(() => {
    if (tspInstance?.cities) {
      setLocalCities([...tspInstance.cities]);
    }
  }, [tspInstance]);

  const handleCitiesChange = (updatedCities) => {
    setLocalCities(updatedCities);
    if (onCitiesUpdate) {
      onCitiesUpdate(updatedCities);
    }
  };

  const handleCityInputChange = (index, field, value) => {
    const numValue = parseFloat(value);
    if (isNaN(numValue)) return;

    const updatedCities = [...localCities];
    updatedCities[index] = {
      ...updatedCities[index],
      [field]: numValue
    };

    setLocalCities(updatedCities);
    if (onCitiesUpdate) {
      onCitiesUpdate(updatedCities);
    }
  };

  if (!isOpen) return null;

  const tspData = {
    tour: null,
    cities: localCities,
    tourLength: 0
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
