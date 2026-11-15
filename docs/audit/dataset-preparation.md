\## Source-to-Binary Class Coverage



The Java source inventory was compared with compiled classes

inside the Ant distribution JAR files.



For Ant 1.9.4:

\- Source class records: 865

\- Matched top-level class names: 813

\- Unmatched source class names: 52



Three representative unmatched classes were checked in the

complete build directory:

\- FTP

\- SSHExec

\- Scale



All three were absent from the build output. These classes

belong to optional Ant integrations.



The unmatched records have been retained in LocPerClass.csv

because the CSV represents the source inventory, not only

the compiled binary inventory.



Source-level LOC and compiled-class coverage are treated

as separate measurements.

