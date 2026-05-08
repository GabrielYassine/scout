/**
 * Modal editor for route graph cities and depots.
 * @author s235257
 */

import { useEffect, useRef, useState } from "react";
import RouteVisualization from "./RouteVisualization.jsx";
import { normalizeCities, sanitizeCity } from "./routeVisualizationData.js";
import "@/features/run/styles/RouteGraphModal.css";

const toInt = (value) => Math.round(Number(value) || 0);

function isNumericField(field) {
  return field === "x" || field === "y" || field === "demand";
}

export default function RouteGraphModal({
  isOpen,
  onClose,
  nodes = [],
  instanceType = "TSP",
  onCitiesUpdate,
  onCityChange,
  onAddCity,
  onRemoveCity,
  onDepotToggle,
}) {
  const scrollRef = useRef(null);
  const [canScrollDown, setCanScrollDown] = useState(false);

  // Normalize city data and create a signature for depot configuration to optimize RouteVisualization re-rendering.
  const cities = normalizeCities(nodes);
  const depotSignature = cities.map((city) => (city.isDepot ? "1" : "0")).join("");

  const updateScrollIndicator = () => {
    const element = scrollRef.current;
    if (!element) return;

    const hasMoreBelow =
      element.scrollTop + element.clientHeight < element.scrollHeight - 2;

    setCanScrollDown(hasMoreBelow);
  };

  useEffect(() => {
    if (!isOpen) return;

    requestAnimationFrame(updateScrollIndicator);
  }, [isOpen, cities.length]);

  if (!isOpen) return null;

  const handleCitiesChange = (updatedCities) => {
    onCitiesUpdate?.(updatedCities.map(sanitizeCity));
  };

  const handleCityChange = (index, field, value) => {
    onCityChange?.(index, field, isNumericField(field) ? toInt(value) : value);
  };

  const tspData = {
    tour: null,
    cities,
    tourLength: 0,
  };

  return (
    <div className="tsp-modal-overlay" onClick={onClose}>
      <div
        className="tsp-modal-container"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="tsp-modal-header">
          <h3>Route Graph Editor</h3>

          <button className="tsp-modal-close" onClick={onClose}>
            <span>×</span>
          </button>
        </div>

        <div className="tsp-modal-content">
          <div className="tsp-modal-split">
            <div className="tsp-modal-graph">
              <RouteVisualization
                key={`route-editor-${cities.length}-${depotSignature}`}
                tspData={tspData}
                width={600}
                height={400}
                editable
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

                <div
                  ref={scrollRef}
                  className="cities-scroll"
                  onScroll={updateScrollIndicator}
                >
                  {cities.map((city, index) => (
                    <div key={city.id} className="city-row">
                      <span className="city-col-id">{city.nodeId}</span>

                      <button
                        type="button"
                        className={`depot-toggle ${city.isDepot ? "active" : ""}`}
                        onClick={() => onDepotToggle?.(index)}
                        title={city.isDepot ? "Depot" : "Customer"}
                        disabled={instanceType !== "VRP"}
                      />

                      <input
                        type="number"
                        step="1"
                        className="city-input"
                        value={city.x}
                        onChange={(event) =>
                          handleCityChange(index, "x", event.target.value)
                        }
                      />

                      <input
                        type="number"
                        step="1"
                        className="city-input"
                        value={city.y}
                        onChange={(event) =>
                          handleCityChange(index, "y", event.target.value)
                        }
                      />

                      <input
                        type="number"
                        step="1"
                        className="city-input"
                        value={city.demand}
                        onChange={(event) =>
                          handleCityChange(index, "demand", event.target.value)
                        }
                        disabled={instanceType !== "VRP" || city.isDepot}
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

                {canScrollDown && (
                  <div className="cities-scroll-indicator" aria-hidden="true">
                    ▼
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="tsp-modal-actions">
            <button className="btn btn--yellow" onClick={() => onAddCity?.()}>
              + Add City
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}