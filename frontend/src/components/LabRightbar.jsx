import "./LabRightbar.css";

export default function LabRightbar({ hoverInfo }) {
  return (
    <section className="lab-rightbar">
      <div className="lr-content">
        <div className="lr-title">Info</div>

        <div className="lr-section">
          <div className="lr-section-header">
            <div className="lr-section-title">Description</div>
          </div>

          <div className="lr-section-body">
            {hoverInfo ? (
              <>
                <div className="lr-selected-piece">{hoverInfo.title}</div>
                <div className="lr-text">{hoverInfo.description}</div>
              </>
            ) : (
              <div className="lr-placeholder">
                Hover over a puzzle piece to see its description.
              </div>
            )}
          </div>
        </div>

        <div className="lr-section">
          <div className="lr-section-header">
            <div className="lr-section-title">Details</div>
          </div>

          <div className="lr-section-body">
             <div className="lr-placeholder"></div>
          </div>
        </div>
      </div>
    </section>
  );
}
