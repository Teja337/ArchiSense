# ArchiSense — Application Implementation Documentation

## 1. Project Overview

ArchiSense is a web-based application for exploring and understanding architectural technical debt in Java software systems.

The application integrates the existing AsTdEA research implementation and supports both the prepared Apache Ant baseline dataset and uploaded Maven-based Java projects.

The purpose of this implementation phase is to provide an accessible interface for examining architectural measurements, issue details, and evolution across software versions, while supporting project upload, build, metadata preparation, and analysis workflows.

## 2. Technology Stack

### Frontend
- React
- Vite
- JavaScript
- CSS

### Backend
- Java
- Spring Boot
- Maven
- Apache Commons CSV
- Apache BCEL

### Analysis Engine and Data
- Existing AsTdEA implementation
- Original AsTdEA analysis results
- Prepared Apache Ant datasets
- Uploaded Java project source code and compiled classes

## 3. Application Architecture

The application follows a frontend-backend architecture.

1. React provides the user interface.
2. The frontend sends HTTP requests to the Spring Boot backend.
3. The backend manages project information, uploads, builds, metadata preparation, and analysis execution.
4. The backend reads and processes AsTdEA output files.
5. The backend converts analysis results into JSON responses.
6. React displays the returned information.

The original AsTdEA implementation is preserved separately from the ArchiSense application. A minimal null-handling correction was applied to the bundled Arcan source and its rebuilt JAR to allow fresh analysis to complete. This change is documented as an engine compatibility correction, not a new research contribution.

## 4. Implemented Application Modules

### 4.1 Overview
Displays project information, available versions, and summary measurements.

### 4.2 Projects
Displays available projects, uploaded project names, processing statuses, and analyzed versions.

Supports uploading Maven-based Java projects and monitoring processing outcomes.

### 4.3 Analysis
Allows users to explore four architecture issue categories:
- Class cycles
- Package cycles
- Hub dependencies
- Unstable dependencies

Provides version selection, search, issue counts, and expandable issue properties.

### 4.4 Evolution
Displays inter-version measurements obtained from the original AsTdEA results.

For single-version uploaded projects, the application explains that evolution analysis requires multiple software versions rather than presenting fabricated trends.

### 4.5 Reports
Displays an overall analysis summary, version-wise comparisons, and architecture issue distribution. Supports CSV export of the displayed version-wise measurements.

### 4.6 Project Upload and Processing
Supports uploading Maven-based Java project ZIP files.

The processing workflow includes:
1. ZIP validation and extraction.
2. Maven project build.
3. Discovery of Java source and compiled class directories.
4. Metadata preparation for AsTdEA.
5. Fresh architecture analysis.
6. Storage and presentation of analysis results.

The workflow tracks processing stages and project statuses.

### 4.7 Failure Handling
The backend records processing failures and their associated stages.

For failures occurring after project registration, it stores:
- `status.txt`
- `failure-stage.txt`
- `failure-message.txt`

The Projects page displays the failure stage and error message after refreshing the project list. Older failed projects without persisted failure details are handled without causing API errors.

## 5. Backend API Endpoints

| HTTP Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/projects | Retrieve available projects |
| GET | /api/projects/ant/versions | Retrieve Apache Ant software versions |
| GET | /api/projects/ant/results | Retrieve version-wise results |
| GET | /api/projects/ant/evolution | Retrieve evolution metrics |
| GET | /api/projects/ant/issues/class-cycles/{versionIndex} | Retrieve class cycle issues |
| GET | /api/projects/ant/issues/package-cycles/{versionIndex} | Retrieve package cycle issues |
| GET | /api/projects/ant/issues/hub-dependencies/{versionIndex} | Retrieve hub dependency issues |
| GET | /api/projects/ant/issues/unstable-dependencies/{versionIndex} | Retrieve unstable dependency issues |
| POST | /api/projects/upload | Upload and process a Java project |

Uploaded-project processing uses the same project management API structure, with project-specific results and statuses.

## 6. Dataset and Project Processing

The prepared baseline dataset uses Apache Ant versions:
- 1.9.4
- 1.9.5
- 1.9.6

Prepared datasets are stored under `datasets/ant`, and original analysis results are stored under `results/ant`.

The application reads these prepared baseline results without executing a new AsTdEA analysis whenever a user opens the dashboard.

For uploaded Maven projects, the backend performs a build, prepares the required AsTdEA metadata and compiled-class inputs, and executes fresh analysis.

## 7. Verification

### 7.1 Build Verification
- Backend Maven test execution completed successfully.
- All 15 backend tests passed, with zero failures, errors, or skipped tests.
- Frontend production build completed successfully using Vite 8.3.7.
- The latest frontend build transformed 22 modules and completed in 153 ms.

### 7.2 Functional Verification
- Project, version, result, and evolution APIs were tested.
- All five frontend navigation sections were tested.
- Analysis version selection and issue category selection were tested.
- Analysis search and issue details were tested.
- CSV report export was tested.
- Uploaded single-module and multi-module Maven projects were tested.
- Project build, metadata preparation, and fresh analysis workflows were tested.
- Invalid Maven projects and projects without valid Java source structures were tested for appropriate failure handling.
- Project switching between uploaded projects and Apache Ant was tested.

### 7.3 Failure Handling Verification
- A controlled invalid Maven ZIP upload failed at the BUILD stage as expected.
- The backend persisted the failure status, stage, and message.
- The Projects API returned the saved failure details.
- The frontend displayed the failure stage and message after a browser refresh.
- Three ProjectController tests passed.
- Two ProjectUploadController tests passed.
- The complete backend test suite passed with all 15 tests successful.

### 7.4 Analysis Result Verification
A non-zero architecture test project was checked against the original AsTdEA CSV output.

The displayed class cycles, package cycles, unstable dependencies, empty hub dependency results, project metrics, and CSV report values were verified against the original engine output.

## 8. Current Implementation Scope and Limitations

The current implementation integrates the existing AsTdEA analysis engine into a web application and supports uploaded Maven-based Java projects.

It provides visualization and exploration of existing measurements and supports fresh analysis of uploaded projects.

It does not yet introduce new architectural smell detection algorithms, evidence-based technical debt prioritization methods, or experimentally validated research contributions.

The current metadata preparation and analysis integration also have implementation-specific constraints that must be documented and evaluated before claiming complete compatibility with every Java project.

These research extensions will be considered as a separate development phase after application validation and documentation are completed.

## 9. Conclusion

The ArchiSense application now supports both prepared Apache Ant baseline exploration and uploaded Maven-based Java project analysis.

The frontend, backend, project processing workflow, AsTdEA integration, failure handling, and reporting functionality have undergone recorded build and functional checks.

The latest backend test suite passed all 15 tests, and the frontend production build completed successfully. The application is progressing through its validation phase before research contributions are introduced.