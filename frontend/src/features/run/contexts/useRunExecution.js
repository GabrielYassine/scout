/**
  *
  * @author s235257
 */
import { useContext } from "react";

import { RunExecutionContext } from "./RunExecutionContext.jsx";

export function useRunExecution() {
  const context = useContext(RunExecutionContext);

  if (!context) {
    throw new Error("useRunExecution must be used within RunExecutionProvider");
  }

  return context;
}