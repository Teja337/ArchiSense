# ArchiSense — System Architecture

## 1. Introduction

ArchiSense is a research-oriented application for analyzing architectural technical debt in Java software systems.

The application combines a React frontend, a Spring Boot backend, and the existing AsTdEA architecture analysis engine. It supports exploration of prepared Apache Ant analysis results and fresh analysis of uploaded Maven-based Java projects.

The architecture separates the user interface, backend processing, analysis engine, input datasets, and generated analysis output.

## 2. High-Level Architecture

The application follows a frontend-backend architecture.

```text
                 USER
                   |
                   v
          REACT FRONTEND
          (React + Vite)
                   |
                   | HTTP REST API
                   v
          SPRING BOOT BACKEND
                   |
          +--------+---------+
          |                  |
          v                  v
     CONTROLLERS          SERVICES
          |                  |
          +--------+---------+
                   |
          +--------+---------+
          |                  |
          v                  v
     PREPARED DATA       UPLOADED PROJECTS
     APACHE ANT          MAVEN JAVA PROJECTS
          |                  |
          v                  v
     EXISTING RESULTS    BUILD AND METADATA
                             |
                             v
                       ASTDEA ENGINE
                             |
                             v
                       GENERATED RESULTS
```

The frontend communicates with the backend through HTTP requests. The backend handles project management, upload processing, metadata preparation, analysis execution, and result retrieval.

## 3. Frontend Architecture

### 3.1 Technology

The frontend uses:

- React 19
- React DOM
- Vite
- JavaScript
- CSS
- Oxlint for linting

The frontend development server runs at `http://localhost:5173/`.

### 3.2 Source Structure

The frontend source code is organized under `frontend/src/`.

| File or Directory | Responsibility |
|---|---|
| `main.jsx` | Frontend application entry point |
| `App.jsx` | Main application component and shared application state |
| `api.js` | HTTP communication with the backend |
| `Projects.jsx` | Project listing and upload interface |
| `Analysis.jsx` | Architecture issue exploration |
| `Evolution.jsx` | Software evolution information |
| `Reports.jsx` | Metrics and report presentation |
| `App.css` | Application-specific styling |
| `index.css` | Global styling |
| `assets/` | Frontend assets |

### 3.3 Main Application Component

`App.jsx` manages the primary application workspace.

Its responsibilities include:

- Maintaining the active navigation section.
- Loading registered projects.
- Managing the selected project and available versions.
- Retrieving project metrics and version information.
- Displaying overview metrics.
- Initiating fresh analysis.
- Refreshing project information after successful uploads.
- Passing project information to the Analysis, Evolution, and Reports components.

The application provides five main sections:

1. Overview
2. Projects
3. Analysis
4. Evolution
5. Reports

### 3.4 API Communication

`api.js` centralizes the frontend's HTTP communication.

The configured API base URL is:

`http://localhost:8080/api`

The module provides functions for:

- Retrieving projects.
- Retrieving project versions.
- Retrieving project results.
- Retrieving evolution metrics.
- Uploading Maven-based Java project ZIP files.

The upload operation uses multipart form data with the field name `file`.

## 4. Backend Architecture

### 4.1 Technology

The backend uses:

- Java 27
- Spring Boot 4.1.2
- Spring WebMVC
- Spring Boot Actuator
- Apache Commons CSV
- Apache BCEL
- Maven

The backend runs at `http://localhost:8080`.

### 4.2 Package Structure

Backend source code is organized under:

`backend/src/main/java/com/archisense/backend/`

The main packages are:

```text
com.archisense.backend/
├── config/
├── controller/
└── service/
```

### 4.3 Configuration Package

**CorsConfig.java**

Configures cross-origin communication between the local frontend and backend.

The application permits the frontend development origin `http://localhost:5173` for API communication.

### 4.4 Controller Package

Controllers expose REST endpoints and coordinate requests with backend services.

| Controller | Responsibility |
|---|---|
| `ProjectController` | Lists registered projects and their status information |
| `ProjectUploadController` | Accepts uploaded project ZIP files and coordinates processing |
| `VersionController` | Retrieves available software versions |
| `ResultsController` | Retrieves project metrics and detailed issue records |
| `AnalysisController` | Initiates AsTdEA analysis |
| `EvolutionController` | Retrieves Apache Ant inter-version metrics |
| `IssueController` | Provides Apache Ant issue information |

### 4.5 Service Package

Services contain the main backend processing responsibilities.

| Service | Responsibility |
|---|---|
| `ProjectUploadService` | Handles uploaded project storage and validation |
| `MavenBuildService` | Executes Maven builds for uploaded projects |
| `MetadataGenerationService` | Generates project metadata |
| `AsTdEAMetadataPreparationService` | Prepares generated input data for AsTdEA |
| `EngineExecutionService` | Executes the analysis engine |
| `CsvReaderService` | Reads CSV-based analysis information |
| `IssueService` | Retrieves architecture issue information |

The service layer separates processing operations from the REST controllers.

## 5. AsTdEA Engine Integration

ArchiSense uses the existing AsTdEA implementation as its architecture analysis engine.

The original engine is maintained under:

`original/AsTdEA/`

The configured engine files include:

- `original/AsTdEA/out/artifacts/AsTdEA/AsTdEA.jar`
- `original/AsTdEA/libs/arcan-1.2.1-modded.jar`
- `original/AsTdEA/libs/lib/`

The backend's `EngineExecutionService` coordinates analysis execution using the configured engine and input directories.

Generated analysis output is maintained separately under:

`runtime/engine-output/`

This separation helps preserve the original engine files, prepared datasets, and generated execution results.

## 6. Analysis Workflows

ArchiSense supports two distinct analysis workflows.

### 6.1 Prepared Apache Ant Dataset

The Apache Ant workflow uses the prepared multi-version dataset.

The dataset is maintained under:

`datasets/ant/`

The workflow is:

1. The frontend requests the registered Apache Ant project.
2. The backend retrieves available software versions.
3. The backend reads prepared analysis results.
4. The frontend displays metrics and architecture issues.
5. Evolution information is retrieved from the generated inter-version metrics.

The prepared Apache Ant results support comparisons across the available software versions.

### 6.2 Uploaded Maven-Based Java Projects

The uploaded-project workflow supports fresh analysis.

The processing sequence is:

```text
ZIP FILE UPLOAD
      |
      v
UPLOAD VALIDATION
      |
      v
PROJECT EXTRACTION
      |
      v
MAVEN BUILD
      |
      v
METADATA PREPARATION
      |
      v
ASTDEA INPUT GENERATION
      |
      v
ENGINE EXECUTION
      |
      v
RESULT STORAGE
      |
      v
FRONTEND RESULT RETRIEVAL
```

The backend performs the following operations:

1. Receives and validates the uploaded ZIP file.
2. Extracts the Maven project.
3. Builds the project using Maven.
4. Discovers Java source and compiled class directories.
5. Generates the metadata required by AsTdEA.
6. Prepares the AsTdEA input directory.
7. Executes the analysis engine.
8. Stores the generated results.
9. Makes the results available through backend APIs.

The upload workflow records processing status and failure information when processing does not complete successfully.

### 6.3 Processing Status

Project processing uses statuses including:

- `UPLOADED`
- `PROCESSING`
- `ANALYSIS_COMPLETED`
- `FAILED`
- `READY_FOR_ANALYSIS`

Failure information can include the processing stage and an associated error message.

## 7. Data and Result Management

ArchiSense maintains separate locations for different categories of data.

| Directory | Purpose |
|---|---|
| `datasets/` | Prepared analysis datasets |
| `original/AsTdEA/` | Original AsTdEA implementation and required engine files |
| `uploads/` | Uploaded Java projects and generated project input |
| `runtime/engine-output/` | Generated analysis execution output |

Analysis results are stored in CSV-based files generated or maintained by the AsTdEA workflow.

The backend reads these files and provides the relevant information through REST endpoints. The frontend presents the retrieved metrics and issue details.

## 8. REST API Integration

The frontend and backend communicate through REST endpoints.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/projects` | Retrieve registered projects |
| POST | `/api/projects/upload` | Upload and process a Java project |
| GET | `/api/projects/{projectId}/versions` | Retrieve project versions |
| GET | `/api/projects/{projectId}/results` | Retrieve project metrics |
| GET | `/api/projects/{projectId}/issues/{issueType}/{versionIndex}` | Retrieve detailed issue records |
| POST | `/api/analysis/run?projectId={projectId}` | Execute analysis |
| GET | `/api/projects/ant/evolution` | Retrieve Apache Ant evolution metrics |

Apache Ant-specific issue endpoints are also provided through:

`/api/projects/ant/issues/`

Supported issue categories include class cycles, package cycles, hub dependencies, and unstable dependencies.

## 9. Error Handling and Validation

The backend performs validation during project upload and processing.

Important validation and error-handling responsibilities include:

- ZIP archive validation.
- Protection against unsafe archive extraction paths.
- Maven build failure reporting.
- Java source and metadata validation.
- Analysis execution error handling.
- Project status persistence.
- Reporting of processing failure stages and messages.

The frontend handles unsuccessful API responses and displays appropriate error or status messages.

## 10. Architecture and Implementation Boundaries

The application maintains a distinction between the existing analysis engine and the ArchiSense application.

| Component | Responsibility |
|---|---|
| React frontend | User interaction and result presentation |
| Spring Boot backend | Application APIs and processing coordination |
| AsTdEA | Existing architecture analysis |
| Prepared datasets | Existing Apache Ant multi-version input and results |
| Uploaded project storage | User-provided Maven projects |
| Generated output | Results from analysis execution |

The original AsTdEA implementation is preserved separately from the main application code. A minimal Arcan runtime null-handling correction and rebuilt engine JAR are documented in the baseline reproduction and source-code audit documents.

## 11. Conclusion

ArchiSense combines a React-based interface, a Spring Boot processing backend, and the existing AsTdEA architecture analysis engine.

Its architecture supports both exploration of prepared multi-version Apache Ant results and fresh analysis of uploaded Maven-based Java projects.

The separation of frontend presentation, backend services, engine integration, datasets, and generated output provides a structured foundation for further application development and future research contributions.