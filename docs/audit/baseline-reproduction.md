# AsTdEA Baseline Reproduction

## 1. Original Maven Test Verification

- **OS:** Windows 11
- **Java:** 27
- **Maven:** 3.10.0
- **Command:** `mvn test`
- **Tests run:** 12
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Result:** BUILD SUCCESS

The initial two test errors were resolved by creating the missing `resources` directory in the separate test copy. No original Java source files were modified.

## 2. Fresh AsTdEA Engine Execution

The original AsTdEA engine was executed again using the prepared Apache Ant dataset.

- **Dataset:** Apache Ant
- **Versions analysed:** ant-1.9.4, ant-1.9.5, ant-1.9.6
- **Execution mode:** Fresh analysis using Arcan
- **Input directory:** `datasets`
- **Output directory:** Temporary runtime output
- **Execution method:** Java classpath execution with the original AsTdEA JAR, modified Arcan JAR, and required library dependencies.
- **Result:** Successful execution

The engine completed analysis for all three versions and generated intra-version and inter-version metric files.

## 3. Baseline Metric Comparison

The freshly generated metrics were compared against the previously reproduced baseline results.

| Metric | Fresh execution | Original baseline |
|---|---:|---:|
| Analysed versions | 3 | 3 |
| Analysis time span | 634 days | 634 days |
| Intra-version architecture smells | 218 | 218 |
| Intra-version class cycles | 164 | 164 |
| Intra-version package cycles | 6 | 6 |
| Intra-version hub dependencies | 12 | 12 |
| Intra-version unstable dependencies | 36 | 36 |
| Inter-version architecture smells | 73 | 73 |
| Inter-version class cycles | 55 | 55 |
| Inter-version package cycles | 2 | 2 |
| Inter-version hub dependencies | 4 | 4 |
| Inter-version unstable dependencies | 12 | 12 |

All compared intra-version and inter-version metric fields matched the original baseline values.

## 4. Verification Conclusion

The fresh execution successfully reproduced the original AsTdEA baseline metrics for all three Apache Ant versions.

This confirms that the prepared dataset, engine, and runtime dependencies can be used to perform a fresh analysis and reproduce the existing baseline results.

The original AsTdEA Java source files and existing baseline result files were not modified during this verification.
