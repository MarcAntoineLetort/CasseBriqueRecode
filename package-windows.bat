@echo off
rem ============================================================================
rem  Fabrique un dossier "CasseBrique" autonome (Java + JavaFX inclus) a envoyer tel quel.
rem  A lancer sur Windows, avec JDK 21 et Maven installes (jpackage fait partie du JDK).
rem  Resultat : dist\CasseBrique\CasseBrique.exe  -> zipper le dossier "CasseBrique" et l'envoyer.
rem ============================================================================
setlocal
cd /d "%~dp0"

call mvn -q clean package dependency:copy-dependencies -DoutputDirectory=target\libs
if errorlevel 1 goto erreur

copy /y target\casse-brique-3.0.jar target\libs\ >nul
if exist dist rmdir /s /q dist

jpackage --type app-image --name CasseBrique ^
  --input target\libs --main-jar casse-brique-3.0.jar --main-class cassebrique.Launcher ^
  --dest dist --java-options "-Xmx512m" ^
  --jlink-options "--strip-debug --no-header-files --no-man-pages --compress=2"
if errorlevel 1 goto erreur

echo.
echo Termine : dist\CasseBrique\CasseBrique.exe
echo (Pour un vrai installateur .exe avec raccourcis : installer WiX Toolset 3, puis remplacer
echo  "--type app-image" par "--type exe --win-shortcut --win-menu --win-dir-chooser".)
goto fin

:erreur
echo.
echo ECHEC : verifiez que "java -version" affiche 21 et que "mvn -v" fonctionne.
exit /b 1

:fin
endlocal
