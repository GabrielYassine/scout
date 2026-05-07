/**
  * This file imports all the run templates for the different algorithms and combines them into a single array called runTemplates.
  * This array can then be used to display the available run templates in the UI or to execute a specific template when selected by the user.
  * @author s230632
 */

import { eaTemplates } from "./runTemplates/eaTemplates/index.js";
import { saTemplates } from "./runTemplates/saTemplates/index.js";
import { acoTemplates } from "./runTemplates/acoTemplates/index.js";

export const runTemplates = [
  ...eaTemplates,
  ...saTemplates,
  ...acoTemplates,
];