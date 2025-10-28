@ECHO OFF
SETLOCAL

SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

IF NOT EXIST "%WRAPPER_PROPERTIES%" (
  ECHO Cannot locate %WRAPPER_PROPERTIES%
  EXIT /B 1
)

IF NOT EXIST "%WRAPPER_JAR%" (
  FOR /F "tokens=2 delims==" %%A IN ('findstr /R "^wrapperUrl" "%WRAPPER_PROPERTIES%"') DO SET WRAPPER_URL=%%A
  IF EXIST "%SystemRoot%\System32\curl.exe" (
    curl -fsSL "%WRAPPER_URL%" -o "%WRAPPER_JAR%"
  ) ELSE (
    powershell -Command "Invoke-WebRequest -Uri %WRAPPER_URL% -OutFile '%WRAPPER_JAR%'"
  )
)

IF NOT EXIST "%WRAPPER_JAR%" (
  IF NOT "%MAVEN_HOME%"=="" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
    EXIT /B %ERRORLEVEL%
  )
  IF EXIST "%ProgramFiles%\Apache\Maven\bin\mvn.cmd" (
    "%ProgramFiles%\Apache\Maven\bin\mvn.cmd" %*
    EXIT /B %ERRORLEVEL%
  )
  mvn %*
  EXIT /B %ERRORLEVEL%
)

IF NOT "%JAVA_HOME%"=="" (
  SET JAVA_CMD=%JAVA_HOME%\bin\java.exe
) ELSE (
  SET JAVA_CMD=java
)

"%JAVA_CMD%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
