
import { useEffect, useState } from "react";
import { getResults, getEvolution } from "./api";

function formatNumber(value, decimals = 0) {
  const number = Number(value);

  if (!Number.isFinite(number)) return "—";

  return number.toLocaleString("en-US", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

function Reports({ projectId = "ant" }) {
  const [results, setResults] = useState([]);
  const [evolution, setEvolution] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const isAnt = projectId === "ant";

  useEffect(() => {
    let cancelled = false;

    async function loadReportData() {
      setLoading(true);
      setError("");
      setResults([]);
      setEvolution(null);

      try {
        const resultData = await getResults(projectId);

        if (!Array.isArray(resultData)) {
          throw new Error("The server returned an unexpected results format.");
        }

        let evolutionData = null;

        if (isAnt) {
          evolutionData = await getEvolution(projectId);
        }

        if (!cancelled) {
          setResults(resultData);
          setEvolution(evolutionData);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Unable to load report data.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    if (projectId) {
      loadReportData();
    }

    return () => {
      cancelled = true;
    };
  }, [projectId, isAnt]);

  function exportCSV() {
    const headers = [
      "Software Version",
      "Architecture Smells",
      "Technical Debt",
      "Class Cycles",
      "Package Cycles",
      "Hub Dependencies",
      "Unstable Dependencies",
      "Lines of Code",
      "Total Classes",
      "Total Packages",
    ];

    const rows = results.map((item) => [
      item.versionLabel || `Version ${item.versionIndex}`,
      item.ASCount,
      item.TDAmount,
      item.classCDCount,
      item.packageCDCount,
      item.HDCount,
      item.UDCount,
      item.LOC,
      item.totalClassCount,
      item.totalPackageCount,
    ]);

    const csvContent = [headers, ...rows]
      .map((row) =>
        row
          .map((value) => `"${String(value ?? "").replace(/"/g, '""')}"`)
          .join(",")
      )
      .join("\n");

    const blob = new Blob([csvContent], {
      type: "text/csv;charset=utf-8;",
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = `archisense-${projectId}-analysis-report.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  if (loading) {
    return (
      <section className="panel report-panel">
        Loading report data...
      </section>
    );
  }

  if (error) {
    return (
      <section className="panel results-panel report-panel">
        <div className="notice">{error}</div>
      </section>
    );
  }

  if (results.length === 0) {
    return (
      <section className="panel report-panel">
        <strong>No report data is currently available.</strong>
        <p>Complete an AsTdEA analysis for this project first.</p>
      </section>
    );
  }

  const issueTypes = [
    { label: "Class cycles", key: "classCDCount" },
    { label: "Package cycles", key: "packageCDCount" },
    { label: "Hub dependencies", key: "HDCount" },
    { label: "Unstable dependencies", key: "UDCount" },
  ];

  const totalSmells = isAnt
    ? Number(evolution?.numOfIntraVersionSmells ?? 0)
    : Number(results[0].ASCount ?? 0);

  const totalInterSmells = Number(
    evolution?.numOfInterVersionSmells ?? 0
  );

  const uploadedProject = results[0];

  return (
    <section className="reports-page">
      <div className="welcome-row">
        <div>
          <h2>Project Analysis Reports</h2>
          <p>
            Summary and comparison of original AsTdEA measurements
            for {isAnt ? "Apache Ant" : "the uploaded project"}.
          </p>
        </div>

        <button className="primary-button" onClick={exportCSV}>
          Export CSV ↓
        </button>
      </div>

      {isAnt ? (
        <>
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h3>Overall project summary</h3>
                <p>Inter-version analysis measurements.</p>
              </div>
              <span className="pending-tag">ORIGINAL DATA</span>
            </div>

            <div className="metric-grid">
              <article className="metric-card">
                <span>Analyzed versions</span>
                <strong>{formatNumber(evolution?.analysedVersions)}</strong>
                <small>Apache Ant releases</small>
              </article>

              <article className="metric-card">
                <span>Analysis time span</span>
                <strong>
                  {formatNumber(evolution?.analysedTimeSpanInDays)}
                </strong>
                <small>Days</small>
              </article>

              <article className="metric-card">
                <span>Intra-version smells</span>
                <strong>{formatNumber(totalSmells)}</strong>
                <small>Across analyzed versions</small>
              </article>

              <article className="metric-card">
                <span>Inter-version smells</span>
                <strong>{formatNumber(totalInterSmells)}</strong>
                <small>Across version transitions</small>
              </article>
            </div>
          </section>
        </>
      ) : (
        <>
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h3>Uploaded project summary</h3>
                <p>
                  Measurements for {uploadedProject.versionLabel || "the uploaded version"}.
                </p>
              </div>
              <span className="pending-tag">ORIGINAL DATA</span>
            </div>

            <div className="empty-state">
              <strong>Single-version report</strong>
              <p>
                Only one software version has been analyzed.
                Inter-version evolution and version comparisons
                are not available yet.
              </p>
            </div>

            <div className="metric-grid">
              <article className="metric-card">
                <span>Architecture smells</span>
                <strong>{formatNumber(uploadedProject.ASCount)}</strong>
                <small>Detected by AsTdEA</small>
              </article>

              <article className="metric-card">
                <span>Technical debt</span>
                <strong>{formatNumber(uploadedProject.TDAmount, 2)}</strong>
                <small>AsTdEA measurement</small>
              </article>

              <article className="metric-card">
                <span>Lines of code</span>
                <strong>{formatNumber(uploadedProject.LOC)}</strong>
                <small>Prepared source metadata</small>
              </article>

              <article className="metric-card">
                <span>Total classes</span>
                <strong>{formatNumber(uploadedProject.totalClassCount)}</strong>
                <small>Analyzed version</small>
              </article>
            </div>
          </section>
        </>
      )}

      <section className="panel results-panel">
        <div className="panel-heading">
          <div>
            <h3>Version-wise comparison</h3>
            <p>Architecture measurements for each analyzed software version.</p>
          </div>
        </div>

        <div className="analysis-table-wrapper">
          <table className="analysis-table">
            <thead>
              <tr>
                <th>Version</th>
                <th>Smells</th>
                <th>Technical debt</th>
                <th>Class cycles</th>
                <th>Package cycles</th>
                <th>Hub dependencies</th>
                <th>Unstable dependencies</th>
              </tr>
            </thead>

            <tbody>
              {results.map((item) => (
                <tr key={item.versionIndex}>
                  <td>
                    <strong>
                      {isAnt
                        ? ["Ant 1.9.4", "Ant 1.9.5", "Ant 1.9.6"][
                            Number(item.versionIndex)
                          ] || item.versionLabel
                        : item.versionLabel || "Uploaded version"}
                    </strong>
                  </td>
                  <td>{formatNumber(item.ASCount)}</td>
                  <td>{formatNumber(item.TDAmount, 2)}</td>
                  <td>{formatNumber(item.classCDCount)}</td>
                  <td>{formatNumber(item.packageCDCount)}</td>
                  <td>{formatNumber(item.HDCount)}</td>
                  <td>{formatNumber(item.UDCount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Architecture issue distribution</h3>
            <p>Issue counts reported by AsTdEA.</p>
          </div>
        </div>

        <div className="report-distribution">
          {issueTypes.map((issue) => {
            const value = isAnt
              ? Number(
                  evolution?.[
                    issue.key === "classCDCount"
                      ? "numOfIntraVersionClassCDs"
                      : issue.key === "packageCDCount"
                        ? "numOfIntraVersionPackCDs"
                        : issue.key === "HDCount"
                          ? "numOfIntraVersionHDs"
                          : "numOfIntraVersionUDs"
                  ] ?? 0
                )
              : Number(uploadedProject[issue.key] ?? 0);

            const percentage =
              totalSmells > 0 ? (value / totalSmells) * 100 : 0;

            return (
              <div className="distribution-item" key={issue.key}>
                <div className="distribution-heading">
                  <strong>{issue.label}</strong>
                  <span>{formatNumber(value)}</span>
                </div>

                <div className="distribution-track">
                  <div
                    className="distribution-fill"
                    style={{ width: `${Math.min(percentage, 100)}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>

        <p className="report-note">
          Counts are displayed as reported by AsTdEA. Percentages
          represent proportions relative to the architecture-smell
          total; they are not independent risk scores.
        </p>
      </section>

      <footer className="report-footer">
        Report generated from prepared AsTdEA results.
        No new analysis was executed.
      </footer>
    </section>
  );
}

export default Reports;
