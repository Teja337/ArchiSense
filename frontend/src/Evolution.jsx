
import { useEffect, useState } from "react";
import { getEvolution, getResults } from "./api";

function Evolution({ projectId = "ant" }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadEvolution() {
      setLoading(true);
      setError("");
      setData(null);

      try {
        let result;

        if (projectId === "ant") {
          result = await getEvolution(projectId);
        } else {
          const results = await getResults(projectId);
          result = Array.isArray(results) ? results[0] : null;
        }

        if (!cancelled) {
          setData(result);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Unable to load evolution data.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    if (projectId) {
      loadEvolution();
    } else {
      setLoading(false);
    }

    return () => {
      cancelled = true;
    };
  }, [projectId]);

  if (loading) {
    return <section className="panel">Loading evolution metrics...</section>;
  }

  if (error) {
    return (
      <section className="panel">
        Unable to load evolution data: {error}
      </section>
    );
  }

  if (!data) {
    return (
      <section className="panel">
        No analysis results are available for this project.
      </section>
    );
  }

  const isAnt = projectId === "ant";

  const antMetrics = [
    ["Analyzed versions", data.analysedVersions],
    ["Analysis time span", `${data.analysedTimeSpanInDays} days`],
    ["Intra-version smells", data.numOfIntraVersionSmells],
    ["Inter-version smells", data.numOfInterVersionSmells],
    ["Intra-version class cycles", data.numOfIntraVersionClassCDs],
    ["Intra-version package cycles", data.numOfIntraVersionPackCDs],
    ["Intra-version hub dependencies", data.numOfIntraVersionHDs],
    ["Intra-version unstable dependencies", data.numOfIntraVersionUDs],
    ["Inter-version class cycles", data.numOfInterVersionClassCDs],
    ["Inter-version package cycles", data.numOfInterVersionPackCDs],
    ["Inter-version hub dependencies", data.numOfInterVersionHDs],
    ["Inter-version unstable dependencies", data.numOfInterVersionUDs],
  ];

  const uploadedMetrics = [
    ["Architecture smells", data.ASCount],
    ["Technical debt amount", data.TDAmount],
    ["Class cycles", data.classCDCount],
    ["Package cycles", data.packageCDCount],
    ["Hub dependencies", data.HDCount],
    ["Unstable dependencies", data.UDCount],
    ["Lines of code", data.LOC],
    ["Total classes", data.totalClassCount],
    ["Total packages", data.totalPackageCount],
  ];

  const metrics = isAnt ? antMetrics : uploadedMetrics;

  return (
    <section className="panel results-panel evolution-page">
      <div className="panel-heading">
        <div>
          <h3>
            {isAnt
              ? "Apache Ant — Evolution Analysis"
              : "Uploaded Project — Architecture Analysis"}
          </h3>
          <p>
            {isAnt
              ? "Architecture smell evolution across the analyzed software versions."
              : `AsTdEA measurements for ${data.versionLabel || "the uploaded version"}.`}
          </p>
        </div>

        <span className="pending-tag">REAL AsTdEA DATA</span>
      </div>

      {!isAnt && (
        <div className="empty-state">
          <strong>Single-version analysis</strong>
          <p>
            This project currently has one analyzed software version.
            Its architecture measurements are shown below. Version-to-version
            evolution will become available when additional versions are analyzed.
          </p>
        </div>
      )}

      <div className="metric-grid">
        {metrics.map(([label, value]) => (
          <article className="metric-card" key={label}>
            <span>{label}</span>
            <strong>{value ?? "—"}</strong>
            <small>Original AsTdEA measurement</small>
          </article>
        ))}
      </div>
    </section>
  );
}

export default Evolution;
