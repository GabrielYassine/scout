import { useEffect, useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import TopNavbar from "./components/TopNavbar.jsx";

import HomePage from "./pages/HomePage.jsx";
import LabPage from "./pages/labPage/LabPage.jsx";
import HistoryPage from "./pages/HistoryPage.jsx";
import RunPage from "./pages/RunPage.jsx";

export default function App() {
  const [catalog, setCatalog] = useState(null);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setCatalogLoading(true);
        setCatalogError(null);

        const res = await fetch("/api/catalog");
        if (!res.ok) throw new Error(`Catalog fetch failed: ${res.status}`);

        const data = await res.json();
        if (!cancelled) setCatalog(data);
      } catch (e) {
        if (!cancelled) setCatalogError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setCatalogLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="app-root">
      <TopNavbar />

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route
          path="/lab"
          element={
            <LabPage
              catalog={catalog}
              catalogLoading={catalogLoading}
              catalogError={catalogError}
            />
          }
        />
        <Route
          path="/history"
          element={
            <HistoryPage
              catalog={catalog}
              catalogLoading={catalogLoading}
              catalogError={catalogError}
            />
          }
        />
        <Route
          path="/run"
          element={
            <RunPage
              catalog={catalog}
              catalogLoading={catalogLoading}
              catalogError={catalogError}
            />
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}