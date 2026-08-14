param(
  [switch]$Install
)

$ErrorActionPreference = "Stop"
$Project = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Project

Write-Host "Activity AI - First Android Test Build"
Write-Host "Project: $Project"

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
  $Candidates = @(
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk"
  )
  foreach ($c in $Candidates) {
    if (Test-Path $c) {
      $env:ANDROID_HOME = $c
      $env:ANDROID_SDK_ROOT = $c
      break
    }
  }
}

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
  throw "Android SDK not found. Install Android Studio + SDK Platform 37 and Build Tools 36.0.0 first."
}

$Sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }
Write-Host "Android SDK: $Sdk"

$Gradlew = Join-Path $Project "gradlew.bat"
if (Test-Path $Gradlew) {
  & $Gradlew clean assembleDebug
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
  gradle clean assembleDebug
} else {
  throw "Gradle wrapper is not present and system Gradle was not found. Open this project once in Android Studio and allow Gradle sync, then rerun this script."
}

$Apk = Join-Path $Project "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $Apk)) {
  throw "Build finished but APK was not found at expected path: $Apk"
}

$Out = Join-Path $Project "ActivityAI-M1-FirstTest.apk"
Copy-Item $Apk $Out -Force
Write-Host ""
Write-Host "BUILD SUCCESS"
Write-Host "APK: $Out"

if ($Install) {
  $adb = Join-Path $Sdk "platform-tools\adb.exe"
  if (-not (Test-Path $adb)) { throw "adb.exe not found. Install Android SDK Platform Tools." }
  & $adb install -r $Out
}
