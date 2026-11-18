
import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = "http://localhost:8080/api";

const issueTypes = [
  { value: "class-cycles", label: "Class cycles" },
  { value: "package-cycles", label: "Package cycles" },
  { value: "hub-dependencies", label: "Hub dependencies" },
  { value: "unstable-dependencies", label: "Unstable dependencies" },
];

function formatLabel(value) {
  return String(value)
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (letter) => letter.toUpperCase());
}

function Analysis({ projectId = "ant", versions = [] }) {
  const [issueType, setIssueType] = useState("class-cycles");
  const [versionIndex, setVersionIndex] = useState("0");
  const [issues, setIssues] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    setVersionIndex("0");
    setIssues([]);
    setSearch("");
    setError("");
  }, [projectId]);

  useEffect(() => {
    if (!projectId || versions.length === 0) {
      setIssues([]);
      setError("");
      setLoading(false);
      return;
    }

    const controller = new AbortController();

    async function loadIssues() {
      setLoading(true);
      setError("");
      setIssues([]);
      setSearch("");

      try {
        const response = await fetch(
          `${API_BASE_URL}/projects/${encodeURIComponent(projectId)}/issues/${issueType}/${versionIndex}`,
          { signal: controller.signal }
        );

        if (!response.ok) {
          throw new Error(
            `Unable to load analysis records (HTTP ${response.status}).`
          );
        }

        const data = await response.json();

        if (!Array.isArray(data)) {
          throw new Error("The server returned an unexpected results format.");
        }

        setIssues(data);
      } catch (err) {
        if (err.name !== "AbortError") {
          setError(err.message);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadIssues();

    return () => controller.abort();
  }, [projectId, issueType, versionIndex, versions]);

  const selectedIssueType = issueTypes.find(
    (item) => item.value === issueType
  );

  const selectedVersion = versions[Number(versionIndex)];

  const filteredIssues = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return issues;

    return issues.filter((issue) =>
      Object.values(issue).some((value) =>
        String(value ?? "").toLowerCase().includes(query)
      )
    );
  }, [issues, search]);

  return (
    <section className="analysis-page">
      <div className="welcome-row">
        <div>
          <h2>Architecture Issue Analysis</h2>
          <p>
            Explore architecture issues detected by the original AsTdEA
            analysis.
          </p>
        </div>
        <span className="research-tag">REAL AsTdEA RESULTS</span>
      </div>

      <section className="panel analysis-panel">
        <div className="panel-heading">
          <div>
            <h3>Analysis filters</h3>
            <p>Select an issue category and software version.</p>
          </div>
          <span className="step-label">FILTERS</span>
        </div>

        <div className="analysis-filters">
          <div>
            <label htmlFor="issueType">Issue category</label>
            <select
              id="issueType"
              value={issueType}
              onChange={(event) => setIssueType(event.target.value)}
            >
              {issueTypes.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="analysisVersion">Software version</label>
            <select
              id="analysisVersion"
              value={versionIndex}
              onChange={(event) => setVersionIndex(event.target.value)}
              disabled={versions.length === 0}
            >
              {versions.map((version, index) => (
                <option key={version} value={index}>
                  {version}
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="panel results-panel analysis-panel">
        <div className="panel-heading">
          <div>
            <h3>
              {selectedIssueType?.label} — {selectedVersion || "No version selected"}
            </h3>
            <p>Original issue records and their measured properties.</p>
          </div>

          <span className="pending-tag">
            {loading ? "LOADING" : `${filteredIssues.length} RECORDS`}
          </span>
        </div>

        {versions.length === 0 && (
          <div className="empty-state">
            <strong>No software versions available</strong>
            <p>Select a project with an available version in Overview.</p>
          </div>
        )}

        {!loading && !error && versions.length > 0 && issues.length === 0 && (
  <div className="empty-state">
    <strong>No {selectedIssueType?.label?.toLowerCase()} detected</strong>
    <p>
      The AsTdEA analysis completed successfully, but no
      {` ${selectedIssueType?.label?.toLowerCase()} `}
      were found in this software version.
    </p>
  </div>
)}

        {loading && (
          <div className="empty-state">
            <p>Loading analysis records...</p>
          </div>
        )}

        {!loading && error && (
          <div className="notice">{error}</div>
        )}

        {!loading && !error && versions.length > 0 && issues.length === 0 && (
          <div className="empty-state">
            <strong>No records found</strong>
            <p>No records are available for this selection.</p>
          </div>
        )}

        {!loading && !error && issues.length > 0 &&
          filteredIssues.length === 0 && (
            <div className="empty-state">
              <strong>No matching records</strong>
              <p>Try another search term.</p>
            </div>
          )}

        {!loading && !error && filteredIssues.length > 0 && (
          <div className="analysis-table-wrapper">
            <table className="analysis-table">
              <thead>
                <tr>
                  <th>Issue ID</th>
                  <th>Component</th>
                  <th>Severity score</th>
                  <th>Size</th>
                  <th>All properties</th>
                </tr>
              </thead>

              <tbody>
                {filteredIssues.map((issue, index) => {
                  const component =
                    issue.affectedComponents ||
                    issue.mainComponent ||
                    "—";

                  return (
                    <tr key={issue.id ?? index}>
                      <td>
                        <strong>{issue.id ?? "—"}</strong>
                      </td>

                      <td className="component-cell">
                        {String(component)}
                      </td>

                      <td>{issue.severityScore ?? "—"}</td>

                      <td>{issue.size ?? "—"}</td>

                      <td>
                        <details className="property-details">
                          <summary>View details</summary>

                          <div className="property-list">
                            {Object.entries(issue).map(([key, value]) => (
                              <div className="property-item" key={key}>
                                <strong>{formatLabel(key)}</strong>
                                <p>
                                  {value === null ||
                                  value === undefined ||
                                  value === ""
                                    ? "—"
                                    : String(value)}
                                </p>
                              </div>
                            ))}
                          </div>
                        </details>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}

export default Analysis;
