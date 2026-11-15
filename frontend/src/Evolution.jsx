
import { useEffect, useState } from "react";
import { getEvolution } from "./api";

function Evolution() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadEvolution() {
      try {
        const result = await getEvolution("ant");
        setData(result);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    loadEvolution();
  }, []);

  if (loading) {
    return <section className="panel">Loading evolution metrics...</section>;
  }

  if (error) {
    return <section className="panel">Unable to load evolution data: {error}</section>;
  }

  if (!data) return null;

  const metrics = [
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

  return (
    <section className="panel results-panel">
      <div className="panel-heading">
        <div>
          <h3>Apache Ant — Evolution Analysis</h3>
          <p>
            Architecture smell evolution across the analyzed
            software versions.
          </p>
        </div>
        <span className="pending-tag">REAL AsTdEA DATA</span>
      </div>

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
