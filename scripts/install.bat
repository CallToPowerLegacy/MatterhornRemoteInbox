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

setlocal
:PROMPT
SET /P ACCEPTSOFTWARELICENSEAGREEMENT=Do you accept the EULA (to be found in doc/Licenses) (Y/[N])? 
IF /I "%ACCEPTSOFTWARELICENSEAGREEMENT%" NEQ "Y" GOTO ACCEPTSOFTWARELICENSEAGREEMENT_END

msg "%USERNAME%" If another version of MHRI is already installed make sure you saved your key file and your database before installing! Click Enter to continue or close the window to cancel...

pause

SET PATH_TO_FELIX=mhri-1.8.0
SET INSTALL_DIR_NAME=mhri-1.8.0

:: Old versions of MHRI
SET OLD_INSTALL_DIR_NAME_v1-0-0=mhri-1.0.0
SET OLD_INSTALL_DIR_NAME_v1-0-1=mhri-1.0.1
SET OLD_INSTALL_DIR_NAME_v1-1-0=mhri-1.1.0
SET OLD_INSTALL_DIR_NAME_v1-1-1=mhri-1.1.1
SET OLD_INSTALL_DIR_NAME_v1-1-2=mhri-1.1.2
SET OLD_INSTALL_DIR_NAME_v1-1-3=mhri-1.1.3
SET OLD_INSTALL_DIR_NAME_v1-1-4=mhri-1.1.4
SET OLD_INSTALL_DIR_NAME_v1-2-0=mhri-1.2.0
SET OLD_INSTALL_DIR_NAME_v1-2-1=mhri-1.2.1
SET OLD_INSTALL_DIR_NAME_v1-3-0=mhri-1.3.0
SET OLD_INSTALL_DIR_NAME_v1-3-1=mhri-1.3.1
SET OLD_INSTALL_DIR_NAME_v1-4-0=mhri-1.4.0
SET OLD_INSTALL_DIR_NAME_v1-5-0=mhri-1.5.0
SET OLD_INSTALL_DIR_NAME_v1-5-1=mhri-1.5.1
SET OLD_INSTALL_DIR_NAME_v1-6-0=mhri-1.6.0
SET OLD_INSTALL_DIR_NAME_v1-6-1=mhri-1.6.1
SET OLD_INSTALL_DIR_NAME_v1-6-2=mhri-1.6.2
SET OLD_INSTALL_DIR_NAME_v1-6-3=mhri-1.6.3

SET AUTOSTART_FILE_REL_PATH=windows
SET AUTOSTART_FILE=MHRI.xml
SET TASK_NAME="Matterhorn Remote Inbox"
SET SHORTCUT_PATH=%USERPROFILE%\Desktop
SET SHORTCUT_NAME=Matterhorn Remote Inbox
SET SHORTCUT_DESCRIPTION=Matterhorn Remote Inbox
SET SHORTCUT_VB_SCRIPT_NAME=mhri_shortcut
SET START_BAT=start.bat
SET SHORTCUT_ICON_PATH=windows
SET SHORTCUT_ICON=mhri_logo.ico

:: Check Program Files folders
:: if DEFINED PROGRAMFILES(X86) (
:: 	SET TOOL_INSTALL_DIR=%PROGRAMFILES(X86)%\%INSTALL_DIR_NAME%
:: ) else (
:: 	SET TOOL_INSTALL_DIR=%PROGRAMFILES%\%INSTALL_DIR_NAME%
:: )
SET TOOL_INSTALL_DIR=%PROGRAMFILES%\%INSTALL_DIR_NAME%

@echo ON
echo Removing old Matterhorn Remote Inbox versions
@echo OFF

:: Remove old versions from Program Files
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-0-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-0-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-0-1%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-0-1%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-1%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-1%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-2%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-2%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-3%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-3%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-4%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-1-4%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-2-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-2-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-2-1%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-2-1%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-3-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-3-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-3-1%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-3-1%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-4-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-4-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-5-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-5-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-5-1%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-5-1%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-0%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-0%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-1%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-1%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-2%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-2%"
)
IF EXIST "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-3%" (
rmdir /s /q "%PROGRAMFILES%\%OLD_INSTALL_DIR_NAME_v1-6-3%"
)

:: Remove same version from Program Files
IF EXIST "%TOOL_INSTALL_DIR%" (
rmdir /s /q "%TOOL_INSTALL_DIR%"
)

:: Remove shortcut
IF EXIST "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk" (
del "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk"
)

@echo ON
echo Done removing old Matterhorn Remote Inbox versions
echo Installing the Matterhorn Remote Inbox
@echo OFF

:: Copy felix to Program Files
MKDIR "%TOOL_INSTALL_DIR%"
XCOPY "%PATH_TO_FELIX%" "%TOOL_INSTALL_DIR%" /E /C /R /I /K /Y

:: Check installation
IF NOT EXIST "%TOOL_INSTALL_DIR%" GOTO INSTALLATIONFAILED

:: Change user permissions
icacls "%TOOL_INSTALL_DIR%" /grant %USERNAME%:(F,MA) /T /C
:: Create task for autostart
schtasks /CREATE /TN %TASK_NAME% /F /XML "%TOOL_INSTALL_DIR%\%AUTOSTART_FILE_REL_PATH%\%AUTOSTART_FILE%"
:: Create shortcut
cd %SHORTCUT_PATH%
if exist %SHORTCUT_VB_SCRIPT_NAME%.vbs del %SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("Set oWS = WScript.CreateObject("WScript.Shell")") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs   
FOR /F "tokens=1* delims=;" %%B IN ("sLinkFile = "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk"") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("Set oLink = oWS.CreateShortcut(sLinkFile)") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("   oLink.TargetPath = "%TOOL_INSTALL_DIR%\%START_BAT%"") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("   oLink.Description = "%SHORTCUT_DESCRIPTION%"") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("   oLink.WorkingDirectory = "%TOOL_INSTALL_DIR%"") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("   oLink.IconLocation = "%TOOL_INSTALL_DIR%\%SHORTCUT_ICON_PATH%\%SHORTCUT_ICON%"") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
FOR /F "tokens=1* delims=;" %%B IN ("   oLink.Save") do echo %%B>>%SHORTCUT_PATH%\%SHORTCUT_VB_SCRIPT_NAME%.vbs
CSCRIPT %SHORTCUT_VB_SCRIPT_NAME%.vbs
del %SHORTCUT_VB_SCRIPT_NAME%.vbs

powercfg -x -standby-timeout-ac 0
powercfg -x -standby-timeout-dc 0
powercfg -x -hibernate-timeout-ac 0
powercfg -x -hibernate-timeout-dc 0

:: User output
IF EXIST "%SHORTCUT_PATH%\%SHORTCUT_NAME%.lnk" (
msg "%USERNAME%" Successfully installed Matterhorn Remote Inbox. To start the software doubleclick the link at '%SHORTCUT_PATH%'. To start the software automatically log out and log in again or restart the computer.
) ELSE (
msg "%USERNAME%" Successfully installed Matterhorn Remote Inbox. To start the software automatically log out and log in again or restart the computer.
)
Exit /B 5

:INSTALLATIONFAILED
endlocal
:: User output
msg "%USERNAME%" Could not install the Matterhorn Remote Inbox. Please try again.
Exit /B 5

:ACCEPTSOFTWARELICENSEAGREEMENT_END
endlocal
:: User output
msg "%USERNAME%" You have to accept the EULA (to be found in doc/Licenses) to install and use the software...
Exit /B 5
