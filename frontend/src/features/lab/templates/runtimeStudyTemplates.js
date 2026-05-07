/**
 * This file is responsible for exporting all the runtime study templates that are used in the application.
 *  It imports the templates from their respective directories and combines them into a single array called `runtimeStudyTemplates`.
  * This array can then be used throughout the application to access the various runtime study templates.
  * @author s230632
 */

import { saRuntimeTemplates } from "./runtimeStudyTemplates/saRuntimeTemplates/index.js";
import { eaRuntimeTemplates } from "./runtimeStudyTemplates/eaRuntimeTemplates/index.js";
import { acoRuntimeTemplates } from "./runtimeStudyTemplates/acoRuntimeTemplates/index.js";

export const runtimeStudyTemplates = [
  ...eaRuntimeTemplates,
  ...saRuntimeTemplates,
  ...acoRuntimeTemplates,
];
