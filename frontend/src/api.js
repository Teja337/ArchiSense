
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
    `${API_BASE_URL}/projects/${encodeURIComponent(projectId)}/versions`
  );

  if (!response.ok) {
    throw new Error("Failed to load project versions");
  }

  return response.json();
}

export async function getResults(projectId) {
  const response = await fetch(
    `${API_BASE_URL}/projects/${encodeURIComponent(projectId)}/results`
  );

  if (!response.ok) {
    throw new Error("Failed to load project results");
  }

  return response.json();
}

export async function getEvolution(projectId) {
  const response = await fetch(
    `${API_BASE_URL}/projects/${encodeURIComponent(projectId)}/evolution`
  );

  if (!response.ok) {
    throw new Error("Failed to load evolution metrics");
  }

  return response.json();
}

// Upload a Java Maven project ZIP and run its analysis.
export async function uploadProject(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/projects/upload`, {
    method: "POST",
    body: formData,
  });

  let data;

  try {
    data = await response.json();
  } catch {
    data = {
      status: "FAILED",
      stage: "UNKNOWN",
      message: "The server returned an invalid response.",
    };
  }

  if (!response.ok) {
    const error = new Error(
      data.message || data.error || "Project upload failed."
    );

    error.responseData = data;
    error.statusCode = response.status;

    throw error;
  }

  return data;
}
