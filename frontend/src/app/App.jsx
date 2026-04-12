import { Routes, Route, Navigate } from "react-router-dom";
import TopNavbar from "@/shared/components/navigation/TopNavbar.jsx";
import { PuzzleConfigProvider } from "@/shared/contexts/PuzzleConfigContext.jsx";

import HomePage from "@/features/home/HomePage.jsx";
import LabPage from "@/features/lab/LabPage.jsx";
import RunPage from "@/features/run/RunPage.jsx";

import { useCatalog } from "@/shared/hooks/useCatalog.js";
import { useTemplates } from "@/shared/hooks/useTemplates.js";

export default function App() {
  const { catalog, catalogLoading, catalogError } = useCatalog();
  const { templates, templatesLoading, templatesError } = useTemplates();

  return (
    <PuzzleConfigProvider>
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
                templates={templates}
                templatesLoading={templatesLoading}
                templatesError={templatesError}
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
    </PuzzleConfigProvider>
  );
}