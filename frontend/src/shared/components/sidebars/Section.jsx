// Section no longer imports sidebar-specific CSS so it can be shared.

// A collapsible section component for the lab left sidebar
// @author s235257
export default function Section({ title, isOpen, onToggle, children }) {
  return (
    <div className="ll-section">
      <button
        type="button"
        className="ll-section-header"
        onClick={onToggle}
        aria-expanded={isOpen}
      >
        <span className="ll-section-title">{title}</span>
        <span className={isOpen ? "ll-triangle open" : "ll-triangle"}>▸</span>
      </button>

      {isOpen && <div className="ll-section-body">{children}</div>}
    </div>
  );
}