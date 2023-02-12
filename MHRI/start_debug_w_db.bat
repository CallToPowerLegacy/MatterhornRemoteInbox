@echo OFF

:: ensure admin privileges
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
	echo Set UAC = CreateObject^("Shell.Application"^) >  "%temp%\getadmin.vbs"
	echo UAC.ShellExecute "%~s0", "", "", "runas", 1 >> "%temp%\getadmin.vbs"
	"%temp%\getadmin.vbs"
	del "%temp%\getadmin.vbs"
	exit /B
) else (
	pushd "%cd%"
	cd /d "%~dp0"
)

SET DEBUG_PORT=8001
SET DEBUG_SUSPEND=n
SET CONF_DIR=.\conf
SET LOG_LEVEL=INFO

SET JAVA_CONF=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=%DEBUG_PORT%,server=y,suspend=%DEBUG_SUSPEND%
SET FELIX_CONFIG_OPTS=-Dfelix.config.properties=file:%CONF_DIR%\config.properties
SET PAX_CONFMAN_OPTS=-Dbundles.configuration.location=%CONF_DIR%
SET PAX_LOGGING_OPTS=-Dlog4j.configuration=file:.\conf\services\org.ops4j.pax.logging.properties -Dorg.ops4j.pax.logging.DefaultServiceLog.level=%LOG_LEVEL%
SET JARS=bin\felix.jar
SET FELIX_CACHE=.\felix-cache
SET DATABASE=.\database

if exist "%FELIX_CACHE%" (
   rmdir /s /q "%FELIX_CACHE%"
)
if exist ".\PathToInbox" (
   rmdir /s /q ".\PathToInbox"
)
if exist ".\logs" (
   rmdir /s /q ".\logs"
)

pause

@echo ON

java %JAVA_CONF% %FELIX_CONFIG_OPTS% %PAX_CONFMAN_OPTS% %PAX_LOGGING_OPTS% -jar %JARS% %FELIX_CACHE%

pause
