@echo OFF

:: Ensure ADMIN privileges
:: Check for ADMIN Privileges
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
	REM Get ADMIN Privileges
	echo Set UAC = CreateObject^("Shell.Application"^) >  "%temp%\getadmin.vbs"
	echo UAC.ShellExecute "%~s0", "", "", "runas", 1 >> "%temp%\getadmin.vbs"
	"%temp%\getadmin.vbs"
	del "%temp%\getadmin.vbs"
	exit /B
) else (
	REM Got ADMIN Privileges
	pushd "%cd%"
	cd /d "%~dp0"
)

@echo OFF

msg "%USERNAME%" Make sure you saved your key file and your database before uninstalling! Click Enter to continue or close the window to cancel...

pause

SET INSTALL_DIR_NAME=mhri-1.8.0
SET TASK_NAME="Matterhorn Remote Inbox"
SET SHORTCUT_PATH=%USERPROFILE%\Desktop
SET SHORTCUT_NAME=Matterhorn Remote Inbox

:: Check Program Files folders
:: if DEFINED PROGRAMFILES(X86) (
:: 	SET TOOL_INSTALL_DIR=%PROGRAMFILES(X86)%\%INSTALL_DIR_NAME%
:: ) else (
:: 	SET TOOL_INSTALL_DIR=%PROGRAMFILES%\%INSTALL_DIR_NAME%
:: )
SET TOOL_INSTALL_DIR=%PROGRAMFILES%\%INSTALL_DIR_NAME%

:: Remove felix from Program Files
IF EXIST "%TOOL_INSTALL_DIR%" (
rmdir /s /q "%TOOL_INSTALL_DIR%"
)

:: Remove shortcut
IF EXIST "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk" (
del "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk"
)

:: Remove autostart task
schtasks /DELETE /TN %TASK_NAME%

IF NOT EXIST "%TOOL_INSTALL_DIR%" (
IF NOT EXIST "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk" (
msg "%USERNAME%" Successfully uninstalled Matterhorn Remote Inbox.
) ELSE (
msg "%USERNAME%" Could not remove the Matterhorn Remote Inbox completely.
)
) ELSE (
msg "%USERNAME%" Could not remove the Matterhorn Remote Inbox completely.
)

Exit /B 5
