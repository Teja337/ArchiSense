$root = "C:\Users\BHANU TEJA\Downloads\ArchiSense-Dataset\ant"
$jarTool = "$env:JAVA_HOME\bin\jar.exe"

foreach ($v in @("1.9.4", "1.9.5", "1.9.6")) {
    Write-Host "`n===== Ant $v ====="

    $versionDir = Join-Path $root "versions\ant-$v"
    $csvPath = Join-Path $versionDir "stats\LocPerClass.csv"
    $jarDir = Join-Path $versionDir "bin"

    $csvClasses = @(
        Import-Csv $csvPath |
        ForEach-Object { $_.fullyQualifiedName }
    )

    $jarClasses = @(
        Get-ChildItem $jarDir -Filter "*.jar" |
        ForEach-Object {
            & $jarTool tf $_.FullName
        } |
        Where-Object {
            $_ -match '\.class$' -and
            $_ -notmatch '\$' -and
            $_ -notmatch '^META-INF/'
        } |
        ForEach-Object {
            $_ -replace '\.class$', '' -replace '/', '.'
        } |
        Sort-Object -Unique
    )

    $csvSet = @{}
    foreach ($name in $csvClasses) {
        $csvSet[$name] = $true
    }

    $jarSet = @{}
    foreach ($name in $jarClasses) {
        $jarSet[$name] = $true
    }

    $matched = @(
        $csvSet.Keys | Where-Object { $jarSet.ContainsKey($_) }
    )

    $missing = @(
        $csvSet.Keys | Where-Object { !$jarSet.ContainsKey($_) }
    )

    Write-Host "CSV class names:" $csvSet.Count
    Write-Host "JAR top-level class names:" $jarSet.Count
    Write-Host "Matched names:" $matched.Count
    Write-Host "CSV classes not found in JARs:" $missing.Count

    if ($missing.Count -gt 0) {
        Write-Host "First missing names:"
        $missing | Select-Object -First 10
    }
}