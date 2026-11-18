# Dataset Preparation and Validation

## 1. Dataset Overview

The ArchiSense baseline dataset uses three Apache Ant software versions:

- `ant-1.9.4`
- `ant-1.9.5`
- `ant-1.9.6`

The dataset is organized into shared metadata and version-specific directories.

```text
datasets/
└── ant/
    ├── stats/
    │   ├── dates.csv
    │   ├── versions.csv
    │   └── locShared.csv
    └── versions/
        ├── ant-1.9.4/
        │   ├── bin/
        │   └── stats/
        │       └── LocPerClass.csv
        ├── ant-1.9.5/
        │   ├── bin/
        │   └── stats/
        │       └── LocPerClass.csv
        └── ant-1.9.6/
            ├── bin/
            └── stats/
                └── LocPerClass.csv
```

## 2. Shared Dataset Metadata

The `datasets/ant/stats/` directory contains three metadata files.

### 2.1 versions.csv

Records the software versions included in the dataset:

- ant-1.9.4
- ant-1.9.5
- ant-1.9.6

### 2.2 dates.csv

Records the corresponding release dates:

| Version | Date |
|---|---|
| ant-1.9.4 | 2014-05-05 |
| ant-1.9.5 | 2015-06-03 |
| ant-1.9.6 | 2015-07-02 |

### 2.3 locShared.csv

Records the shared LOC values corresponding positionally to the versions:

| Version | LOC |
|---|---:|
| ant-1.9.4 | 107044 |
| ant-1.9.5 | 107657 |
| ant-1.9.6 | 107658 |

## 3. Version-Specific Dataset Structure

Each version directory contains two subdirectories:

- `bin/`: compiled distribution files.
- `stats/`: source-level class LOC inventory.

All three versions contain 22 JAR files in their respective `bin/` directories, including `ant.jar`.

Each version's `stats/` directory contains `LocPerClass.csv`.

## 4. Source-Level LOC Inventory

The `LocPerClass.csv` files record source-level class information.

The inspected CSV contains these columns:

- `FilePath`
- `fullyQualifiedName`
- `loc`
- `UnnamedPackage`

The verified record counts are:

| Version | LOC inventory records |
|---|---:|
| ant-1.9.4 | 865 |
| ant-1.9.5 | 867 |
| ant-1.9.6 | 867 |

These records represent the source inventory used for class-level LOC metadata.

## 5. Source-to-Binary Class Coverage

The Java source inventory was compared with compiled classes inside the Ant distribution JAR files.

For Ant 1.9.4:

- Source class records: 865
- Matched top-level class names: 813
- Unmatched source class names: 52

Three representative unmatched classes were checked in the complete build directory:

- FTP
- SSHExec
- Scale

All three were absent from the build output. These classes belong to optional Ant integrations.

The unmatched records have been retained in `LocPerClass.csv` because the CSV represents the source inventory, not only the compiled binary inventory.

Source-level LOC and compiled-class coverage are treated as separate measurements.

## 6. Dataset Validation

The following structural checks were performed:

- Confirmed the presence of all three Ant version directories.
- Confirmed that each version contains `bin/` and `stats/`.
- Confirmed that all three versions contain `LocPerClass.csv`.
- Counted the source inventory records for each version.
- Confirmed that each version contains 22 JAR files in `bin/`.
- Confirmed the presence of `ant.jar` in each version.
- Inspected the shared version, date, and LOC metadata files.
- Compared source inventory records with compiled class names for Ant 1.9.4.

## 7. Preparation Summary

The prepared Apache Ant dataset provides three software versions, shared version metadata, per-version source-level LOC inventories, and compiled distribution JAR files.

This dataset was used for the AsTdEA baseline reproduction and fresh multi-version analysis documented in `baseline-reproduction.md`.

The source inventory and compiled binary coverage are maintained as distinct measurements so that unmatched source records are not silently discarded.