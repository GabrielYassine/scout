import { useState, useRef, useCallback, useMemo, useEffect } from "react";
import "./TSPVisualization.css";

export default function TSPVisualization({
  tspData,
  run,
  width,
  height,
  editable = false,
  onCitiesChange
}) {
  const containerRef = useRef(null);
  const svgRef = useRef(null);

  const [dimensions, setDimensions] = useState({
    width: width || 800,
    height: height || 600
  });
  const [draggedCity, setDraggedCity] = useState(null);
  const [dragPosition, setDragPosition] = useState(null);
  const [cities, setCities] = useState([]);

  const sourceData = useMemo(() => {
    if (run?.series?.tspTour && run?.series?.tspCities) {
      const tspCitiesSeries = run.series.tspCities;
      const citiesData = Array.isArray(tspCitiesSeries)
        ? (Array.isArray(tspCitiesSeries[0]) ? tspCitiesSeries[0] : tspCitiesSeries)
        : null;

      const tspTourSeries = run.series.tspTour;
      const tourDataEntry = Array.isArray(tspTourSeries)
        ? tspTourSeries[tspTourSeries.length - 1]
        : tspTourSeries;

      const tourArray = tourDataEntry?.tour || tourDataEntry;
      const tourLength = tourDataEntry?.length;

      return {
        tour: tourArray,
        cities: Array.isArray(citiesData)
          ? citiesData.map((city, index) => ({
              id: index,
              x: city.x,
              y: city.y
            }))
          : [],
        observedTourLength: tourLength,
        originalTourLength: run.series?.fitness?.[run.series.fitness.length - 1]
          ? Math.abs(run.series.fitness[run.series.fitness.length - 1])
          : null
      };
    } else if (tspData) {
      return {
        tour: tspData.tour,
        cities: tspData.cities,
        originalTourLength: tspData.tourLength
      };
    }

    return null;
  }, [tspData, run]);

  useEffect(() => {
    if (sourceData?.cities) {
      setCities([...sourceData.cities]);
    }
  }, [sourceData]);

  useEffect(() => {
    if (!containerRef.current || (width && height)) return;

    const updateDimensions = () => {
      if (containerRef.current) {
        const { width: w, height: h } = containerRef.current.getBoundingClientRect();
        setDimensions({
          width: Math.max(300, w),
          height: Math.max(200, h)
        });
      }
    };

    updateDimensions();

    const resizeObserver = new ResizeObserver(updateDimensions);
    resizeObserver.observe(containerRef.current);

    return () => resizeObserver.disconnect();
  }, [width, height]);

  const { minX, minY, scale, offsetX, offsetY } = useMemo(() => {
    if (!cities || cities.length === 0) {
      return {
        minX: 0,
        maxX: 100,
        minY: 0,
        maxY: 100,
        scale: 1,
        offsetX: 0,
        offsetY: 0
      };
    }

    const padding = 30;
    const { width: w, height: h } = dimensions;

    const minX = Math.min(...cities.map((c) => c.x));
    const maxX = Math.max(...cities.map((c) => c.x));
    const minY = Math.min(...cities.map((c) => c.y));
    const maxY = Math.max(...cities.map((c) => c.y));

    const dataWidth = maxX - minX || 1;
    const dataHeight = maxY - minY || 1;

    const scaleX = (w - 2 * padding) / dataWidth;
    const scaleY = (h - 2 * padding) / dataHeight;
    const scale = Math.min(scaleX, scaleY);

    const offsetX = padding + (w - 2 * padding - dataWidth * scale) / 2;
    const offsetY = padding + (h - 2 * padding - dataHeight * scale) / 2;

    return { minX, maxX, minY, maxY, scale, offsetX, offsetY };
  }, [cities, dimensions]);

  const toSVGCoords = useCallback(
    (x, y) => {
      return {
        x: (x - minX) * scale + offsetX,
        y: dimensions.height - ((y - minY) * scale + offsetY)
      };
    },
    [minX, minY, scale, offsetX, offsetY, dimensions.height]
  );

  const fromSVGCoords = useCallback(
    (svgX, svgY) => {
      return {
        x: (svgX - offsetX) / scale + minX,
        y: (dimensions.height - svgY - offsetY) / scale + minY
      };
    },
    [minX, minY, scale, offsetX, offsetY, dimensions.height]
  );

  const getSVGPoint = useCallback((clientX, clientY) => {
    if (!svgRef.current) return { x: 0, y: 0 };

    const rect = svgRef.current.getBoundingClientRect();
    return {
      x: clientX - rect.left,
      y: clientY - rect.top
    };
  }, []);

  const handleMouseDown = useCallback(
    (e, cityIndex) => {
      if (!editable) return;
      e.preventDefault();
      setDraggedCity(cityIndex);
    },
    [editable]
  );

  const handleMouseMove = useCallback(
    (e) => {
      if (draggedCity === null) return;

      const svgPoint = getSVGPoint(e.clientX, e.clientY);
      const dataCoords = fromSVGCoords(svgPoint.x, svgPoint.y);

      setDragPosition(dataCoords);

      setCities((prevCities) =>
        prevCities.map((city, index) =>
          index === draggedCity
            ? { ...city, x: dataCoords.x, y: dataCoords.y }
            : city
        )
      );
    },
    [draggedCity, getSVGPoint, fromSVGCoords]
  );

  const handleMouseUp = useCallback(() => {
    if (draggedCity !== null && editable && onCitiesChange) {
      onCitiesChange(cities);
    }
    setDraggedCity(null);
    setDragPosition(null);
  }, [draggedCity, editable, onCitiesChange, cities]);

  const tourPath = useMemo(() => {
    if (!sourceData?.tour || !cities || cities.length === 0) return "";

    const pathPoints = sourceData.tour
      .map((cityIndex) => {
        const city = cities[cityIndex];
        if (!city) return null;
        const coords = toSVGCoords(city.x, city.y);
        return `${coords.x},${coords.y}`;
      })
      .filter((p) => p !== null);

    if (pathPoints.length > 0) {
      const firstCity = cities[sourceData.tour[0]];
      if (firstCity) {
        const firstCoords = toSVGCoords(firstCity.x, firstCity.y);
        pathPoints.push(`${firstCoords.x},${firstCoords.y}`);
      }
    }

    return pathPoints.join(" ");
  }, [sourceData?.tour, cities, toSVGCoords]);

  const currentTourLength = useMemo(() => {
    if (!sourceData?.tour || !cities || cities.length === 0) return 0;

    let totalDistance = 0;
    for (let i = 0; i < sourceData.tour.length; i++) {
      const currentCity = cities[sourceData.tour[i]];
      const nextCity = cities[sourceData.tour[(i + 1) % sourceData.tour.length]];

      if (currentCity && nextCity) {
        const dx = currentCity.x - nextCity.x;
        const dy = currentCity.y - nextCity.y;
        totalDistance += Math.sqrt(dx * dx + dy * dy);
      }
    }
    return totalDistance;
  }, [sourceData?.tour, cities]);

  const pheromoneEdges = useMemo(() => {
    if (!run?.series?.pheromoneHeatmap || !cities?.length) {
      return [];
    }

    const heatmaps = run.series.pheromoneHeatmap;
    const tours = run.series.tspTour || [];

    if (!heatmaps.length) return [];

    const heatmapIndex = Math.min(heatmaps.length - 1, Math.max(0, tours.length - 1));
    const matrix = heatmaps[heatmapIndex];

    if (!Array.isArray(matrix) || !Array.isArray(matrix[0])) {
      return [];
    }

    const n = Math.min(matrix.length, cities.length);
    if (n === 0) return [];

    let maxVal = 0;
    const allEdges = [];

    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        const rawV = Math.max(
          Number(matrix[i]?.[j] ?? 0),
          Number(matrix[j]?.[i] ?? 0)
        );

        allEdges.push({ a: i, b: j, v: rawV });
        if (rawV > maxVal) maxVal = rawV;
      }
    }

    if (maxVal <= 0) return [];

    return allEdges
      .map(({ a, b, v }) => {
        const c1 = cities[a];
        const c2 = cities[b];
        if (!c1 || !c2) return null;

        const p1 = toSVGCoords(c1.x, c1.y);
        const p2 = toSVGCoords(c2.x, c2.y);

        const visualFloor = maxVal * 0.02;
        const visualValue = Math.max(v, visualFloor);
        const normalized = visualValue / maxVal;
        const boosted = Math.pow(normalized, 0.55);

        return {
          x1: p1.x,
          y1: p1.y,
          x2: p2.x,
          y2: p2.y,
          intensity: Math.max(0.06, Math.min(1, boosted)),
          value: v,
          isWeak: v < maxVal * 0.05
        };
      })
      .filter(Boolean);
  }, [run?.series?.pheromoneHeatmap, run?.series?.tspTour, cities, toSVGCoords]);

  return (
    <div ref={containerRef} className="tsp-visualization">
      {(sourceData?.observedTourLength !== undefined || sourceData?.originalTourLength != null) && (
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
        className="tsp-svg"
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        <defs>
          <filter id="pheromone-blur-wide" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="10" />
          </filter>
        </defs>

        {pheromoneEdges.map((e, idx) => (
          <line
            key={`ph-${idx}`}
            x1={e.x1}
            y1={e.y1}
            x2={e.x2}
            y2={e.y2}
            stroke="#ff6a2a"
            strokeOpacity={0.08 + 0.5 * e.intensity}
            strokeWidth={0.5 + 3 * e.intensity}
            strokeLinecap="round"
          />
        ))}

        {tourPath && <polyline points={tourPath} className="tour-path" />}

        {cities.map((city, index) => {
          const isDragging = draggedCity === index;
          const displayCity = isDragging && dragPosition ? dragPosition : city;
          const coords = toSVGCoords(displayCity.x, displayCity.y);

          return (
            <g
              key={city.id ?? index}
              className={`city ${isDragging ? "dragging" : ""} ${editable ? "editable" : "readonly"}`}
            >
              <circle
                className="city-dot"
                cx={coords.x}
                cy={coords.y}
                onMouseDown={(e) => handleMouseDown(e, index)}
              />
              <text className="city-label" x={coords.x} y={coords.y - 12}>
                {index}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}