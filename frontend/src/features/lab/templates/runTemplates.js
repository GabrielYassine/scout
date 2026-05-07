/**
  *
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