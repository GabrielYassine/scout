import Section from "./Section.jsx";

/**
 * Generic collapsible section used by both sidebars.
 *
 * If `collapsible` is false, it renders a static header + body without needing local state.
 */
export default function SidebarSection({
  title,
  children,
  collapsible = true,
  isOpen = true,
  onToggle,
}) {
  if (!collapsible) {
    return (
      <div className="ll-section">
        <div className="ll-section-header" aria-expanded={true}>
          <span className="ll-section-title">{title}</span>
        </div>
        <div className="ll-section-body">{children}</div>
      </div>
    );
  }

  return <Section title={title} isOpen={isOpen} onToggle={onToggle} children={children} />;
}
