@echo off
rem ============================================================================
rem  Fabrique dist\CasseBrique.zip : un dossier autonome (Java + JavaFX inclus) a envoyer tel quel.
rem  Le destinataire decompresse le zip et lance CasseBrique\CasseBrique.exe (rien a installer).
rem
rem  Prerequis (une seule fois) : JDK 21 (avec jpackage). Pour la compilation, UNE des deux :
rem    - Maven installe (commande "mvn"), le script compile tout seul ;
rem    - ou, dans Eclipse : clic droit sur le projet > Run As > Maven build... > Goals : clean package
rem      (a refaire apres chaque modification du code), puis lancer ce script.
rem ============================================================================
setlocal
cd /d "%~dp0"

rem --- jpackage : JAVA_HOME en priorite, sinon le PATH
set "JPACKAGE=jpackage"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
"%JPACKAGE%" --version >nul 2>&1
if errorlevel 1 (
  echo jpackage introuvable. Installez un JDK 21 et definissez JAVA_HOME ^(ex. C:\Program Files\Java\jdk-21^).
  goto erreur
)

rem --- compilation
where mvn >nul 2>&1
if not errorlevel 1 (
  echo Compilation avec Maven...
  call mvn -q clean package
  if errorlevel 1 goto erreur
) else (
  echo Maven absent de la ligne de commande : utilisation du dernier "Maven build" fait dans Eclipse.
)
if not exist target\casse-brique.jar (
  echo target\casse-brique.jar introuvable : lancez d'abord "clean package" ^(voir en-tete de ce fichier^).
  goto erreur
)
if not exist target\libs\javafx-graphics*.jar (
  echo target\libs est vide : lancez "clean package" ^(et non "compile"^).
  goto erreur
)

rem --- dossier autonome
if exist dist rmdir /s /q dist
copy /y target\casse-brique.jar target\libs\ >nul
"%JPACKAGE%" --type app-image --name CasseBrique ^
  --input target\libs --main-jar casse-brique.jar --main-class cassebrique.Launcher ^
  --dest dist --java-options "-Xmx512m" ^
  --jlink-options "--strip-debug --no-header-files --no-man-pages --compress=zip-6"
if errorlevel 1 goto erreur

rem --- zip pret a envoyer
powershell -NoProfile -Command "Compress-Archive -Path 'dist\CasseBrique' -DestinationPath 'dist\CasseBrique.zip' -Force"
if errorlevel 1 goto erreur

echo.
echo ======================================================================
echo  Termine : dist\CasseBrique.zip  ^(a envoyer^)
echo  Pour tester : dist\CasseBrique\CasseBrique.exe
echo ======================================================================
goto fin

:erreur
echo.
echo ECHEC : voir le message ci-dessus.
exit /b 1

:fin
endlocal
