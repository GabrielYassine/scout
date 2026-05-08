/**
 * Template selector for applying predefined run configurations in the lab sidebar.
 * @author s230632
 */

import SidebarSection from "../SidebarSection.jsx";

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
  const hasTemplates = templates.length > 0;
  const cannotApplyTemplate = disabled || !selectedTemplateId;

  function handleApplyTemplate() {
    onApplyTemplate?.(selectedTemplateId);
    setSelectedTemplateId("");
  }

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
          onChange={(event) => setSelectedTemplateId(event.target.value)}
          disabled={disabled || templatesLoading || !hasTemplates}
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
          disabled={cannotApplyTemplate}
          onClick={handleApplyTemplate}
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