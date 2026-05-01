/*
 * Selector displays selectable puzzle pieces in tabs,
 * keeps track of the active tab,
 * and renders each item with the correct enabled/disabled state.
 */
import "@/features/lab/styles/Selector.css";
import PuzzlePiece from "../PuzzlePiece";
import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";
import { COMPONENT_TYPES, TAB_COLUMNS, getDisableReason } from "./selectorRules.js";
export default function Selector({
  catalog,
  onPieceHover,
  onPieceLeave,
  puzzleConfig,
  params,
}) {
  const [activeTab, setActiveTab] = useSessionStorageState(
    "scout:activeSelector",
    "searchSpace"
  );

  const runMode = params?.global?.experimentType ?? "run";
  const isRuntimeStudy = runMode === "runtimeStudy";

  const activeType = COMPONENT_TYPES.find((type) => type.key === activeTab) ?? COMPONENT_TYPES[0];

  const currentActiveTab = activeType.key;
  const items = catalog?.[activeType.catalogKey] ?? [];

  // Add placeholder cells so the last tab row stays aligned.
  const fillerCount = (TAB_COLUMNS - (COMPONENT_TYPES.length % TAB_COLUMNS)) % TAB_COLUMNS;

  // Count how many pieces of a given type are currently selected.
  const getCount = (key) => {
    const group = puzzleConfig?.[key];
    return Array.isArray(group) ? group.length : 0;
  };

  return (
    <div className="selector-container">
      <div className="tab-buttons-row">
        {COMPONENT_TYPES.map(({ key, label }) => {
          const count = getCount(key);

          return (
            <button
              key={key}
              className={`tab-button ${currentActiveTab === key ? "active" : ""}`}
              onClick={() => setActiveTab(key)}
            >
              <span className={`count-badge count-badge-${key}`}>{count}</span>
              {label}
            </button>
          );
        })}

        {Array.from({ length: fillerCount }).map((_, index) => (
          <div
            key={`tab-filler-${index}`}
            className="tab-button tab-button--placeholder"
            aria-hidden="true"
          />
        ))}
      </div>

      <div className="option-list-outer">
        <div className="option-list">
          {items.map((item) => {
            const disabledReason = getDisableReason({ item, currentActiveTab, activeType, puzzleConfig, isRuntimeStudy, });
            return (
              <PuzzlePiece
                key={item.id}
                id={item.id}
                label={item.displayName}
                type={currentActiveTab}
                onHover={onPieceHover}
                onLeave={onPieceLeave}
                isDisabled={disabledReason !== null}
                disabledReason={disabledReason}
              />
            );
          })}
        </div>
      </div>
    </div>
  );
}