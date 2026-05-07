/**
  *
  * @author s235257
 */

export default function InstanceUploadSection({
  fileInputRef,
  uploading,
  uploadError,
  exporting,
  exportError,
  onFileUpload,
  onExport,
}) {
  return (
    <div className="tsp-upload-section">
      <input
        ref={fileInputRef}
        type="file"
        accept=".tsp,.vrp,.txt"
        onChange={onFileUpload}
        disabled={uploading}
        className="tsp-file-input"
        id="instance-file-upload"
      />

      <label
        htmlFor="instance-file-upload"
        className={`tsp-upload-btn ${uploading ? "disabled" : ""}`}
      >
        {uploading ? "Uploading..." : "Upload Instance File"}
      </label>

      <button
        className={`tsp-upload-btn tsp-export-btn ${exporting ? "disabled" : ""}`}
        type="button"
        onClick={onExport}
        disabled={exporting}
      >
        {exporting ? "Exporting..." : "Export Instance File"}
      </button>

      <div className="tsp-file-hint">Accepts .tsp and .vrp file formats</div>

      {uploadError && <div className="tsp-upload-error">{uploadError}</div>}
      {exportError && <div className="tsp-upload-error">{exportError}</div>}
    </div>
  );
}