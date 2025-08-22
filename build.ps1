# Set strict mode to catch errors
Set-StrictMode -Version Latest

# --- C++ COMPILATION ---
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Configuring C++ project with CMake..." -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# Define paths
$sourceDir = "src/main/cpp"
$buildDir = "src/main/cpp/build"

# Clean the previous C++ build directory to ensure a fresh build
if (Test-Path $buildDir) {
    Write-Host "Removing old C++ build directory: $buildDir"
    Remove-Item -Recurse -Force $buildDir
}

# Run CMake to configure the project, explicitly telling it to use Visual Studio 2022 64-bit.
# This is the key fix for the "No CMAKE_C_COMPILER" error.
cmake -S $sourceDir -B $buildDir -G "Visual Studio 17 2022" -A x64 -DCMAKE_BUILD_TYPE=Release

# Check if CMake configuration was successful
if (-not $?) {
    Write-Host "`nCMake configuration failed! Aborting script." -ForegroundColor Red
    exit 1
}

Write-Host "`n===================================================" -ForegroundColor Cyan
Write-Host " Building C++ libraries..." -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# Run the build using CMake's build command for the 'Release' configuration.
cmake --build $buildDir --config Release

# Check if C++ build was successful
if (-not $?) {
    Write-Host "`nC++ build failed! Aborting script." -ForegroundColor Red
    exit 1
}


# --- JAVA COMPILATION & PACKAGING ---
Write-Host "`n===================================================" -ForegroundColor Green
Write-Host " Building Java project with Maven..." -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green

# Run the Maven command WITHOUT the -DcompileLibs=true flag.
# Maven will now pick up the DLLs we just built via the <resources> block in pom.xml.
mvn clean package

# Check if the Maven command was successful
if (-not $?) {
    Write-Host "`nMaven build failed! Aborting script." -ForegroundColor Red
    exit 1
}