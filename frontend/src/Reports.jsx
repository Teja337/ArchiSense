import { useEffect, useState } from "react";
import { getResults, getEvolution } from "./api";

const versionNames = ["Ant 1.9.4", "Ant 1.9.5", "Ant 1.9.6"];

function formatNumber(value, decimals = 0) {
  const number = Number(value);

  if (!Number.isFinite(number)) return "—";

  return number.toLocaleString("en-US", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

function Reports() {
  const [results, setResults] = useState([]);
  const [evolution, setEvolution] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadReportData() {
      try {
        const [resultData, evolutionData] = await Promise.all([
          getResults("ant"),
          getEvolution("ant"),
        ]);

        setResults(resultData);
        setEvolution(evolutionData);
      } catch (err) {
        setError(err.message || "Unable to load report data.");
      } finally {
        setLoading(false);
      }
    }

    loadReportData();
  }, []);

  function exportCSV() {
    const headers = [
      "Software Version",
      "Architecture Smells",
      "Technical Debt",
      "Class Cycles",
      "Package Cycles",
      "Hub Dependencies",
      "Unstable Dependencies",
    ];

    const rows = results.map((item) => [
      versionNames[Number(item.versionIndex)] || item.versionIndex,
      item.ASCount,
      item.TDAmount,
      item.classCDCount,
      item.packageCDCount,
      item.HDCount,
      item.UDCount,
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
    link.download = "archisense-ant-analysis-report.csv";
    link.click();

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

  if (!evolution || results.length === 0) {
    return (
      <section className="panel report-panel">
        No report data is currently available.
      </section>
    );
  }

  const issueTypes = [
    {
      label: "Class cycles",
      key: "classCDCount",
    },
    {
      label: "Package cycles",
      key: "packageCDCount",
    },
    {
      label: "Hub dependencies",
      key: "HDCount",
    },
    {
      label: "Unstable dependencies",
      key: "UDCount",
    },
  ];

  const totalSmells = Number(evolution.numOfIntraVersionSmells);
  const totalInterSmells = Number(evolution.numOfInterVersionSmells);

  return (
    <section className="reports-page">
      <div className="welcome-row">
        <div>
          <h2>Project Analysis Reports</h2>
          <p>
            Summary and comparison of original AsTdEA measurements
            for Apache Ant.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={exportCSV}
        >
          Export CSV ↓
        </button>
      </div>

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
            <strong>{evolution.analysedVersions}</strong>
            <small>Apache Ant releases</small>
          </article>

          <article className="metric-card">
            <span>Analysis time span</span>
            <strong>
              {formatNumber(evolution.analysedTimeSpanInDays)}
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

      <section className="panel results-panel">
        <div className="panel-heading">
          <div>
            <h3>Version-wise comparison</h3>
            <p>Architecture measurements for each software version.</p>
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
                      {versionNames[Number(item.versionIndex)] ||
                        `Version ${item.versionIndex}`}
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
            <p>
              Intra-version issue counts reported by AsTdEA.
            </p>
          </div>
        </div>

        <div className="report-distribution">
          {issueTypes.map((issue) => {
            const value = Number(evolution[
              `numOfIntraVersion${
                issue.key === "classCDCount"
                  ? "ClassCDs"
                  : issue.key === "packageCDCount"
                    ? "PackCDs"
                    : issue.key === "HDCount"
                      ? "HDs"
                      : "UDs"
              }`
            ]);

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
          Counts are displayed as reported by AsTdEA. The percentages
          are visual proportions relative to the intra-version smell
          total, not independent risk scores.
        </p>
      </section>

      <footer className="report-footer">
        Report generated from prepared Apache Ant AsTdEA results.
        No new analysis was executed.
      </footer>
    </section>
  );
}

export default Reports;