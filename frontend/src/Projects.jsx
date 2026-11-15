import { useEffect, useState } from "react";
import { getProjects, getVersions } from "./api";

function Projects() {
  const [projects, setProjects] = useState([]);
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadProjects() {
      try {
        const projectData = await getProjects();
        setProjects(projectData);

        if (projectData.length > 0) {
          const versionData = await getVersions(projectData[0].id);
          setVersions(versionData);
        }
      } catch (err) {
        setError(err.message || "Unable to load projects.");
      } finally {
        setLoading(false);
      }
    }

    loadProjects();
  }, []);

  if (loading) {
    return <section className="panel">Loading projects...</section>;
  }

  if (error) {
    return <section className="panel">{error}</section>;
  }

  return (
    <section className="projects-page">
      <div className="welcome-row">
        <div>
          <h2>Projects</h2>
          <p>Manage and explore available software projects.</p>
        </div>
      </div>

      {projects.map((project) => (
        <section className="panel project-card" key={project.id}>
          <div className="panel-heading">
            <div>
              <h3>{project.name}</h3>
              <p>{project.description}</p>
            </div>

            <span className="pending-tag">{project.status}</span>
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

          <h4>Software versions</h4>

          <div className="version-list">
            {versions.map((version) => (
              <div className="version-item" key={version}>
                <span>{version}</span>
                <span className="pending-tag">Available</span>
              </div>
            ))}
          </div>
        </section>
      ))}
    </section>
  );
}

export default Projects;