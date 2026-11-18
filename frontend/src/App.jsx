
import { useEffect, useState } from "react";
import "./App.css";
import { getProjects, getVersions, getResults } from "./api";
import Evolution from "./Evolution";
import Analysis from "./Analysis";
import Reports from "./Reports";
import Projects from "./Projects";

const menu = [
  ["Overview", "▦"],
  ["Projects", "▤"],
  ["Analysis", "⌁"],
  ["Evolution", "↗"],
  ["Reports", "▧"],
];

function App() {
  const [active, setActive] = useState("Overview");
  const [projects, setProjects] = useState([]);
  const [project, setProject] = useState("ant");
  const [versions, setVersions] = useState([]);
  const [selectedVersions, setSelectedVersions] = useState([]);
  const [results, setResults] = useState([]);
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [backendStatus, setBackendStatus] = useState("Checking");
  const [analysisRunning, setAnalysisRunning] = useState(false);
  const [lastRunId, setLastRunId] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  // Refresh the shared project list after a successful upload.
  async function refreshProjects() {
    try {
      const data = await getProjects();

      setProjects(data);
      setBackendStatus("Connected");

      setProject((currentProject) => {
        if (data.some((item) => item.id === currentProject)) {
          return currentProject;
        }

        const uploadedProjects = data.filter(
          (item) => item.id !== "ant"
        );

        return uploadedProjects.length > 0
          ? uploadedProjects[uploadedProjects.length - 1].id
          : data[0]?.id || "ant";
      });
    } catch (error) {
      console.error("Unable to refresh projects:", error);
      setBackendStatus("Disconnected");
    }
  }

  // Load registered projects when the application starts.
  useEffect(() => {
    async function loadProjects() {
      try {
        const data = await getProjects();

        setProjects(data);
        setBackendStatus("Connected");

        if (data.length > 0) {
          const uploadedProjects = data.filter(
            (item) => item.id !== "ant"
          );

          setProject(
            uploadedProjects.length > 0
              ? uploadedProjects[uploadedProjects.length - 1].id
              : data[0].id
          );
        }
      } catch (error) {
        setBackendStatus("Disconnected");
        setNotice(
          error.message || "Unable to connect to backend."
        );
      } finally {
        setLoading(false);
      }
    }

    loadProjects();
  }, []);

  // Load versions and results independently for the selected project.
  useEffect(() => {
    let cancelled = false;

    async function loadProjectData() {
      if (!project) return;

      setResults([]);
      setVersions([]);
      setSelectedVersions([]);
      setNotice("");

      const [versionOutcome, resultOutcome] =
        await Promise.allSettled([
          getVersions(project),
          getResults(project),
        ]);

      if (cancelled) return;

      if (versionOutcome.status === "fulfilled") {
        const versionData = versionOutcome.value;

        setVersions(versionData);
        setSelectedVersions(versionData);
      } else {
        setVersions([]);
        setSelectedVersions([]);

        setNotice(
          "Could not load project versions: " +
            (versionOutcome.reason?.message || "Unknown error")
        );
      }

      if (resultOutcome.status === "fulfilled") {
        setResults(resultOutcome.value);
      } else {
        setResults([]);

        setNotice((current) =>
          current
            ? current + " | Existing analysis results are unavailable."
            : "Existing analysis results are unavailable. Run a fresh analysis."
        );
      }
    }

    loadProjectData();

    return () => {
      cancelled = true;
    };
  }, [project]);

  function toggleVersion(version) {
    setSelectedVersions((current) =>
      current.includes(version)
        ? current.filter((item) => item !== version)
        : [...current, version]
    );
  }

  const selectedVersion = selectedVersions[0];
  const selectedVersionIndex = versions.indexOf(selectedVersion);

  const selectedResult =
    project !== "ant"
      ? results[0] || null
      : results.find(
          (item) =>
            Number(item.versionIndex) === selectedVersionIndex
        ) || null;

  const selectedProject = projects.find(
    (item) => item.id === project
  );

  function formatNumber(value, decimals = 0) {
    if (value === undefined || value === null) return "—";

    const number = Number(value);

    if (!Number.isFinite(number)) return "—";

    return number.toLocaleString("en-US", {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    });
  }

  // Run fresh analysis for the selected project.
  async function startAnalysis() {
    if (!project) {
      setNotice("Please select a project first.");
      return;
    }

    if (!selectedVersions.length) {
      setNotice("Please select at least one software version.");
      return;
    }

    if (analysisRunning) return;

    try {
      setAnalysisRunning(true);
      setLastRunId("");
      setNotice("Fresh AsTdEA analysis is running. Please wait...");

      const response = await fetch(
        `http://localhost:8080/api/analysis/run?projectId=${encodeURIComponent(project)}`,
        {
          method: "POST",
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.message ||
            data.error ||
            "Analysis execution failed."
        );
      }

      const runIdentifier =
        data.runId || data.analysisOutput || "";

      setLastRunId(runIdentifier);

      const refreshedResults = await getResults(project);
      setResults(refreshedResults);

      setRefreshKey((current) => current + 1);

      setNotice("Analysis completed successfully!");
    } catch (error) {
      setNotice("Analysis failed: " + error.message);
    } finally {
      setAnalysisRunning(false);
    }
  }

  const metricCards = [
    ["Projects", projects.length, "Registered projects"],
    ["Versions", versions.length, "Available versions"],
    [
      "Architecture smells",
      selectedResult
        ? formatNumber(selectedResult.ASCount)
        : "—",
      "Detected by AsTdEA",
    ],
    [
      "Technical debt",
      selectedResult
        ? formatNumber(selectedResult.TDAmount, 2)
        : "—",
      "Calculated debt amount",
    ],
  ];

  const issueMetrics = [
    ["Class cycles", "classCDCount"],
    ["Package cycles", "packageCDCount"],
    ["Hub dependencies", "HDCount"],
    ["Unstable dependencies", "UDCount"],
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">A</div>

          <div className="brand-text">
            <h2>ArchiSense</h2>
            <span>ARCHITECTURE INTELLIGENCE</span>
          </div>
        </div>

        <p className="nav-label">WORKSPACE</p>

        <nav>
          {menu.map(([label, symbol]) => (
            <button
              key={label}
              className={`nav-item ${
                active === label ? "selected" : ""
              }`}
              onClick={() => setActive(label)}
            >
              <span>{symbol}</span>
              {label}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="engine-status">
            <span className="status-dot" />

            <div>
              <strong>Analysis engine</strong>
              <small>AsTdEA engine available locally</small>
            </div>
          </div>

          <p>ArchiSense · Research Edition</p>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div>
            <span className="breadcrumb">
              Workspace / {active}
            </span>
            <h1>{active}</h1>
          </div>

          <div className="top-right">
            <span className="environment">
              LOCAL ENVIRONMENT
            </span>

            <div className="avatar" title="ArchiSense">
              <svg
                viewBox="0 0 24 24"
                width="21"
                height="21"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <circle cx="12" cy="8" r="3.5" />
                <path d="M5 20c.8-3.5 3.3-5.5 7-5.5s6.2 2 7 5.5" />
              </svg>
            </div>
          </div>
        </header>

        <section className="content">

          {/* Keep Projects mounted during tab switching. */}
          <div
            style={{
              display: active === "Projects" ? "block" : "none",
            }}
          >
            <Projects onUploadComplete={refreshProjects} />
          </div>

          {active === "Evolution" ? (
            <Evolution
              key={refreshKey}
              projectId={project}
            />
          ) : active === "Analysis" ? (
            <Analysis
              key={refreshKey}
              projectId={project}
              versions={versions}
            />
          ) : active === "Reports" ? (
            <Reports
              key={refreshKey}
              projectId={project}
            />
          ) : active === "Overview" ? (
            <>
              <div className="welcome-row">
                <div>
                  <h2>
                    Architectural Technical Debt Analysis
                  </h2>

                  <p>
                    Analyze architecture smells, dependencies and
                    technical debt across software versions.
                  </p>
                </div>

                <span className="research-tag">
                  RESEARCH WORKSPACE
                </span>
              </div>

              <div className="metric-grid">
                {metricCards.map(([label, value, detail]) => (
                  <article className="metric-card" key={label}>
                    <span>{label}</span>
                    <strong>{value}</strong>
                    <small>{detail}</small>
                  </article>
                ))}
              </div>

              <div className="workspace-grid">
                <section className="panel project-panel">
                  <div className="panel-heading">
                    <div>
                      <h3>Analysis workspace</h3>
                      <p>
                        Choose a project and its software versions.
                      </p>
                    </div>

                    <span className="step-label">STEP 01</span>
                  </div>

                  <label htmlFor="project">Project</label>

                  <select
                    id="project"
                    value={project}
                    onChange={(event) =>
                      setProject(event.target.value)
                    }
                    disabled={loading || projects.length === 0}
                  >
                    {projects.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name}
                      </option>
                    ))}
                  </select>

                  {selectedProject && (
                    <p>{selectedProject.description}</p>
                  )}

                  <label>Software versions</label>

                  <div className="version-list">
                    {versions.map((version) => (
                      <label
                        className="version-option"
                        key={version}
                      >
                        <input
                          type="checkbox"
                          checked={selectedVersions.includes(version)}
                          onChange={() => toggleVersion(version)}
                          disabled={analysisRunning}
                        />

                        <span>{version}</span>
                        <small>Prepared dataset</small>
                      </label>
                    ))}

                    {!loading && versions.length === 0 && (
                      <p>No versions available.</p>
                    )}
                  </div>

                  <button
                    className="primary-button"
                    onClick={startAnalysis}
                    disabled={
                      selectedVersions.length === 0 ||
                      analysisRunning
                    }
                  >
                    {analysisRunning
                      ? "Running Analysis..."
                      : "Run Fresh Analysis"}

                    <span>
                      {analysisRunning ? "..." : "→"}
                    </span>
                  </button>

                  {notice && (
                    <div className="notice" role="status">
                      {notice}
                    </div>
                  )}

                  {lastRunId && (
                    <div className="status-note">
                      <strong>Latest analysis run</strong>
                      <p>{lastRunId}</p>
                    </div>
                  )}
                </section>

                <section className="panel status-panel">
                  <div className="panel-heading">
                    <div>
                      <h3>System status</h3>
                      <p>
                        Application integration overview.
                      </p>
                    </div>

                    <span className="status-dot" />
                  </div>

                  <div className="status-row">
                    <span>React frontend</span>
                    <strong className="good">Running</strong>
                  </div>

                  <div className="status-row">
                    <span>AsTdEA engine</span>
                    <strong>Available locally</strong>
                  </div>

                  <div className="status-row">
                    <span>Java backend</span>
                    <strong
                      className={
                        backendStatus === "Connected"
                          ? "good"
                          : "pending"
                      }
                    >
                      {backendStatus}
                    </strong>
                  </div>

                  <div className="status-row">
                    <span>Results integration</span>
                    <strong className="good">
                      {results.length > 0
                        ? "Connected"
                        : "Pending"}
                    </strong>
                  </div>

                  <div className="status-note">
                    <strong>Selected version</strong>
                    <p>
                      {selectedVersion || "No version selected"}
                    </p>
                  </div>
                </section>
              </div>

              <section className="panel results-panel">
                <div className="panel-heading">
                  <div>
                    <h3>Analysis results</h3>
                    <p>
                      Latest AsTdEA analysis measurements for the
                      selected version.
                    </p>
                  </div>

                  <span className="pending-tag">
                    {selectedResult
                      ? "RESULTS AVAILABLE"
                      : "NO RESULTS"}
                  </span>
                </div>

                {selectedResult ? (
                  <div className="metric-grid">
                    {issueMetrics.map(([label, key]) => (
                      <article className="metric-card" key={key}>
                        <span>{label}</span>

                        <strong>
                          {formatNumber(selectedResult[key])}
                        </strong>

                        <small>{selectedVersion}</small>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state">
                    <div className="empty-icon">⌁</div>

                    <strong>No version selected</strong>

                    <p>
                      Select a software version to view its existing
                      AsTdEA results.
                    </p>
                  </div>
                )}
              </section>

              <footer>
                ArchiSense — Evidence-Based Prioritization of
                Architectural Technical Debt in Java Systems
              </footer>
            </>
          ) : null}
        </section>
      </main>
    </div>
  );
}

export default App;
