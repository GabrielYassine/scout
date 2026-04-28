/*
 * puzzleMasks stores the mask image for each puzzle piece shape
 * and returns the CSS mask styles needed to render that shape in the UI.
 */
import _0000 from "@/assets/puzzlePNG/0000.png";

import _1000 from "@/assets/puzzlePNG/3flat/1000.png";
import _2000 from "@/assets/puzzlePNG/3flat/2000.png";

import _1010 from "@/assets/puzzlePNG/2flat/1010.png";
import _1020 from "@/assets/puzzlePNG/2flat/1020.png";
import _1100 from "@/assets/puzzlePNG/2flat/1100.png";
import _1200 from "@/assets/puzzlePNG/2flat/1200.png";
import _2020 from "@/assets/puzzlePNG/2flat/2020.png";
import _2200 from "@/assets/puzzlePNG/2flat/2200.png";

import _1110 from "@/assets/puzzlePNG/1flat/1110.png";
import _1120 from "@/assets/puzzlePNG/1flat/1120.png";
import _1210 from "@/assets/puzzlePNG/1flat/1210.png";
import _2120 from "@/assets/puzzlePNG/1flat/2120.png";
import _2210 from "@/assets/puzzlePNG/1flat/2210.png";
import _2220 from "@/assets/puzzlePNG/1flat/2220.png";

import _1111 from "@/assets/puzzlePNG/0flat/1111.png";
import _1112 from "@/assets/puzzlePNG/0flat/1112.png";
import _1122 from "@/assets/puzzlePNG/0flat/1122.png";
import _1212 from "@/assets/puzzlePNG/0flat/1212.png";
import _1222 from "@/assets/puzzlePNG/0flat/1222.png";
import _2222 from "@/assets/puzzlePNG/0flat/2222.png";

export const PUZZLE_MASKS = {
  "0000": _0000,

  "1000": _1000,
  "2000": _2000,

  "1010": _1010,
  "1020": _1020,
  "1100": _1100,
  "1200": _1200,
  "2020": _2020,
  "2200": _2200,

  "1110": _1110,
  "1120": _1120,
  "1210": _1210,
  "2120": _2120,
  "2210": _2210,
  "2220": _2220,

  "1111": _1111,
  "1112": _1112,
  "1122": _1122,
  "1212": _1212,
  "1222": _1222,
  "2222": _2222,
};

function buildMaskStyle(url) {
  return {
    WebkitMaskImage: `url(${url})`,
    maskImage: `url(${url})`,
    WebkitMaskRepeat: "no-repeat",
    maskRepeat: "no-repeat",
    WebkitMaskSize: "100% 100%",
    maskSize: "100% 100%",
    WebkitMaskPosition: "center",
    maskPosition: "center",
  };
}

export function maskStyle(key) {
  const url = PUZZLE_MASKS[key] ?? PUZZLE_MASKS["0000"];
  return buildMaskStyle(url);
}