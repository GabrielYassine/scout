import flat_hole from "../assets/puzzlePNG/flat_hole.png";
import flat_tab from "../assets/puzzlePNG/flat_tab.png";
import hole_hole from "../assets/puzzlePNG/hole_hole.png";
import hole_tab from "../assets/puzzlePNG/hole_tab.png";
import tab_tab from "../assets/puzzlePNG/tab_tab.png";

export const PUZZLE_MASKS = {
    flat_hole,
    flat_tab,
    hole_hole,
    hole_tab,
    tab_tab,
};

export function maskStyle(key) {
    const url = PUZZLE_MASKS[key];
    if (!url) return {};

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
