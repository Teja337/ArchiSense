# ArchiSense — Application Implementation Documentation

## 1. Project Overview

ArchiSense is a web-based application for exploring and understanding architectural technical debt in Java software systems.

The application currently uses the existing AsTdEA research implementation and its prepared analysis results for Apache Ant versions 1.9.4, 1.9.5, and 1.9.6.

The purpose of this implementation phase is to provide an accessible interface for examining architectural measurements, issue details, and evolution across software versions.

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

### Analysis Engine and Data
- Existing AsTdEA implementation
- Prepared Apache Ant datasets
- Original AsTdEA CSV analysis results

## 3. Application Architecture

The application follows a frontend-backend architecture.

1. React provides the user interface.
2. The frontend sends HTTP requests to the Spring Boot backend.
3. The backend reads prepared datasets and CSV analysis results.
4. The backend converts the results into JSON responses.
5. React displays the returned information.

The original AsTdEA source code and analysis algorithms have not been modified during this application implementation phase.

## 4. Implemented Application Modules

### 4.1 Overview
Displays project information, available versions, and summary measurements.

### 4.2 Projects
Displays the available software project and its analyzed versions.

### 4.3 Analysis
Allows users to explore four architecture issue categories:
- Class cycles
- Package cycles
- Hub dependencies
- Unstable dependencies

Provides version selection, search, issue counts, and expandable issue properties.

### 4.4 Evolution
Displays inter-version measurements obtained from the original AsTdEA results.

### 4.5 Reports
Displays an overall analysis summary, version-wise comparisons, and architecture issue distribution. Supports CSV export of the displayed version-wise measurements.

## 5. Backend API Endpoints

| HTTP Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/projects | Retrieve available projects |
| GET | /api/projects/ant/versions | Retrieve software versions |
| GET | /api/projects/ant/results | Retrieve version-wise results |
| GET | /api/projects/ant/evolution | Retrieve evolution metrics |
| GET | /api/projects/ant/issues/class-cycles/{versionIndex} | Retrieve class cycle issues |
| GET | /api/projects/ant/issues/package-cycles/{versionIndex} | Retrieve package cycle issues |
| GET | /api/projects/ant/issues/hub-dependencies/{versionIndex} | Retrieve hub dependency issues |
| GET | /api/projects/ant/issues/unstable-dependencies/{versionIndex} | Retrieve unstable dependency issues |

## 6. Dataset

The application uses Apache Ant versions:
- 1.9.4
- 1.9.5
- 1.9.6

Prepared datasets are stored under `datasets/ant`, and original analysis results are stored under `results/ant`.

The application reads these prepared results. It does not execute a new AsTdEA analysis when a user opens the dashboard.

## 7. Verification

The following checks have been completed:

- Backend Maven packaging completed successfully.
- Frontend production build completed successfully.
- Production preview opened successfully.
- Project, version, result, and evolution APIs returned JSON.
- Class cycle issue API was verified.
- All five frontend navigation sections were tested.
- Analysis version selection and issue category selection were tested.
- Analysis search and issue details were tested.
- CSV report export was tested.
- Temporary CSV test controller was removed.

## 8. Current Implementation Scope

The current implementation focuses on integrating the existing AsTdEA analysis results into a web application.

It provides visualization and exploration of existing measurements. It does not yet introduce new architectural smell detection algorithms, technical debt prioritization methods, or experimentally validated research contributions.

These research extensions will be considered as a separate development phase.

## 9. Conclusion

The current ArchiSense application provides a working interface for exploring architectural technical debt measurements and their evolution in Apache Ant. The frontend, backend, prepared datasets, and existing analysis results are integrated and have passed the recorded build and functional checks.