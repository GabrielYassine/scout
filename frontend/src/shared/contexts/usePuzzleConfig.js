import { useContext } from "react";
import { PuzzleConfigContext } from "./PuzzleConfigContext.jsx";

export const usePuzzleConfig = () => {
  const context = useContext(PuzzleConfigContext);
  if (!context) {
    throw new Error("usePuzzleConfig must be used within PuzzleConfigProvider");
  }
  return context;
};