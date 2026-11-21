@echo off
setlocal EnableExtensions
REM Create a self-signed code-signing certificate for local Niagara development.
REM Writes only under security\, which is gitignored except for the example templates.

if "%NIAGARA_SIGNING_STORE_PASSWORD%"=="" (
  echo Set NIAGARA_SIGNING_STORE_PASSWORD before running this script.
  exit /b 1
)

if "%NIAGARA_SIGNING_ALIAS%"=="" set "NIAGARA_SIGNING_ALIAS=controlbasic"
if "%NIAGARA_SIGNING_KEY_PASSWORD%"=="" set "NIAGARA_SIGNING_KEY_PASSWORD=%NIAGARA_SIGNING_STORE_PASSWORD%"
if "%NIAGARA_SIGNING_DNAME%"=="" set "NIAGARA_SIGNING_DNAME=CN=Control Basic Dev, OU=Development, O=Local, C=US"
if "%NIAGARA_SIGNING_DAYS%"=="" set "NIAGARA_SIGNING_DAYS=3650"

set "ROOT=%~dp0.."
set "SEC=%ROOT%\security"
if not exist "%SEC%" mkdir "%SEC%"
if "%NIAGARA_SIGNING_KEYSTORE%"=="" set "NIAGARA_SIGNING_KEYSTORE=%SEC%\dev-keystore.p12"

if exist "%NIAGARA_SIGNING_KEYSTORE%" (
  echo Keystore already exists: %NIAGARA_SIGNING_KEYSTORE%
  echo Delete it first if you intend to replace it.
  exit /b 1
)

keytool -genkeypair ^
  -alias "%NIAGARA_SIGNING_ALIAS%" ^
  -keyalg RSA ^
  -keysize 2048 ^
  -sigalg SHA256withRSA ^
  -validity %NIAGARA_SIGNING_DAYS% ^
  -storetype PKCS12 ^
  -keystore "%NIAGARA_SIGNING_KEYSTORE%" ^
  -storepass "%NIAGARA_SIGNING_STORE_PASSWORD%" ^
  -keypass "%NIAGARA_SIGNING_KEY_PASSWORD%" ^
  -dname "%NIAGARA_SIGNING_DNAME%" ^
  -ext "BasicConstraints=ca:false" ^
  -ext "KeyUsage=digitalSignature" ^
  -ext "ExtendedKeyUsage=codeSigning"
if errorlevel 1 exit /b 1

keytool -exportcert ^
  -alias "%NIAGARA_SIGNING_ALIAS%" ^
  -keystore "%NIAGARA_SIGNING_KEYSTORE%" ^
  -storetype PKCS12 ^
  -storepass "%NIAGARA_SIGNING_STORE_PASSWORD%" ^
  -rfc ^
  -file "%SEC%\dev-cert.cer"
if errorlevel 1 exit /b 1

echo alias=%NIAGARA_SIGNING_ALIAS%> "%SEC%\niagara.signing.properties"
echo keystoreType=PKCS12>> "%SEC%\niagara.signing.properties"
echo keystoreFile=%NIAGARA_SIGNING_KEYSTORE%>> "%SEC%\niagara.signing.properties"
echo storePassword=%NIAGARA_SIGNING_STORE_PASSWORD%>> "%SEC%\niagara.signing.properties"
echo keyPassword=%NIAGARA_SIGNING_KEY_PASSWORD%>> "%SEC%\niagara.signing.properties"
echo timestampUrl=http://timestamp.digicert.com>> "%SEC%\niagara.signing.properties"

echo ^<?xml version="1.0" encoding="UTF-8"?^> > "%SEC%\niagara.signing.xml"
echo ^<signingProfile^> >> "%SEC%\niagara.signing.xml"
echo   ^<className^>com.tridium.gradle.plugins.signing.profile.RestrictedSigningProfile^</className^> >> "%SEC%\niagara.signing.xml"
echo   ^<alias^>%NIAGARA_SIGNING_ALIAS%^</alias^> >> "%SEC%\niagara.signing.xml"
echo   ^<keystoreType^>PKCS12^</keystoreType^> >> "%SEC%\niagara.signing.xml"
echo   ^<keystoreFile^>%NIAGARA_SIGNING_KEYSTORE%^</keystoreFile^> >> "%SEC%\niagara.signing.xml"
echo   ^<storePassword^>%NIAGARA_SIGNING_STORE_PASSWORD%^</storePassword^> >> "%SEC%\niagara.signing.xml"
echo   ^<keyPassword^>%NIAGARA_SIGNING_KEY_PASSWORD%^</keyPassword^> >> "%SEC%\niagara.signing.xml"
echo   ^<timestampUrl^>http://timestamp.digicert.com^</timestampUrl^> >> "%SEC%\niagara.signing.xml"
echo ^</signingProfile^> >> "%SEC%\niagara.signing.xml"

echo Wrote %NIAGARA_SIGNING_KEYSTORE%
echo Wrote %SEC%\dev-cert.cer
echo Import the .cer into the Workbench User Trust Store. See docs\SIGNING.md.
exit /b 0
