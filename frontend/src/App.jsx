import { Routes, Route, Navigate } from "react-router-dom";
import TopNavbar from "./components/TopNavbar.jsx";

import HomePage from "./pages/HomePage.jsx";
import LabPage from "./pages/LabPage.jsx";
import HistoryPage from "./pages/HistoryPage.jsx";

export default function App() {
  return (
    <div className="app-root">
      <TopNavbar />

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/lab" element={<LabPage />} />
        <Route path="/history" element={<HistoryPage />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
