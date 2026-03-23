import { useState, useRef } from "react";
import "./LabRightbar.css";
import TSPGraphModal from "./TSPGraphModal.jsx";

export default function LabRightbar({ hoverInfo, tspInstance, onTspInstanceChange }) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const fileInputRef = useRef(null);

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setUploadError(null);

    try {
      const content = await file.text();

      const response = await fetch("/api/tsp/upload", {
        method: "POST",
        headers: {
          "Content-Type": "text/plain",
        },
        body: content,
      });

      if (!response.ok) {
        throw new Error("Failed to upload TSP file");
      }

      const data = await response.json();

      // Convert to the format we need for editing
      const cities = data.cities.map(city => ({
        id: city.id,
        x: city.x,
        y: city.y
      }));

      onTspInstanceChange({
        name: data.name,
        cities: cities
      });

    } catch (err) {
      setUploadError(err.message || "Failed to upload file");
      console.error("Upload error:", err);
    } finally {
      setUploading(false);
      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleCityChange = (index, field, value) => {
    const numValue = parseFloat(value);
    if (isNaN(numValue)) return;

    const updatedCities = [...tspInstance.cities];
    updatedCities[index] = {
      ...updatedCities[index],
      [field]: numValue
    };

    onTspInstanceChange({
      ...tspInstance,
      cities: updatedCities
    });
  };

  const handleAddCity = () => {
    const newCity = {
      id: tspInstance.cities.length,
      x: 0,
      y: 0
    };

    onTspInstanceChange({
      ...tspInstance,
      cities: [...tspInstance.cities, newCity]
    });
  };

  const handleRemoveCity = (index) => {
    if (tspInstance.cities.length <= 1) return; // Keep at least one city

    const updatedCities = tspInstance.cities.filter((_, i) => i !== index);
    // Reassign IDs
    const reindexedCities = updatedCities.map((city, idx) => ({
      ...city,
      id: idx
    }));

    onTspInstanceChange({
      ...tspInstance,
      cities: reindexedCities
    });
  };

  const handleCitiesUpdate = (updatedCities) => {
    onTspInstanceChange({
      ...tspInstance,
      cities: updatedCities
    });
  };

  return (
    <section className="lab-rightbar">
      <div className="lr-content">
        <div className="lr-title">Info</div>

        <div className="lr-section">
          <div className="lr-section-header">
            <div className="lr-section-title">Description</div>
          </div>

          <div className="lr-section-body lr-description-body">
            {hoverInfo ? (
              <>
                <div className="lr-selected-piece">{hoverInfo.title}</div>
                <div className="lr-text">{hoverInfo.description}</div>
              </>
            ) : (
              <div className="lr-placeholder">
                Hover over a puzzle piece to see its description.
              </div>
            )}
          </div>
        </div>

        <div className="lr-section">
          <div className="lr-section-header">
            <div className="lr-section-title">Problem Instance</div>
          </div>

          <div className="lr-section-body">
            <div className="tsp-upload-section">
              <input
                ref={fileInputRef}
                type="file"
                accept=".tsp"
                onChange={handleFileUpload}
                disabled={uploading}
                className="tsp-file-input"
                id="tsp-file-upload"
              />
              <label
                htmlFor="tsp-file-upload"
                className={`tsp-upload-btn ${uploading ? 'disabled' : ''}`}
              >
                {uploading ? "Uploading..." : "Upload TSP File"}
              </label>
              <div className="tsp-file-hint">Accepts .tsp file format only</div>
            </div>

            {tspInstance?.name && (
              <div className="tsp-instance-name">
                Instance: <strong>{tspInstance.name}</strong>
              </div>
            )}

            {tspInstance?.cities && tspInstance.cities.length > 0 && (
              <button
                className="view-graph-btn"
                onClick={() => setIsModalOpen(true)}
              >
                View Graph
              </button>
            )}

            <div className="cities-list">
              <div className="cities-header">
                <span className="city-col-id">ID</span>
                <span className="city-col-coord">X</span>
                <span className="city-col-coord">Y</span>
                <span className="city-col-action"></span>
              </div>

              <div className="cities-scroll">
                {tspInstance?.cities.map((city, index) => (
                  <div key={city.id} className="city-row">
                    <span className="city-col-id">{city.id}</span>
                    <input
                      type="number"
                      className="city-input"
                      value={city.x}
                      onChange={(e) => handleCityChange(index, 'x', e.target.value)}
                    />
                    <input
                      type="number"
                      className="city-input"
                      value={city.y}
                      onChange={(e) => handleCityChange(index, 'y', e.target.value)}
                    />
                    <button
                      className="city-remove-btn"
                      onClick={() => handleRemoveCity(index)}
                      disabled={tspInstance.cities.length <= 1}
                      title="Remove city"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>

              <button className="add-city-btn" onClick={handleAddCity}>
                + Add City
              </button>
            </div>
          </div>
        </div>
      </div>

      <TSPGraphModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        tspInstance={tspInstance}
        onCitiesUpdate={handleCitiesUpdate}
      />
    </section>
  );
}
