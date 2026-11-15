import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = "http://localhost:8080/api";

const issueTypes = [
  { value: "class-cycles", label: "Class cycles" },
  { value: "package-cycles", label: "Package cycles" },
  { value: "hub-dependencies", label: "Hub dependencies" },
  { value: "unstable-dependencies", label: "Unstable dependencies" },
];

const versions = [
  { value: 0, label: "Ant 1.9.4" },
  { value: 1, label: "Ant 1.9.5" },
  { value: 2, label: "Ant 1.9.6" },
];

function formatLabel(value) {
  return value
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (letter) => letter.toUpperCase());
}

function Analysis() {
  const [issueType, setIssueType] = useState("class-cycles");
  const [versionIndex, setVersionIndex] = useState("0");
  const [issues, setIssues] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadIssues() {
      setLoading(true);
      setError("");
      setIssues([]);
      setSearch("");

      try {
        const response = await fetch(
          `${API_BASE_URL}/projects/ant/issues/${issueType}/${versionIndex}`
        );

        if (!response.ok) {
          throw new Error("Unable to load analysis records.");
        }

        const data = await response.json();
        setIssues(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    loadIssues();
  }, [issueType, versionIndex]);

  const selectedIssueType = issueTypes.find(
    (item) => item.value === issueType
  );

  const selectedVersion = versions.find(
    (item) => String(item.value) === versionIndex
  );

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

      <section className="panel">
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
            >
              {versions.map((version) => (
                <option key={version.value} value={version.value}>
                  {version.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="panel results-panel">
        <div className="panel-heading">
          <div>
            <h3>
              {selectedIssueType?.label} — {selectedVersion?.label}
            </h3>
            <p>Original issue records and their measured properties.</p>
          </div>

          <span className="pending-tag">
            {loading ? "LOADING" : `${filteredIssues.length} RECORDS`}
          </span>
        </div>

        {!loading && !error && issues.length > 0 && (
          <div className="analysis-search">
            <input
              type="search"
              placeholder="Search issue ID, component or property..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />

            <span>
              Showing {filteredIssues.length} of {issues.length} records
            </span>
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

        {!loading && !error && issues.length === 0 && (
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