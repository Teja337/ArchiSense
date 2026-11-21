# ArchiSense — Final Application Validation Report

## 1. Purpose

This document records the validation performed on the ArchiSense application before beginning research-gap development.

The validation focused on application stability, integration with the original AsTdEA engine, correctness of displayed measurements, project upload and analysis workflows, reporting, and repository consistency.

## 2. Environment

- Operating System: Windows 11
- Java: JDK 27
- Maven: 3.10.0
- Node.js: 26.13.0
- Frontend: React with Vite
- Backend: Spring Boot
- Analysis Engine: Original AsTdEA implementation

## 3. Automated Validation

### Backend Tests

- Tests run: 15
- Failures: 0
- Errors: 0
- Skipped: 0
- Result: BUILD SUCCESS

### Frontend Lint

- Errors: 0
- Warnings: 3
- Result: Completed successfully

The three warnings concern React state updates in `Evolution.jsx` and `Analysis.jsx`. They were recorded without making unrelated code changes.

### Frontend Production Build

- Build tool: Vite 8.3.7
- Modules transformed: 22
- Result: Successful production build

## 4. Apache Ant Baseline Validation

The original AsTdEA analysis was checked against the selected Apache Ant versions.

| Metric | Ant 1.9.4 | Ant 1.9.5 | Ant 1.9.6 |
|---|---:|---:|---:|
| Architecture smells | 72 | 73 | 73 |
| Technical debt | 42.64502345664507 | 12.416561205092956 | 12.416561205092956 |
| Class cycles | 54 | 55 | 55 |
| Package cycles | 2 | 2 | 2 |
| Hub dependencies | 4 | 4 | 4 |
| Unstable dependencies | 12 | 12 | 12 |
| Lines of code | 107044 | 107657 | 107658 |
| Total classes | 1521 | 1534 | 1534 |
| Total packages | 169 | 170 | 170 |

The Evolution measurements were also checked:

- Analyzed versions: 3
- Analysis time span: 634 days
- Intra-version smells: 218
- Inter-version smells: 73
- Intra-version class cycles: 164
- Intra-version package cycles: 6
- Intra-version hub dependencies: 12
- Intra-version unstable dependencies: 36
- Inter-version class cycles: 55
- Inter-version package cycles: 2
- Inter-version hub dependencies: 4
- Inter-version unstable dependencies: 12

The Reports page and exported CSV were checked against these measurements. The CSV version labels were subsequently corrected to display the actual Apache Ant release names. All original baseline measurements remained unchanged.

## 5. Uploaded Project Validation

A Java Maven test project was uploaded and analyzed successfully.

- Project status: `ANALYSIS_COMPLETED`
- Available versions: 1
- Architecture smells: 4
- Technical debt: 0.5037603630185343
- Class cycles: 1
- Package cycles: 1
- Hub dependencies: 0
- Unstable dependencies: 2
- Lines of code: 136
- Total classes: 9
- Total packages: 9

The issue records and detailed properties were checked against the original AsTdEA output files.

The Analysis page correctly displayed all four categories, including the empty hub-dependency state.

The exported CSV matched the original measurements.

## 6. Upload and Failure Handling

The upload workflow was checked using a fresh Java Maven project.

The following stages completed successfully:

1. Project upload
2. Maven build
3. Metadata preparation
4. AsTdEA analysis
5. Results registration

An invalid Maven project was also checked.

- Status: `FAILED`
- Failure stage: `BUILD`
- Error message: Maven build failed
- Build-log location: Displayed in the Projects page

Older failed projects without persisted failure information display an appropriate unavailable-details message.

## 7. Application Integration

The following application areas were checked:

- Projects page and project registration
- Overview dashboard
- Project and version selection
- Analysis results and issue details
- Evolution measurements
- Reports page
- CSV export
- Backend and frontend integration
- AsTdEA engine availability

## 8. Validation Follow-up and Resolved Observations

The following observations identified during the initial validation were subsequently addressed and verified:

1. **Apache Ant CSV version labels:** Updated `ResultsController` to display the actual release names: `Ant 1.9.4`, `Ant 1.9.5`, and `Ant 1.9.6`. Exported CSV output was verified, and all original baseline measurements remained unchanged.
2. **Single-version Evolution explanation:** Visually confirmed that an uploaded project with one analyzed version displays an explanatory message instead of fabricated version-to-version trends.

The backend test suite was rerun after the CSV label correction:

- Tests run: 15
- Failures: 0
- Errors: 0
- Skipped: 0
- Result: BUILD SUCCESS

Both observations are resolved. No outstanding observations from this initial validation remain.

## 9. Repository Status

At the time of this report update:

- Branch: `main`
- Latest application correction commit: `6ba5aff`
- Commit message: `Use actual Ant release labels in results`
- Application correction pushed successfully to `origin/main`
- Working tree was clean after the application correction commit
- This report update is pending its own commit and push

## 10. Conclusion

The major automated tests, Apache Ant baseline measurements, uploaded-project analysis, issue details, reporting, CSV exports, and failure-handling workflows were successfully validated.

The two minor observations identified during initial validation were subsequently resolved and verified. The backend test suite also passed all 15 tests.

This report provides a documented validation baseline for ArchiSense. Research-gap implementation and experimental evaluation can proceed as a separate phase, while preserving the verified application and original AsTdEA baseline.