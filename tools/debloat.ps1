# Disables stock home, ads and telemetry on the Mi Box. Reversible: tools/restore.ps1.
param([Parameter(Mandatory)][string]$Device, [switch]$Restore)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkgs = "com.google.android.tvlauncher","com.google.android.apps.tv.launcherx",
  "com.google.android.tvrecommendations","com.miui.tv.analytics","com.xiaomi.statistic",
  "com.mitv.tvhome.michannel","com.mitv.tvhome.mitvplus","com.google.android.play.games"
$cmd = if ($Restore) { "enable" } else { "disable-user --user 0" }
foreach ($p in $pkgs) { Write-Host "$cmd $p"; & $adb -s $Device shell pm $cmd.Split(" ") $p }
if (-not $Restore) { & $adb -s $Device shell cmd package set-home-activity dev.raj.lucerra/.MainActivity }
