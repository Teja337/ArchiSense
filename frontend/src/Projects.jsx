
import { useEffect, useState } from "react";
import { getProjects, getVersions, uploadProject } from "./api";

function Projects({ onUploadComplete }) {
  const [projects, setProjects] = useState([]);
  const [projectVersions, setProjectVersions] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [uploadResult, setUploadResult] = useState(null);

  async function loadProjects() {
    const projectData = await getProjects();

    setProjects(projectData);

    const versionEntries = await Promise.all(
      projectData.map(async (project) => {
        try {
          const versions = await getVersions(project.id);
          return [project.id, versions];
        } catch (err) {
          console.error(
            `Unable to load versions for ${project.id}:`,
            err
          );

          return [project.id, []];
        }
      })
    );

    setProjectVersions(Object.fromEntries(versionEntries));
  }

  async function refreshProjectLists() {
    try {
      await loadProjects();
    } catch (err) {
      console.error("Unable to refresh project list:", err);
    }

    if (onUploadComplete) {
      try {
        await onUploadComplete();
      } catch (err) {
        console.error("Unable to refresh shared project list:", err);
      }
    }
  }

  useEffect(() => {
    async function initialize() {
      try {
        await loadProjects();
      } catch (err) {
        setError(err.message || "Unable to load projects.");
      } finally {
        setLoading(false);
      }
    }

    initialize();
  }, []);

  function handleFileChange(event) {
    const file = event.target.files?.[0];

    setSelectedFile(file || null);
    setUploadError("");
    setUploadResult(null);

    if (file && !file.name.toLowerCase().endsWith(".zip")) {
      setSelectedFile(null);
      setUploadError("Please select a ZIP file.");
      event.target.value = "";
    }
  }

  async function handleUpload(event) {
    event.preventDefault();

    if (!selectedFile) {
      setUploadError("Please select a Java project ZIP file first.");
      return;
    }

    if (uploading) return;

    setUploading(true);
    setUploadError("");
    setUploadResult(null);

    try {
      const result = await uploadProject(selectedFile);

      setUploadResult(result);
      setSelectedFile(null);
      event.target.reset();

    } catch (err) {
      const failure = err.responseData || {
        status: "FAILED",
        stage: "UNKNOWN",
        message: err.message || "Project upload failed.",
      };

      setUploadResult(failure);
      setUploadError("");

    } finally {
      await refreshProjectLists();
      setUploading(false);
    }
  }

  return (
    <section className="projects-page">
      <div className="welcome-row">
        <div>
          <h2>Projects</h2>
          <p>
            Manage and explore available software projects.
          </p>
        </div>
      </div>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Upload Java Project</h3>
            <p>
              Upload a Maven-based Java project as a ZIP file.
              ArchiSense will build the project and run the analysis.
            </p>
          </div>
        </div>

        <form onSubmit={handleUpload}>
          <div>
            <label htmlFor="project-zip">
              Select project ZIP file
            </label>

            <input
              id="project-zip"
              type="file"
              accept=".zip,application/zip"
              onChange={handleFileChange}
              disabled={uploading}
            />
          </div>

          {selectedFile && (
            <p>Selected file: {selectedFile.name}</p>
          )}

          <button
            type="submit"
            disabled={uploading || !selectedFile}
          >
            {uploading
              ? "Uploading and analysing..."
              : "Upload and Analyse"}
          </button>
        </form>

        {uploadError && (
          <p role="alert" className="error-message">
            {uploadError}
          </p>
        )}

        {uploading && (
          <div className="status-note" role="status">
            <strong>Project processing in progress</strong>
            <p>
              ArchiSense is uploading, building and analysing
              your Java project. Please wait until processing finishes.
            </p>
          </div>
        )}

        {uploadResult?.status === "FAILED" && (
          <div className="upload-error" role="alert">
            <h3>Project processing failed</h3>

            <p>
              <strong>Status:</strong> FAILED
            </p>

            <p>
              <strong>Failure stage:</strong>{" "}
              {uploadResult.stage || "UNKNOWN"}
            </p>

            {uploadResult.projectId && (
              <p>
                <strong>Project ID:</strong>{" "}
                {uploadResult.projectId}
              </p>
            )}

            <p>
              <strong>Error:</strong>{" "}
              {uploadResult.message || "An unexpected error occurred."}
            </p>
          </div>
        )}

        {uploadResult?.status === "ANALYSIS_COMPLETED" && (
          <div className="upload-success">
            <h3>Analysis completed</h3>

            <p>
              <strong>Status:</strong> {uploadResult.status}
            </p>

            <p>
              <strong>Project ID:</strong> {uploadResult.projectId}
            </p>

            <p>
              <strong>Original file:</strong>{" "}
              {uploadResult.originalFilename}
            </p>

            {uploadResult.analysisOutput && (
              <p>
                <strong>Analysis output:</strong>{" "}
                {uploadResult.analysisOutput}
              </p>
            )}
          </div>
        )}
      </section>

      <section className="panel">
        <h3>Available Projects</h3>

        {loading ? (
          <p>Loading projects...</p>
        ) : error ? (
          <p role="alert">{error}</p>
        ) : projects.length === 0 ? (
          <p>No projects are currently available.</p>
        ) : (
          projects.map((project) => {
            const versions = projectVersions[project.id] || [];

            return (
              <section
                className="panel project-card"
                key={project.id}
              >
                <div className="panel-heading">
                  <div>
                    <h3>{project.name}</h3>
                    <p>{project.description}</p>
                  </div>

                  <span className="pending-tag">
                    {project.status}
                  </span>
                </div>

                <div className="project-information">
                  <div>
                    <span>Project ID</span>
                    <strong>{project.id}</strong>
                  </div>

                  <div>
                    <span>Available versions</span>
                    <strong>{versions.length}</strong>
                  </div>
                </div>

                {project.status === "FAILED" && (
  <div className="upload-error" role="alert">
    <h4>Project processing failed</h4>

    <p>
      <strong>Failure stage:</strong>{" "}
      {project.failureStage || "Unavailable"}
    </p>

    <p>
      <strong>Error:</strong>{" "}
      {project.failureMessage ||
        "Failure details unavailable for this older project."}
    </p>
  </div>
)}

                <h4>Software versions</h4>

                <div className="version-list">
                  {versions.length > 0 ? (
                    versions.map((version) => (
                      <div
                        className="version-item"
                        key={version}
                      >
                        <span>{version}</span>
                        <span className="pending-tag">
                          Available
                        </span>
                      </div>
                    ))
                  ) : (
                    <p>No versions available.</p>
                  )}
                </div>
              </section>
            );
          })
        )}
      </section>
    </section>
  );
}

export default Projects;
