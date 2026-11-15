
const API_BASE_URL = "http://localhost:8080/api";

export async function getProjects() {
  const response = await fetch(`${API_BASE_URL}/projects`);

  if (!response.ok) {
    throw new Error("Failed to load projects");
  }

  return response.json();
}

export async function getVersions(projectId) {
  const response = await fetch(
    `${API_BASE_URL}/projects/${projectId}/versions`
  );

  if (!response.ok) {
    throw new Error("Failed to load project versions");
  }

  return response.json();
}

export async function getResults(projectId) {
  const response = await fetch(
    `${API_BASE_URL}/projects/${projectId}/results`
  );

  if (!response.ok) {
    throw new Error("Failed to load project results");
  }

  return response.json();
}

export async function getEvolution(projectId) {
  const response = await fetch(
    `${API_BASE_URL}/projects/${projectId}/evolution`
  );

  if (!response.ok) {
    throw new Error("Failed to load evolution metrics");
  }

  return response.json();
}
