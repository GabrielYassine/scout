import { saRuntimeTemplates } from "./runtimeStudyTemplates/saRuntimeTemplates/index.js";
import { eaRuntimeTemplates } from "./runtimeStudyTemplates/eaRuntimeTemplates/index.js";
import { acoRuntimeTemplates } from "./runtimeStudyTemplates/acoRuntimeTemplates/index.js";

export const runtimeStudyTemplates = [
  ...eaRuntimeTemplates,
  ...saRuntimeTemplates,
  ...acoRuntimeTemplates,
];
