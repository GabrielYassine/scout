/**
 * Collapsible sidebar section wrapper component.
 * @author s235257
 */

import "@/shared/components/styles/SidebarSections.css";

export default function SidebarSection({
  title,
  children,
  collapsible = true,
  isOpen = true,
  onToggle,
}) {
  if (!collapsible) {
    return (
      <div className="sidebar-section">
        <div className="sidebar-section-header" aria-expanded={true}>
          <span className="sidebar-section-title">{title}</span>
          <span className="sidebar-section-triangle open">▸</span>
        </div>
        <div className="sidebar-section-body">{children}</div>
      </div>
    );
  }

  return (
    <div className="sidebar-section">
      <button
        type="button"
        className="sidebar-section-header"
        onClick={onToggle}
        aria-expanded={isOpen}
      >
        <span className="sidebar-section-title">{title}</span>
        <span
          className={
            isOpen ? "sidebar-section-triangle open" : "sidebar-section-triangle"
          }
        >
          ▸
        </span>
      </button>

      {isOpen && <div className="sidebar-section-body">{children}</div>}
    </div>
  );
}