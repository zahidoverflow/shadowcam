param(
  [string]$RepoRoot = (Resolve-Path ".").Path
)

$ErrorActionPreference = "Stop"

$gitDir = Join-Path $RepoRoot ".git"
if (-not (Test-Path -LiteralPath $gitDir)) {
  Write-Error "Not a git repository: $RepoRoot"
}

$bad = @()

# Cloud-sync tools sometimes drop Windows metadata files into .git, corrupting refs.
$desktopIni = Get-ChildItem -LiteralPath $gitDir -Recurse -Force -File -Filter "desktop.ini" -ErrorAction SilentlyContinue
if ($desktopIni) { $bad += $desktopIni.FullName }

if ($bad.Count -gt 0) {
  Write-Host "Detected suspicious files inside .git (likely cloud-sync corruption):"
  $bad | ForEach-Object { Write-Host " - $_" }
  exit 2
}

Write-Host "OK: no desktop.ini found under .git"

