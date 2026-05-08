/**
 * Helpers for route visualization data shaping.
 * @author s235257
 */

const toInt = (value) => Math.round(Number(value) || 0);

export function sanitizeCity(city) {
  return {
    ...city,
    x: toInt(city.x),
    y: toInt(city.y),
  };
}

// Normalizes raw city data into a consistent format for route visualization.
export function normalizeCities(citiesData) {
  if (!Array.isArray(citiesData)) {
    return [];
  }

  return citiesData.map((city) =>
    sanitizeCity({
      id: city.id,
      nodeId: city.nodeId,
      x: city.x,
      y: city.y,
      demand: city.demand ?? 0,
      isDepot: city.isDepot === true,
    })
  );
}

export function getLatestSeriesValue(seriesValue) {
  if (!Array.isArray(seriesValue)) {
    return seriesValue;
  }

  if (!seriesValue.length) {
    return null;
  }

  return seriesValue[seriesValue.length - 1];
}

export function normalizeRoutes(tour) {
  if (!Array.isArray(tour) || tour.length === 0) {
    return [];
  }

  return Array.isArray(tour[0]) ? tour : [tour];
}

export function extractRunSourceData(run) {
  if (!run?.series?.tspTour || !run?.series?.tspCities) {
    return null;
  }

  const tspCitiesSeries = run.series.tspCities;

  const citiesData = Array.isArray(tspCitiesSeries[0]) || tspCitiesSeries.length === 0 ? getLatestSeriesValue(tspCitiesSeries) : tspCitiesSeries;

  const tourDataEntry = getLatestSeriesValue(run.series.tspTour);
  const tourArray = tourDataEntry?.tour || tourDataEntry;
  const tourLength = tourDataEntry?.length;

  return {
    tour: tourArray,
    cities: normalizeCities(citiesData),
    observedTourLength: tourLength,
    originalTourLength:
      run.series.fitness?.[run.series.fitness.length - 1] != null ? Math.abs(run.series.fitness[run.series.fitness.length - 1]) : null,
  };
}

export function extractTspSourceData(tspData) {
  if (!tspData) {
    return null;
  }

  return {
    tour: tspData.tour,
    cities: normalizeCities(tspData.cities),
    originalTourLength: tspData.tourLength,
  };
}

// Builds a unique key for a set of cities based on their id, coordinates, and depot status.
// Useful for memoization and avoiding unnecessary re-renders when city data hasn't changed.
export function buildCitiesKey(cities) {
  if (!cities.length) {
    return "";
  }

  return cities
    .map((city) => `${city.id}:${city.x},${city.y}:${city.isDepot ? 1 : 0}`)
    .join("|");
}

export function buildRouteSequence(route, depotIndex) {
  if (!Array.isArray(route) || route.length === 0) {
    return [];
  }

  const sequence = route.map(Number).filter(Number.isFinite);

  if (!sequence.length) {
    return [];
  }

  if (depotIndex >= 0) {
    if (sequence[0] !== depotIndex) sequence.unshift(depotIndex);
    if (sequence[sequence.length - 1] !== depotIndex) sequence.push(depotIndex);
  } else {
    sequence.push(sequence[0]);
  }

  return sequence;
}

export function calculateRouteDistance(sequence, cities) {
  let totalDistance = 0;

  for (let i = 0; i < sequence.length - 1; i += 1) {
    const currentCity = cities[sequence[i]];
    const nextCity = cities[sequence[i + 1]];

    if (!currentCity || !nextCity) {
      continue;
    }

    const dx = currentCity.x - nextCity.x;
    const dy = currentCity.y - nextCity.y;

    totalDistance += Math.sqrt(dx * dx + dy * dy);
  }

  return totalDistance;
}