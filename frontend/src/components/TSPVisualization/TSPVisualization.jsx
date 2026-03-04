import { useState, useRef, useCallback, useMemo, useEffect } from "react";
import "./TSPVisualization.css";

export default function TSPVisualization({ tspData, run, width, height }) {
  const containerRef = useRef(null);
  const svgRef = useRef(null);
  const [dimensions, setDimensions] = useState({ width: width || 800, height: height || 600 });
  const [draggedCity, setDraggedCity] = useState(null);

  const sourceData = useMemo(() => {
    if (run?.series?.tspTour && run?.series?.tspCities) {
      const citiesData = run.series.tspCities[0];
      const tourData = run.series.tspTour[run.series.tspTour.length - 1];

      return {
        tour: tourData,
        cities: citiesData.map((city, index) => ({
          id: index,
          x: city.x,
          y: city.y
        })),
        originalTourLength: run.series?.fitness?.[run.series.fitness.length - 1] ?
          Math.abs(run.series.fitness[run.series.fitness.length - 1]) : null
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

  const [cities, setCities] = useState([]);

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

  const { minX, maxX, minY, maxY, scale, offsetX, offsetY } = useMemo(() => {
    if (!cities || cities.length === 0) {
      return { minX: 0, maxX: 100, minY: 0, maxY: 100, scale: 1, offsetX: 0, offsetY: 0 };
    }

    const padding = 30;
    const { width: w, height: h } = dimensions;
    const minX = Math.min(...cities.map(c => c.x));
    const maxX = Math.max(...cities.map(c => c.x));
    const minY = Math.min(...cities.map(c => c.y));
    const maxY = Math.max(...cities.map(c => c.y));

    const dataWidth = maxX - minX || 1;
    const dataHeight = maxY - minY || 1;

    const scaleX = (w - 2 * padding) / dataWidth;
    const scaleY = (h - 2 * padding) / dataHeight;
    const scale = Math.min(scaleX, scaleY);

    const offsetX = padding + (w - 2 * padding - dataWidth * scale) / 2;
    const offsetY = padding + (h - 2 * padding - dataHeight * scale) / 2;

    return { minX, maxX, minY, maxY, scale, offsetX, offsetY };
  }, [cities, dimensions]);

  const toSVGCoords = useCallback((x, y) => {
    return {
      x: (x - minX) * scale + offsetX,
      y: dimensions.height - ((y - minY) * scale + offsetY) // Flip Y axis
    };
  }, [minX, minY, scale, offsetX, offsetY, dimensions.height]);

  const fromSVGCoords = useCallback((svgX, svgY) => {
    return {
      x: (svgX - offsetX) / scale + minX,
      y: (dimensions.height - svgY - offsetY) / scale + minY
    };
  }, [minX, minY, scale, offsetX, offsetY, dimensions.height]);

  const getSVGPoint = useCallback((clientX, clientY) => {
    if (!svgRef.current) return { x: 0, y: 0 };
    const rect = svgRef.current.getBoundingClientRect();
    return {
      x: clientX - rect.left,
      y: clientY - rect.top
    };
  }, []);

  const handleMouseDown = useCallback((e, cityIndex) => {
    e.preventDefault();
    setDraggedCity(cityIndex);
  }, []);

  const handleMouseMove = useCallback((e) => {
    if (draggedCity === null) return;

    const svgPoint = getSVGPoint(e.clientX, e.clientY);
    const dataCoords = fromSVGCoords(svgPoint.x, svgPoint.y);

    setCities(prevCities =>
      prevCities.map((city, index) =>
        index === draggedCity
          ? { ...city, x: dataCoords.x, y: dataCoords.y }
          : city
      )
    );
  }, [draggedCity, getSVGPoint, fromSVGCoords]);

  const handleMouseUp = useCallback(() => {
    setDraggedCity(null);
  }, []);

  const tourPath = useMemo(() => {
    if (!sourceData?.tour || !cities || cities.length === 0) return "";

    const pathPoints = sourceData.tour.map(cityIndex => {
      const city = cities[cityIndex];
      if (!city) return null;
      const coords = toSVGCoords(city.x, city.y);
      return `${coords.x},${coords.y}`;
    }).filter(p => p !== null);

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

  return (
    <div ref={containerRef} className="tsp-visualization">
      <svg
        ref={svgRef}
        width={dimensions.width}
        height={dimensions.height}
        className="tsp-svg"
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        {tourPath && (
          <polyline
            points={tourPath}
            fill="none"
            stroke="#3b82f6"
            strokeWidth="2"
            strokeLinejoin="round"
            className="tour-path"
          />
        )}
        {cities.map((city, index) => {
          const coords = toSVGCoords(city.x, city.y);
          const isDragging = draggedCity === index;

          return (
            <g key={index} className={`city ${isDragging ? "dragging" : ""}`}>
              <circle
                cx={coords.x}
                cy={coords.y}
                r={isDragging ? 8 : 6}
                fill={isDragging ? "#ef4444" : "#10b981"}
                stroke="#fff"
                strokeWidth="2"
                onMouseDown={(e) => handleMouseDown(e, index)}
                style={{ cursor: "grab" }}
              />
              <text
                x={coords.x}
                y={coords.y - 12}
                textAnchor="middle"
                fontSize="11"
                fill="#333"
                pointerEvents="none"
              >
                {index}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}
