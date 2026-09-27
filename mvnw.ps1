# PowerShell entry for Maven Wrapper.
# Fixes PowerShell splitting -Dfoo.bar=baz into "-Dfoo" + ".bar=baz"
# (so `.\mvnw spring-boot:run -Dspring-boot.run.profiles=local` works unquoted).

$ErrorActionPreference = 'Stop'
$fixed = [System.Collections.Generic.List[string]]::new()

for ($i = 0; $i -lt $args.Count; $i++) {
    $a = [string]$args[$i]
    $next = if ($i + 1 -lt $args.Count) { [string]$args[$i + 1] } else { $null }
    if ($a -match '^-D[\w-]+$' -and $next -match '^\.[\w.-]+=') {
        $fixed.Add($a + $next)
        $i++
    } else {
        $fixed.Add($a)
    }
}

$mvnwCmd = Join-Path $PSScriptRoot 'mvnw.cmd'
& $mvnwCmd @fixed
exit $LASTEXITCODE
