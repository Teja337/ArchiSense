# AsTdEA Source Code Audit

## 1. Project Overview

**Project Name:** Architecture Smell and Technical Debt Evolution Analyzer (AsTdEA)

**Original Repository:** https://github.com/PhilippGnoyke/AsTdEA

**Implementation Language:** Java

**Build Tool:** Apache Maven

**Main Entry Point:** `org.astdea.TerminalExecutor`

### Purpose

AsTdEA is a research tool designed to detect architectural smells in Java software systems and analyze how those smells evolve across multiple software versions.

It uses a modified version of Arcan for architectural smell detection. It then tracks smell relationships between versions and calculates technical-debt-related metrics.

### Source Code Inventory

- Total Java source files: 96
- Main packages: `data`, `io`, `logic`, and `utils`
- Main application entry point: `TerminalExecutor.java`

### Audit Objective

The purpose of this audit is to understand the original system's architecture, implementation, analysis methods, input requirements, output structure, dependencies, and limitations before making any modifications for ArchiSense.

## 2. System Architecture and Execution Flow

AsTdEA follows a pipeline-based architecture. It reads software-version data, detects architectural smells, tracks their evolution, calculates metrics, and generates CSV reports.

### 2.1 Main Execution Flow

The main execution starts from `TerminalExecutor.java`.

The processing sequence is:

1. **Application Entry:** `TerminalExecutor` reads command-line arguments and starts the analysis.
2. **Version Discovery:** `ArcanRunner` identifies the available software versions.
3. **Architectural Smell Detection:** `ArcanVersionRunner` invokes the modified Arcan tool to analyze individual versions.
4. **Version Initialization:** `Version` and `VersionSmellsInitialiser` load smell information and project metrics.
5. **Smell Tracking:** Project trackers compare smells between consecutive versions.
6. **Evolution Analysis:** Mapping and inter-version building classes establish relationships between smell instances.
7. **Delta Calculation:** The system calculates smell introductions and removals.
8. **Project Analysis:** `Project` coordinates the analysis and calculates project-level metrics.
9. **Output Generation:** `MainPrinter` and its subprinters generate CSV reports.

### 2.2 Main Package Responsibilities

| Package | Responsibility |
|---|---|
| `data` | Represents projects, versions, components, and architectural smells. |
| `io` | Handles input reading, Arcan execution, and output generation. |
| `logic` | Implements smell tracking, mapping, evolution analysis, and delta calculations. |
| `utils` | Provides mathematical, collection, component, and text-processing utilities. |

### 2.3 Important Architectural Observation

The original AsTdEA implementation separates data representation, input/output operations, and analysis logic.

The modified Arcan library is an important external dependency because AsTdEA relies on it for architectural smell detection and related analysis output.

This separation helps us understand which existing components should be preserved when developing ArchiSense.

## 3. Input Data and Processing Requirements

AsTdEA requires software-version information and architectural analysis data to study the evolution of architectural smells.

### 3.1 Main Input Data

| Input | Purpose |
|---|---|
| Version JAR files or folders | Contains the compiled software versions to analyze. |
| `dates.csv` | Stores release dates for software versions. |
| `loc.csv` | Stores lines-of-code information for software versions in the old input structure. |
| `locShared.csv` | Stores shared LOC information in the new input structure. |
| `versions.csv` | Optionally provides version identifiers and ordering information. |
| `SharedClasses.csv` | Optionally identifies shared classes between versions. |
| Component change CSV files | Provide class-level or package-level change information. |

### 3.2 Input Processing Classes

- `ArcanRunner.java`: Discovers versions and coordinates their analysis.
- `ArcanVersionRunner.java`: Runs the Arcan analysis for individual versions.
- `VersionsReader.java`: Reads version information.
- `DatesReader.java`: Reads release dates.
- `LocReader.java`: Reads lines-of-code information.
- `CsvReadingUtils.java`: Provides common CSV-reading operations.
- `SharedClassesParser.java`: Reads shared-class information.

### 3.3 Processing Requirements

1. Software versions must be available in the expected directory structure.
2. Version names and ordering must be consistent.
3. Release dates and LOC information must correspond to the correct versions.
4. CSV files must contain the expected headers and valid values.
5. Component-change information must follow the expected input format when supplied.

### 3.4 Observation

The analysis depends on correctly aligned version, date, LOC, and architectural data. Incorrect or incomplete input information may affect the accuracy of the resulting evolution metrics.

The system supports different input structures, so the input format must be identified before running an experiment.

## 4. Architectural Smell Detection and Evolution Tracking

AsTdEA analyzes architectural smells in individual software versions and tracks how those smells change across releases.

### 4.1 Architectural Smell Categories

The implementation handles three main architectural smell categories:

1. **Cyclic Dependency (CD):** Represents cyclic dependencies among software components.
2. **Hub-like Dependency (HD):** Represents components with significant incoming and outgoing dependencies.
3. **Unstable Dependency (UD):** Represents dependency relationships involving less stable packages.

The system also analyzes these smells at different architectural levels, including class and package levels where applicable.

### 4.2 Intra-Version Analysis

Intra-version analysis represents architectural smells detected within a single software version.

Important implementation classes include:

- `VersionSmellsInitialiser.java`
- `CdInitHelper.java`
- `HdInitHelper.java`
- `UdInitHelper.java`
- `IntraVersionCd.java`
- `IntraVersionHd.java`
- `IntraVersionUd.java`

These classes initialize smell instances and store their associated properties and components.

### 4.3 Inter-Version Analysis

Inter-version analysis studies relationships between smell instances across consecutive software versions.

Important classes include:

- `SmellProjectTracker.java`
- `CdVersionPairTracker.java`
- `HdVersionPairTracker.java`
- `UdVersionPairTracker.java`
- `CdMappings.java`
- `LinEvoTypeMappings.java`
- `InterVersionCdBuilder.java`

The tracking process identifies relationships between smells in different versions. These relationships help determine whether a smell continues, changes, merges with other smells, splits, or disappears.

### 4.4 Evolution Metrics

AsTdEA calculates information about smell evolution, including:

- Smell introductions and removals.
- Smell lifetime and age.
- Relationships between smell instances.
- Merge and split events.
- Transitions between versions.
- Component changes affecting architectural smells.

### 4.5 Observation

AsTdEA provides a foundation for studying architectural-smell evolution over multiple software releases. Its main focus is detecting, tracking, and measuring architectural smells and their evolution.

## 5. Output Reports and Metrics

After completing the analysis, AsTdEA generates CSV-based reports containing architectural-smell information, evolution relationships, and project metrics.

### 5.1 Output Structure

The generated results are organized into two main categories:

1. **intraVersion:** Contains information about architectural smells detected within individual software versions.
2. **interVersion:** Contains information about relationships and evolution between software versions.

### 5.2 Intra-Version Outputs

These outputs provide information about individual software versions, including:

- Architectural smell properties.
- Components associated with smells.
- Class and package dependency information.
- Version-specific metrics.
- Affected component information.

### 5.3 Inter-Version Outputs

These outputs describe how architectural smells evolve across releases, including:

- Inter-version smell properties.
- Smell transitions.
- Merge and split relationships.
- Component-change effects.
- Incoming and outgoing affected-component information.
- Evolution-related metrics.

### 5.4 Important Output Classes

| Class | Responsibility |
|---|---|
| `MainPrinter.java` | Coordinates report generation. |
| `ProjectMetricsPrinter.java` | Writes project-level metrics. |
| `VersionMetricsPrinter.java` | Writes version-level metrics. |
| `PropsPrinter.java` | Prints smell properties. |
| `CompsPrinter.java` | Prints components associated with smells. |
| `CdTransitionsPrinter.java` | Prints cyclic-dependency transitions. |
| `CdMergesSplitsPrinter.java` | Prints merge and split information. |
| `VersionNamesPrinter.java` | Writes version names. |

### 5.5 Observation

The output structure separates information about individual versions from information about evolution across versions.

These reports provide the data needed to inspect the results and conduct further empirical analysis.

## 6. Dependencies and External Libraries

AsTdEA uses Maven to manage its Java dependencies.

### 6.1 Main Dependencies

| Dependency | Purpose |
|---|---|
| Apache Commons IO | File and directory operations. |
| JCommander | Command-line argument processing. |
| Apache Commons CSV | Reading and writing CSV files. |
| Apache Commons Math | Mathematical and statistical operations. |
| Apache Commons Lang | General-purpose Java utilities. |
| Modified Arcan 1.2.1 | Architectural smell detection and analysis. |
| JUnit 4 | Unit testing. |
| PSJava | Graph-related and algorithmic utilities. |

### 6.2 Important Dependency: Modified Arcan

AsTdEA depends on a locally included modified Arcan JAR:

`libs/arcan-1.2.1-modded.jar`

The Maven configuration declares this dependency using system scope.

The application invokes Arcan through `ArcanRunner.java` and `ArcanVersionRunner.java`.

Several input/output constants are also obtained from Arcan classes, creating a direct connection between the two implementations.

### 6.3 Build Configuration

- Build tool: Apache Maven.
- Java source version configured in `pom.xml`: 14.
- Java target version configured in `pom.xml`: 14.
- Testing framework: JUnit 4.13.2.

### 6.4 Observation

The modified Arcan library is a critical dependency of the original system. Its availability and compatibility must be maintained when building and extending AsTdEA.

The locally included JAR also needs separate dependency and licence inspection before redistribution.

## 7. Testing and Baseline Reproduction

The original AsTdEA source code was tested in a separate local copy to avoid modifying the preserved original repository.

### 7.1 Testing Environment

| Configuration | Version |
|---|---|
| Operating System | Windows 11 |
| Java | 27 |
| Maven | 3.10.0 |
| Build Tool | Apache Maven |
| Testing Framework | JUnit 4.13.2 |

### 7.2 Test Execution

The following command was used:

`mvn test`

Initially, two tests encountered errors because the expected `resources` directory was missing in the test copy.

After creating the missing directory, the test command was executed again.

### 7.3 Final Test Results

| Test Result | Count |
|---|---:|
| Tests Run | 12 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

**Build Result:** BUILD SUCCESS

### 7.4 Reproduction Notes

- Testing was performed in a separate copy of AsTdEA.
- The original Java source files were not modified to resolve the test setup issue.
- The missing `resources` directory was created only in the test copy.
- The successful test result confirms that the existing test suite passed in this environment.

### 7.5 Limitation

Passing unit tests does not establish that the complete application has successfully processed a real software project from input to output.

A separate end-to-end execution is still required to verify the complete application workflow.

## 8. Potential Limitations and Areas for Verification

During source-code inspection, several areas were identified that require further verification through testing and experimentation.

### 8.1 Input Validation

Some input-processing classes assume that CSV files contain the expected headers, valid values, and correctly aligned records.

Further testing is needed to understand how the application handles missing, incomplete, or incorrectly formatted input data.

### 8.2 Edge Cases in Metric Calculations

Some mathematical operations involve division, similarity calculations, and statistical measurements.

Cases such as zero values, empty collections, and projects with very few versions should be tested to verify that the results remain meaningful.

### 8.3 Version and Component Tracking

The tracking process depends on relationships between components and smell instances across software versions.

Further verification is needed for complex cases involving renamed components, multiple predecessors or successors, merges, and splits.

### 8.4 Resource Management

Some input/output operations could benefit from stronger resource-management practices, such as consistent use of try-with-resources.

The parallel execution and memory requirements of large software projects also need further evaluation.

### 8.5 Output Reliability

The generated CSV reports should be checked for correct values, consistent version ordering, and reproducibility across repeated executions.

### 8.6 Summary of Verification Needs

The following areas require additional testing:

- Missing or malformed input files.
- Empty and single-version projects.
- Zero-value metric calculations.
- Complex component changes.
- Large projects and memory consumption.
- Parallel execution behaviour.
- CSV output consistency and reproducibility.

### 8.7 Observation

These points are preliminary audit observations, not confirmed software defects. They will be investigated through controlled experiments before any changes are proposed for ArchiSense.

## 9. Source-Code Audit Summary

The source-code inspection provides an understanding of the original AsTdEA implementation and its major components.

### 9.1 Key Findings

1. AsTdEA is a Java-based research tool for architectural-smell detection and evolution analysis.
2. It uses a modified Arcan library for architectural-smell detection.
3. It supports analysis across multiple software versions.
4. It tracks Cyclic Dependency, Hub-like Dependency, and Unstable Dependency smells.
5. It calculates smell evolution metrics, including introductions, removals, transitions, merges, and splits.
6. It generates CSV reports containing intra-version and inter-version analysis results.
7. Its implementation separates data models, input/output operations, analysis logic, and utility functions.

### 9.2 Current Verification Status

- Original Java source inventory recorded: 96 files.
- Maven test suite: 12 tests passed.
- Test failures and errors: 0 after preparing the test environment.
- Original source preserved separately from the test copy.
- Complete end-to-end application execution: Pending.
- Potential limitations: Identified for further verification.

### 9.3 Conclusion

The audit establishes a preliminary technical understanding of AsTdEA and provides a foundation for the development of ArchiSense.

Before implementing improvements, the original application must be executed with a suitable software-version dataset. Its generated outputs must then be examined to establish a reproducible baseline.

Research gaps and proposed enhancements will be investigated only after this baseline verification is completed.

