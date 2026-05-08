/**
 * Interactive TSP/VRP route visualization component.
 *
 * The component can render both normal TSP tours and VRP route lists.
 * In editable mode, cities can be dragged and the view can be panned/zoomed.
 * During algorithm runs it can also render pheromone intensities as a heatmap
 * behind the actual tour edges.
 *
 * @author s235257
 */

import { useState, useRef, useCallback, useMemo, useEffect } from "react";
import {
  buildCitiesKey,
  buildRouteSequence,
  calculateRouteDistance,
  extractRunSourceData,
  extractTspSourceData,
  normalizeRoutes,
  sanitizeCity,
} from "./routeVisualizationData.js";
import "@/features/run/styles/RouteVisualization.css";

const ROUTE_COLORS = [
  "#3b82f6",
  "#ef4444",
  "#22c55e",
  "#f59e0b",
  "#8b5cf6",
  "#14b8a6",
  "#e11d48",
  "#84cc16",
];

const MIN_ZOOM = 0.25;
const MAX_ZOOM = 8;
const ZOOM_SENSITIVITY = 0.0015;

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
const toInt = (value) => Math.round(Number(value) || 0);

function pheromoneStyle(intensity) {
  const lightness = 88 - 40 * intensity;

  return {
    strokeWidth: 0.6 + 4.4 * intensity,
    color: `hsl(24, 92%, ${lightness}%)`,
  };
}

export default function RouteVisualization({
  tspData,
  run,
  width,
  height,
  editable = false,
  onCitiesChange,
}) {
  const containerRef = useRef(null);
  const svgRef = useRef(null);
  const panStartRef = useRef(null);
  const dragOffsetRef = useRef({ x: 0, y: 0 });
  const viewRef = useRef({ zoom: 1, panX: 0, panY: 0 });

  const [isPanning, setIsPanning] = useState(false);
  const [draggedCity, setDraggedCity] = useState(null);
  const [dragPosition, setDragPosition] = useState(null);
  const [view, setView] = useState({ zoom: 1, panX: 0, panY: 0 });
  const [dimensions, setDimensions] = useState({
    width: width || 800,
    height: height || 600,
  });

  useEffect(() => {
    viewRef.current = view;
  }, [view]);

  // Prefer run data when available, since it contains the algorithm output.
  const sourceData = useMemo(() => {
    return extractRunSourceData(run) ?? extractTspSourceData(tspData);
  }, [tspData, run]);

  const initialCities = useMemo(() => sourceData?.cities ?? [], [sourceData]);
  const [cities, setCities] = useState(() => [...initialCities]);

  // Used to detect real instance changes without resetting city positions on every render.
  const initialCitiesKey = useMemo(
    () => buildCitiesKey(initialCities),
    [initialCities]
  );

  const lastInitialKeyRef = useRef(initialCitiesKey);

  useEffect(() => {
    if (draggedCity !== null) return;
    if (lastInitialKeyRef.current === initialCitiesKey) return;

    lastInitialKeyRef.current = initialCitiesKey;
    setCities([...initialCities]);
  }, [initialCitiesKey, draggedCity, initialCities]);

  // Keep the SVG responsive unless fixed dimensions are provided.
  useEffect(() => {
    if (!containerRef.current) return;

    const updateDimensions = () => {
      if (!containerRef.current) return;

      const { width: measuredWidth, height: measuredHeight } =
        containerRef.current.getBoundingClientRect();

      setDimensions({
        width: width || Math.max(300, measuredWidth),
        height: height || Math.max(200, measuredHeight),
      });
    };

    updateDimensions();

    const resizeObserver = new ResizeObserver(updateDimensions);
    resizeObserver.observe(containerRef.current);

    return () => resizeObserver.disconnect();
  }, [width, height]);

  // Maps problem coordinates into SVG coordinates while preserving relative distances.
  const { minX, minY, scale, offsetX, offsetY } = useMemo(() => {
    if (!cities.length) {
      return {
        minX: 0,
        minY: 0,
        scale: 1,
        offsetX: 0,
        offsetY: 0,
      };
    }

    const padding = 30;
    const { width: svgWidth, height: svgHeight } = dimensions;

    const xs = cities.map((city) => city.x);
    const ys = cities.map((city) => city.y);

    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);

    const dataWidth = maxX - minX || 1;
    const dataHeight = maxY - minY || 1;

    const scaleX = (svgWidth - 2 * padding) / dataWidth;
    const scaleY = (svgHeight - 2 * padding) / dataHeight;
    const scale = Math.min(scaleX, scaleY);

    const offsetX = padding + (svgWidth - 2 * padding - dataWidth * scale) / 2;
    const offsetY = padding + (svgHeight - 2 * padding - dataHeight * scale) / 2;

    return { minX, minY, scale, offsetX, offsetY };
  }, [cities, dimensions]);

  const toSVGCoords = useCallback(
    (x, y) => {
      const baseX = (x - minX) * scale + offsetX;
      const baseY = dimensions.height - ((y - minY) * scale + offsetY);

      return {
        x: baseX,
        y: baseY,
      };
    },
    [minX, minY, scale, offsetX, offsetY, dimensions.height]
  );

  // Converts mouse/SVG coordinates back into problem coordinates for city dragging.
  const fromSVGCoords = useCallback(
    (svgX, svgY) => {
      const baseX = (svgX - view.panX) / view.zoom;
      const baseY = (svgY - view.panY) / view.zoom;

      return {
        x: (baseX - offsetX) / scale + minX,
        y: (dimensions.height - baseY - offsetY) / scale + minY,
      };
    },
    [minX, minY, scale, offsetX, offsetY, dimensions.height, view]
  );

  const getSVGPoint = useCallback((clientX, clientY) => {
    if (!svgRef.current) {
      return { x: 0, y: 0 };
    }

    const rect = svgRef.current.getBoundingClientRect();

    return {
      x: clientX - rect.left,
      y: clientY - rect.top,
    };
  }, []);

  const handleMouseDown = useCallback(
    (event, cityIndex) => {
      if (!editable) return;

      event.preventDefault();
      event.stopPropagation();

      const city = cities[cityIndex];
      if (!city) return;

      const svgPoint = getSVGPoint(event.clientX, event.clientY);
      const dataCoords = fromSVGCoords(svgPoint.x, svgPoint.y);

      // Preserve the cursor offset so the city does not jump when dragging starts.
      dragOffsetRef.current = {
        x: dataCoords.x - city.x,
        y: dataCoords.y - city.y,
      };

      setDraggedCity(cityIndex);
      setIsPanning(false);
      panStartRef.current = null;
    },
    [cities, editable, fromSVGCoords, getSVGPoint]
  );

  const handlePanMouseDown = useCallback(
    (event) => {
      if (!editable || draggedCity !== null || event.button !== 0) return;

      event.preventDefault();

      const point = getSVGPoint(event.clientX, event.clientY);

      panStartRef.current = {
        point,
        panX: viewRef.current.panX,
        panY: viewRef.current.panY,
      };

      setIsPanning(true);
    },
    [editable, draggedCity, getSVGPoint]
  );

  const handleMouseMove = useCallback(
    (event) => {
      if (draggedCity !== null) {
        const svgPoint = getSVGPoint(event.clientX, event.clientY);
        const dataCoords = fromSVGCoords(svgPoint.x, svgPoint.y);

        const nextCoords = {
          x: toInt(dataCoords.x - dragOffsetRef.current.x),
          y: toInt(dataCoords.y - dragOffsetRef.current.y),
        };

        setDragPosition(nextCoords);

        setCities((prevCities) =>
          prevCities.map((city, index) =>
            index === draggedCity ? { ...city, ...nextCoords } : city
          )
        );

        return;
      }

      if (!isPanning || !panStartRef.current) return;

      const point = getSVGPoint(event.clientX, event.clientY);
      const dx = point.x - panStartRef.current.point.x;
      const dy = point.y - panStartRef.current.point.y;
      const { panX, panY } = panStartRef.current;

      setView((prev) => ({
        ...prev,
        panX: panX + dx,
        panY: panY + dy,
      }));
    },
    [draggedCity, getSVGPoint, fromSVGCoords, isPanning]
  );

  const finishInteraction = useCallback(() => {
    if (draggedCity !== null && editable && onCitiesChange) {
      onCitiesChange(cities.map(sanitizeCity));
    }

    setDraggedCity(null);
    setDragPosition(null);
    setIsPanning(false);
    panStartRef.current = null;
    dragOffsetRef.current = { x: 0, y: 0 };
  }, [draggedCity, editable, onCitiesChange, cities]);

  const handleWheel = useCallback(
    (event) => {
      if (!editable) return;

      event.preventDefault();

      const point = getSVGPoint(event.clientX, event.clientY);
      const currentView = viewRef.current;

      const nextZoom = clamp(
        currentView.zoom * Math.exp(-event.deltaY * ZOOM_SENSITIVITY),
        MIN_ZOOM,
        MAX_ZOOM
      );

      // Zoom around the cursor so the point under the mouse stays visually stable.
      const worldX = (point.x - currentView.panX) / currentView.zoom;
      const worldY = (point.y - currentView.panY) / currentView.zoom;

      setView({
        zoom: nextZoom,
        panX: point.x - worldX * nextZoom,
        panY: point.y - worldY * nextZoom,
      });
    },
    [editable, getSVGPoint]
  );

  const depotIndex = useMemo(
    () => cities.findIndex((city) => city.isDepot),
    [cities]
  );

  const buildRoutePath = useCallback(
    (route) => {
      const sequence = buildRouteSequence(route, depotIndex);
      if (!sequence.length) return "";

      const pathPoints = sequence
        .map((cityIndex) => {
          const city = cities[cityIndex];
          if (!city) return null;

          const coords = toSVGCoords(city.x, city.y);
          return `${coords.x},${coords.y}`;
        })
        .filter(Boolean);

      return pathPoints.join(" ");
    },
    [cities, depotIndex, toSVGCoords]
  );

  const routePaths = useMemo(() => {
    const routes = normalizeRoutes(sourceData?.tour);
    if (!routes.length || !cities.length) return [];

    return routes
      .map((route, index) => ({
        key: `route-${index}`,
        color: ROUTE_COLORS[index % ROUTE_COLORS.length],
        points: buildRoutePath(route),
      }))
      .filter((route) => route.points);
  }, [buildRoutePath, cities.length, sourceData]);

  // Recalculate the length when city positions are changed in the editor.
  const currentTourLength = useMemo(() => {
    const routes = normalizeRoutes(sourceData?.tour);
    if (!routes.length || !cities.length) return 0;

    return routes.reduce((sum, route) => {
      const sequence = buildRouteSequence(route, depotIndex);
      return sum + calculateRouteDistance(sequence, cities);
    }, 0);
  }, [sourceData, cities, depotIndex]);

  const pheromoneEdges = useMemo(() => {
    if (!run?.series?.pheromoneHeatmap || !cities.length) {
      return [];
    }

    const heatmaps = run.series.pheromoneHeatmap;
    const tours = run.series.tspTour || [];

    if (!heatmaps.length) return [];

    // Match the heatmap as closely as possible to the latest displayed tour.
    const heatmapIndex = Math.min(
      heatmaps.length - 1,
      Math.max(0, tours.length - 1)
    );

    const matrix = heatmaps[heatmapIndex];

    if (!Array.isArray(matrix) || !Array.isArray(matrix[0])) {
      return [];
    }

    const size = Math.min(matrix.length, cities.length);
    if (size === 0) return [];

    const rawEdges = [];

    // Use the strongest direction for each undirected edge.
    for (let i = 0; i < size; i += 1) {
      for (let j = i + 1; j < size; j += 1) {
        const value = Math.max(
          Number(matrix[i]?.[j] ?? 0),
          Number(matrix[j]?.[i] ?? 0)
        );

        if (Number.isFinite(value) && value > 0) {
          rawEdges.push({ a: i, b: j, value });
        }
      }
    }

    if (!rawEdges.length) return [];

    const values = rawEdges.map((edge) => edge.value);
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);
    const range = maxValue - minValue;

    return rawEdges
      .map(({ a, b, value }) => {
        const cityA = cities[a];
        const cityB = cities[b];

        if (!cityA || !cityB) {
          return null;
        }

        const pointA = toSVGCoords(cityA.x, cityA.y);
        const pointB = toSVGCoords(cityB.x, cityB.y);

        // Normalize each edge relative to the weakest and strongest visible pheromone.
        const intensity =
          range === 0 ? 0.45 : clamp((value - minValue) / range, 0, 1);

        return {
          x1: pointA.x,
          y1: pointA.y,
          x2: pointB.x,
          y2: pointB.y,
          intensity,
          ...pheromoneStyle(intensity),
        };
      })
      .filter(Boolean)
      .sort((a, b) => a.intensity - b.intensity);
  }, [run, cities, toSVGCoords]);

  return (
    <div ref={containerRef} className="tsp-visualization">
      {(sourceData?.observedTourLength !== undefined ||
        sourceData?.originalTourLength != null) && (
        <div className="tour-info">
          {sourceData?.observedTourLength !== undefined ? (
            <span className="tour-length">
              Tour Length: {sourceData.observedTourLength.toFixed(2)}
            </span>
          ) : (
            <span className="tour-length">
              Tour Length: {currentTourLength.toFixed(2)}
            </span>
          )}
        </div>
      )}

      <svg
        ref={svgRef}
        width={dimensions.width}
        height={dimensions.height}
        className={`tsp-svg ${editable ? "editable" : "readonly"} ${
          isPanning ? "panning" : ""
        }`}
        onMouseMove={handleMouseMove}
        onMouseUp={finishInteraction}
        onMouseLeave={finishInteraction}
        onMouseDown={handlePanMouseDown}
        onWheel={handleWheel}
      >
        <g transform={`translate(${view.panX} ${view.panY}) scale(${view.zoom})`}>
          <g className="pheromone-layer" pointerEvents="none">
            {pheromoneEdges.map((edge, index) => (
              <line
                key={`ph-${index}`}
                className="pheromone-edge"
                x1={edge.x1}
                y1={edge.y1}
                x2={edge.x2}
                y2={edge.y2}
                stroke={edge.color}
                strokeWidth={edge.strokeWidth}
                strokeLinecap="round"
              />
            ))}
          </g>

          {routePaths.map((route) => (
            <polyline
              key={route.key}
              points={route.points}
              className="tour-path"
              style={{ stroke: route.color }}
              pointerEvents="none"
            />
          ))}

          {cities.map((city, index) => {
            const isDragging = draggedCity === index;
            const displayCity = isDragging && dragPosition ? dragPosition : city;
            const coords = toSVGCoords(displayCity.x, displayCity.y);
            const cityKey = city.id ?? city.nodeId ?? `city-${index}`;

            return (
              <g
                key={cityKey}
                className={`city ${isDragging ? "dragging" : ""} ${
                  editable ? "editable" : "readonly"
                }`}
                pointerEvents="none"
              >
                <circle
                  className={`city-dot ${city.isDepot ? "depot" : ""}`}
                  cx={coords.x}
                  cy={coords.y}
                  onMouseDown={(event) => handleMouseDown(event, index)}
                  pointerEvents="all"
                />

                <text
                  className="city-label"
                  x={coords.x}
                  y={coords.y - 12}
                  pointerEvents="none"
                >
                  {city.nodeId}
                </text>
              </g>
            );
          })}
        </g>
      </svg>
    </div>
  );
}