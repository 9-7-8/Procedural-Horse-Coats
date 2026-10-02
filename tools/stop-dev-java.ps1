<#
.SYNOPSIS
  Stop this repo's own dev Java processes (runClient, runServer, runGameTest) - and
  nothing else, ever.

.DESCRIPTION
  THE ONLY SANCTIONED WAY TO KILL A PROCESS FROM A CLAUDE SESSION IN THIS REPO.
  A PreToolUse hook (.claude/hooks/guard-process-kill.mjs) refuses every other
  kill command: Stop-Process, taskkill, kill, pkill and the rest.

  Why: on 2026-10-01 a session stopped a dev server with a command-line pattern
  ('nogui'), and it hard-killed the LIVE server with three players on it. The live
  server is C:\Users\Isabel\26.1.2-neoforge-server, its command line is
  "@user_jvm_args.txt ... nogui" with no path in it, and it restarts under new PIDs,
  so neither a pattern nor a remembered PID can be trusted to miss it.

  So this is an ALLOWLIST. A Java process is stopped only if its command line
  contains this repository's own path. Everything else is listed as "left alone"
  and never touched - including anything whose command line mentions the live
  server folder, as a second check. A dev process that does not match is left
  running and reported; ask the owner rather than widening this.

  Gradle daemons are not stopped here: use ./gradlew --stop.

.PARAMETER Id
  Stop only these PIDs - each must still pass the allowlist.

.PARAMETER WhatIf
  List what would be stopped, stop nothing.

.EXAMPLE
  pwsh -File tools/stop-dev-java.ps1 -WhatIf
  pwsh -File tools/stop-dev-java.ps1
  pwsh -File tools/stop-dev-java.ps1 -Id 12345
#>
param(
    [int[]] $Id,
    [switch] $WhatIf
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
# Both slash directions - Gradle writes either.
$repoForms = @($repo, $repo.Replace('\', '/')) | ForEach-Object { $_.ToLowerInvariant() }
$liveServer = '26.1.2-neoforge-server'

function Test-Ours($proc) {
    $cmd = ([string]$proc.CommandLine).ToLowerInvariant()
    if ($cmd -eq '') { return $false }
    if ($cmd.Contains($liveServer.ToLowerInvariant())) { return $false }
    if ($cmd -match 'gradledaemon|gradleworkermain|gradlewrappermain') { return $false }
    foreach ($form in $repoForms) {
        if ($cmd.Contains($form)) { return $true }
    }
    return $false
}

$procs = @(Get-CimInstance Win32_Process -Filter "Name like 'java%'")
if ($Id) {
    $procs = @($procs | Where-Object { $Id -contains $_.ProcessId })
    foreach ($want in $Id) {
        if (-not ($procs | Where-Object { $_.ProcessId -eq $want })) {
            Write-Output "PID $want is not a running java process - nothing to do for it."
        }
    }
}

$stopped = 0
foreach ($p in $procs) {
    $short = ([string]$p.CommandLine)
    if ($short.Length -gt 120) { $short = $short.Substring(0, 120) + '...' }
    if (Test-Ours $p) {
        if ($WhatIf) {
            Write-Output "WOULD STOP $($p.ProcessId)  $short"
        } else {
            Stop-Process -Id $p.ProcessId -Force
            Write-Output "STOPPED    $($p.ProcessId)  $short"
            $stopped++
        }
    } else {
        Write-Output "LEFT ALONE $($p.ProcessId)  $short"
    }
}
Write-Output "$stopped stopped."
