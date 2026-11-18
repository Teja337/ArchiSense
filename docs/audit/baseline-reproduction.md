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

The initial two test errors were resolved by creating the missing `resources` directory in the separate test copy. No original Java source files were modified to resolve these initial test errors.

## 2. Fresh AsTdEA Engine Execution

The AsTdEA engine was executed using the prepared Apache Ant dataset.

- **Dataset:** Apache Ant
- **Versions analysed:** ant-1.9.4, ant-1.9.5, ant-1.9.6
- **Execution mode:** Fresh analysis using Arcan
- **Input directory:** `datasets`
- **Output directory:** Temporary runtime output
- **Execution method:** Java classpath execution with the AsTdEA JAR, rebuilt Arcan JAR, and required library dependencies.
- **Result:** Successful execution

The engine completed analysis for all three versions and generated intra-version and inter-version metric files.

## 3. Arcan Runtime Correction

During fresh analysis, a null-handling issue was encountered in the bundled Arcan implementation.

A minimal null guard was added to:

`runtime/arcan-source/src/main/it/unimib/disco/essere/main/asengine/udutils/UDUtils.java`

The correction handles a null `badDependencies` collection before iteration. It does not introduce a new architecture-smell detection algorithm or change the intended AsTdEA research methodology.

The modified Arcan source was rebuilt successfully, and the resulting JAR was used for fresh analysis.

The original Arcan JAR was preserved as:

`runtime/arcan-source/arcan-1.2.1-modded-original.jar`

The rebuilt JAR is:

`original/AsTdEA/libs/arcan-1.2.1-modded.jar`

This runtime correction is documented to distinguish the verified execution environment from an entirely unmodified original distribution.

## 4. Baseline Metric Comparison

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

## 5. Verification Conclusion

The fresh execution successfully reproduced the original AsTdEA baseline metrics for all three Apache Ant versions.

This confirms that the prepared dataset, AsTdEA engine, rebuilt Arcan dependency, and runtime dependencies can be used to perform a fresh analysis and reproduce the existing baseline results.

The AsTdEA analysis methodology was not intentionally changed as part of this verification. A minimal null-handling correction was made in the bundled Arcan source to allow fresh analysis to complete. The original Arcan JAR was preserved separately.

The existing baseline result files were not modified during this verification.