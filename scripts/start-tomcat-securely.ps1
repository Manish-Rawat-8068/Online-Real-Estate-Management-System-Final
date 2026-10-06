$ErrorActionPreference = 'Stop'

$tomcatHome = 'C:\Users\Manish Rawat\Downloads\apache-tomcat-10.1.60-windows-x64\apache-tomcat-10.1.60'
$javaHome = 'C:\Program Files\Java\jdk-26.0.2'
$mysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$projectRoot = Split-Path -Parent $PSScriptRoot
$warFile = Join-Path $projectRoot 'target\real-estate-management.war'
$catalina = Join-Path $tomcatHome 'bin\catalina.bat'
$secretPtr = [IntPtr]::Zero
$plainPassword = $null
$serverXml = Join-Path $tomcatHome 'conf\server.xml'
$configuredPassword = [Environment]::GetEnvironmentVariable('REAL_ESTATE_DB_PASSWORD', 'Process')
if ([string]::IsNullOrWhiteSpace($configuredPassword)) { $configuredPassword = [Environment]::GetEnvironmentVariable('REAL_ESTATE_DB_PASSWORD', 'User') }
if ([string]::IsNullOrWhiteSpace($configuredPassword)) { $configuredPassword = [Environment]::GetEnvironmentVariable('REAL_ESTATE_DB_PASSWORD', 'Machine') }

try {
    if ([string]::IsNullOrWhiteSpace($configuredPassword)) {
        $secret = Read-Host 'Enter the MySQL root password (input is hidden; it will not be saved)' -AsSecureString
        $secretPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPtr)
    } else {
        $plainPassword = $configuredPassword
    }
    $env:REAL_ESTATE_DB_PASSWORD = $plainPassword
    $env:MYSQL_PWD = $plainPassword
    $env:REAL_ESTATE_DB_USER = 'root'
    $env:REAL_ESTATE_DB_URL = 'jdbc:mysql://localhost:3306/real_estate_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000'
    $env:JAVA_HOME = $javaHome
    $env:CATALINA_HOME = $tomcatHome
    $env:CATALINA_BASE = $tomcatHome
    if (-not (Test-Path -LiteralPath $serverXml)) { throw 'Tomcat server.xml was not found.' }
    $serverConfiguration = [IO.File]::ReadAllText($serverXml)
    $connectorPattern = '(<Connector\b(?=[^>]*\bport="8080")[^>]*?\bprotocol=")([^"]+)(")'
    $commentSpans = [regex]::Matches($serverConfiguration, '(?s)<!--.*?-->')
    $activeConnectors = @([regex]::Matches($serverConfiguration, $connectorPattern) | Where-Object {
        $position = $_.Index
        -not ($commentSpans | Where-Object { $position -ge $_.Index -and $position -lt ($_.Index + $_.Length) })
    })
    if ($activeConnectors.Count -ne 1) { throw 'Expected exactly one active Tomcat HTTP connector on port 8080.' }
    if ($activeConnectors[0].Groups[2].Value -ne 'org.apache.coyote.http11.Http11Nio2Protocol') {
        if ($activeConnectors[0].Groups[2].Value -ne 'HTTP/1.1') { throw 'The active Tomcat 8080 connector uses an unexpected protocol.' }
        $configurationBackup = "$serverXml.codex-backup"
        if (-not (Test-Path -LiteralPath $configurationBackup)) { Copy-Item -LiteralPath $serverXml -Destination $configurationBackup }
        $serverConfiguration = [regex]::Replace($serverConfiguration, $connectorPattern, '${1}org.apache.coyote.http11.Http11Nio2Protocol${3}', 1)
        [IO.File]::WriteAllText($serverXml, $serverConfiguration, [Text.UTF8Encoding]::new($false))
    }
    # Java 26's Windows NIO selector creates a loopback pipe under java.io.tmpdir.
    # The packaged desktop launch context redirects TEMP to a long sandbox path,
    # which exceeds Windows' Unix-domain socket path limit and breaks Tomcat NIO.
    $tomcatRuntimeTemp = Join-Path $env:USERPROFILE 'AppData\Local\Temp\RealEstateTomcat'
    New-Item -ItemType Directory -Path $tomcatRuntimeTemp -Force | Out-Null
    $env:CATALINA_TMPDIR = $tomcatRuntimeTemp

    if (-not (Test-Path -LiteralPath $mysqlExe)) { throw 'The MySQL command-line client was not found at the installed MySQL location.' }
    if (-not (Test-Path -LiteralPath $catalina)) { throw 'Tomcat 10.1 startup script was not found.' }
    if (-not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) { throw 'Java 26 was not found at the installed JDK location.' }
    if (-not (Test-Path -LiteralPath $warFile)) { throw 'Build the Maven WAR before starting this script.' }

    function Invoke-DatabaseQuery([string]$query) {
        $output = & $mysqlExe --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --database=real_estate_db --batch --skip-column-names --execute=$query 2>$null
        if ($LASTEXITCODE -ne 0) { throw 'MySQL setup failed. Verify the entered password and that real_estate_db is available.' }
        return $output
    }

    $hasActive = [int](Invoke-DatabaseQuery "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='is_active';")
    if ($hasActive -eq 0) { Invoke-DatabaseQuery 'ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;' | Out-Null }

    $hasAgreementIndex = [int](Invoke-DatabaseQuery "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='rental_agreements' AND column_name='application_id' AND non_unique=0;")
    if ($hasAgreementIndex -eq 0) {
        $duplicates = Invoke-DatabaseQuery 'SELECT COUNT(*) FROM (SELECT application_id FROM rental_agreements GROUP BY application_id HAVING COUNT(*)>1) d;'
        if ([int]$duplicates -gt 0) { throw 'The database has duplicate agreements for an application. No agreement migration was applied; preserve and resolve those records before retrying.' }
        Invoke-DatabaseQuery 'ALTER TABLE rental_agreements ADD CONSTRAINT uq_rental_agreements_application UNIQUE (application_id);' | Out-Null
    }

    $hasPropertyRent = [int](Invoke-DatabaseQuery "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='properties' AND column_name='rent';")
    if ($hasPropertyRent -eq 0) {
        $legacyPropertyPrice = [int](Invoke-DatabaseQuery "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='properties' AND column_name='price';")
        if ($legacyPropertyPrice -eq 0) { throw 'The properties table has neither rent nor the legacy price column; no property data was changed.' }
        Invoke-DatabaseQuery 'ALTER TABLE properties ADD COLUMN rent DECIMAL(12,2) NULL;' | Out-Null
        Invoke-DatabaseQuery 'UPDATE properties SET rent=price WHERE rent IS NULL;' | Out-Null
        Invoke-DatabaseQuery 'ALTER TABLE properties MODIFY rent DECIMAL(12,2) NOT NULL;' | Out-Null
    }

    $hasAgreementRent = [int](Invoke-DatabaseQuery "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='rental_agreements' AND column_name='rent';")
    if ($hasAgreementRent -eq 0) { Invoke-DatabaseQuery 'ALTER TABLE rental_agreements ADD COLUMN rent DECIMAL(12,2) NULL;' | Out-Null }
    Invoke-DatabaseQuery 'UPDATE rental_agreements g JOIN rental_applications a ON a.id=g.application_id JOIN properties p ON p.id=a.property_id SET g.rent=p.rent WHERE g.rent IS NULL OR g.rent=0.00;' | Out-Null
    if ($hasAgreementRent -eq 0) { Invoke-DatabaseQuery 'ALTER TABLE rental_agreements MODIFY rent DECIMAL(12,2) NOT NULL;' | Out-Null }

    & $catalina stop | Out-Null
    $deadline = (Get-Date).AddSeconds(25)
    do {
        Start-Sleep -Milliseconds 500
        $listener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
    } while ($listener -and (Get-Date) -lt $deadline)
    if ($listener) { throw 'The old Tomcat listener did not stop; refusing to start a duplicate server.' }

    Copy-Item -LiteralPath $warFile -Destination (Join-Path $tomcatHome 'webapps\real-estate-management.war') -Force

    Write-Host 'Database schema checks passed. Starting Tomcat with Java 26; the password remains only in this process environment.'
    & $catalina run
}
finally {
    Remove-Item Env:REAL_ESTATE_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item Env:REAL_ESTATE_DB_USER -ErrorAction SilentlyContinue
    Remove-Item Env:REAL_ESTATE_DB_URL -ErrorAction SilentlyContinue
    $configuredPassword = $null
    $plainPassword = $null
    if ($secretPtr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPtr) }
    if ($secret) { $secret.Dispose() }
}
