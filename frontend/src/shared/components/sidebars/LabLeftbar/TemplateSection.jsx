/*
 * TemplateSection renders the template selector and apply button.
 */
import SidebarSection from "./SidebarSection.jsx";

export default function TemplateSection({
  open,
  setOpen,
  selectedTemplateId,
  setSelectedTemplateId,
  disabled,
  templates,
  templatesLoading,
  templatesError,
  onApplyTemplate,
}) {
  return (
    <SidebarSection
      title="Templates"
      isOpen={open.templates ?? true}
      onToggle={() =>
        setOpen((current) => ({
          ...current,
          templates: !(current.templates ?? true),
        }))
      }
    >
      <div className="ll-subsection">
        <select
          className="field-input"
          value={selectedTemplateId}
          onChange={(e) => setSelectedTemplateId(e.target.value)}
          disabled={disabled || templatesLoading || templates.length === 0}
        >
          <option value="">Select template</option>
          {templates.map((template) => (
            <option key={template.id} value={template.id}>
              {template.displayName}
            </option>
          ))}
        </select>

        <button
          className="btn btn--green"
          type="button"
          disabled={disabled || !selectedTemplateId}
          onClick={() => {
            onApplyTemplate?.(selectedTemplateId);
            setSelectedTemplateId("");
          }}
        >
          Apply Template
        </button>

        {templatesError && (
          <div className="ll-subsection">
            Failed to load templates: {templatesError}
          </div>
        )}
      </div>
    </SidebarSection>
  );
}