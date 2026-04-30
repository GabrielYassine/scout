import { bitstringEaTemplates } from "./bitstringEaTemplates.js";
import { tspEaTemplates } from "./tspEaTemplates.js";
import { vrpEaTemplates } from "./vrpEaTemplates.js";

export const eaTemplates = [
  ...bitstringEaTemplates,
  ...tspEaTemplates,
  ...vrpEaTemplates,
];