# ArchiSense — Setup and Installation Guide

## 1. Introduction

This document describes the prerequisites, installation steps, configuration, startup procedure, and basic verification process for the ArchiSense application.

ArchiSense consists of a React frontend, a Spring Boot backend, and the existing AsTdEA architecture analysis engine.

## 2. System Requirements

The verified development environment uses:

| Component | Version / Requirement |
|---|---|
| Operating System | Windows 11 |
| Java Development Kit | JDK 27 |
| Node.js | 26.13.0 |
| npm | 11.20.0 |
| Spring Boot | 4.1.2 |
| React | 19.3.1 |
| Vite | 8.3.6 or compatible resolved version |
| Git | Required for repository management |

The backend includes a Maven Wrapper (`mvnw.cmd`) for Maven operations.

## 3. Repository Structure

The main project directories are:

```text
ArchiSense/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── datasets/
│   └── ant/
├── original/
│   └── AsTdEA/
├── runtime/
│   └── arcan-source/
├── docs/
│   └── audit/
├── uploads/
└── .gitignore
```

Generated analysis output and uploaded project data are maintained separately from the original engine and prepared datasets.

## 4. Obtain the Project

Clone the ArchiSense repository:

```bash
git clone https://github.com/Teja337/ArchiSense.git
```

Navigate into the project directory:

```bash
cd ArchiSense
```

Ensure that the original AsTdEA files and prepared dataset directories are present in the repository checkout.

## 5. Backend Setup

### 5.1 Backend Configuration

The backend configuration is maintained in:

`backend/src/main/resources/application.properties`

Important properties include:

| Property | Configured Value |
|---|---|
| `archisense.project-root` | `..` |
| `archisense.engine.jar` | `original/AsTdEA/out/artifacts/AsTdEA/AsTdEA.jar` |
| `archisense.engine.arcan-jar` | `original/AsTdEA/libs/arcan-1.2.1-modded.jar` |
| `archisense.engine.lib-dir` | `original/AsTdEA/libs/lib` |
| `archisense.dataset-dir` | `datasets` |
| `archisense.output-dir` | `runtime/engine-output` |
| `archisense.upload-dir` | `uploads` |

The multipart upload limits are configured to 500 MB for both individual files and requests.

### 5.2 Start the Backend

Open PowerShell and navigate to the backend directory:

```powershell
cd backend
```

Start the Spring Boot application using the Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs at:

`http://localhost:8080`

Keep this terminal running while using the frontend.

## 6. Frontend Setup

### 6.1 Install Dependencies

Open a separate PowerShell terminal and navigate to the frontend directory:

```powershell
cd frontend
```

Install the dependencies:

```powershell
npm.cmd install
```

### 6.2 Start the Development Server

Run:

```powershell
npm.cmd run dev
```

The frontend is available at:

`http://localhost:5173/`

Keep this terminal running while using the application.

## 7. Running the Application

To use ArchiSense locally:

1. Start the Spring Boot backend.
2. Start the Vite frontend.
3. Open `http://localhost:5173/` in a browser.
4. Select a project from the Projects section.
5. Explore its available metrics, architecture issues, evolution information, and reports.
6. Upload a Maven-based Java project when fresh uploaded-project analysis is required.

The frontend communicates with the backend through the `/api/**` endpoints. The backend CORS configuration permits the local frontend origin.

## 8. Testing and Build Verification

### 8.1 Backend Tests

From the backend directory:

```powershell
.\mvnw.cmd test
```

### 8.2 Frontend Lint

From the frontend directory:

```powershell
npm.cmd run lint
```

### 8.3 Frontend Production Build

Run:

```powershell
npm.cmd run build
```

The production build is generated in the frontend `dist/` directory.

### 8.4 Frontend Preview

To preview the production build locally:

```powershell
npm.cmd run preview
```

## 9. Analysis Data and Execution

ArchiSense supports two analysis workflows:

**Prepared Apache Ant dataset**

The Apache Ant project uses the prepared multi-version dataset and its associated AsTdEA results. These results support project metrics, architecture issue exploration, and evolution analysis.

**Uploaded Maven-based Java projects**

Uploaded projects are processed through the backend workflow:

1. ZIP upload and validation.
2. Maven project build.
3. AsTdEA metadata preparation.
4. Architecture analysis.
5. Storage and retrieval of analysis results.

Uploaded-project processing failures are reported with the corresponding processing stage and error information when available.

## 10. Important Notes

- Preserve the original AsTdEA implementation and its associated files.
- Keep the configured engine JARs and required libraries available at their expected paths.
- Do not manually delete generated analysis output or uploaded project data while the application is using them.
- Uploaded-project processing expects a valid Maven project containing Java source code.
- A project with only one analyzed version cannot provide a multi-version evolution trend.
- The prepared Apache Ant dataset and uploaded-project analysis are separate workflows.

## 11. Related Documentation

Additional technical information is maintained in:

- `docs/audit/application-implementation.md` — Application implementation and validation.
- `docs/audit/baseline-reproduction.md` — Original AsTdEA baseline reproduction.
- `docs/audit/dataset-preparation.md` — Dataset preparation and validation.
- `docs/audit/source-code-audit.md` — Original source-code audit.
- `docs/audit/source-file-inventory.txt` — Audited source-file inventory.

## 12. Conclusion

This guide provides the setup and execution instructions for the ArchiSense development environment. Following these steps allows the frontend, backend, and existing AsTdEA analysis engine to be used together for exploring prepared architectural analysis results and processing uploaded Maven-based Java projects.