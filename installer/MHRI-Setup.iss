#define appName "Matterhorn Remote Inbox"
#define appVersion "1.8.0"
#define appVersionAddition " build 20.03.2015 1"
#define appPublisher "ELAN e.V."
#define appURL "http://zentrum.virtuos.uni-osnabrueck.de/mhri"
#define appExeName "start.bat"
#define appPublisherContact "calltopower88@gmail.com"
#define appCopyright "2013-2015 Denis Meyer, ELAN e.V."
#define srcPath "C:\Users\calltopower\Documents\Repositories\MHRI"
#define srcPath_short "Repositories\MHRI"
#define desktopPath "C:\Users\Denis\Desktop"
#define installFolderName "MHRI"
#define MinJRE "1.8"
#define WebJRE "http://www.oracle.com/technetwork/java/javase/downloads/index.html"

[Setup]
AppId={{033C4A11-A6BE-4F05-9F18-A8AD0A1D5881}}
AppName={#appName}
AppVersion={#appVersion}
AppPublisher={#appPublisher}
AppPublisherURL={#appURL}
AppSupportURL={#appURL}
AppUpdatesURL={#appURL}
DefaultDirName={pf}\{#installFolderName}\{#appVersion}
DefaultGroupName={#appName}
LicenseFile={#srcPath}\doc\Licenses\EULA
OutputDir={#desktopPath}
OutputBaseFilename=MHRI {#appVersion} {#appVersionAddition} Setup
SetupIconFile={#srcPath}\MHRI\windows\mhri_logo.ico
Compression=lzma
SolidCompression=yes
RestartIfNeededByRun=False
UsePreviousAppDir=False
AppContact={#appPublisherContact}
PrivilegesRequired=admin
AppReadmeFile={#srcPath}\README
CloseApplicationsFilter=*.exe,*.dll,*.chm, *.bat
UninstallLogMode=overwrite
UninstallDisplayName={#appName}
UninstallDisplayIcon={uninstallexe}
AppCopyright={#appCopyright}
MinVersion=0,6.1
RestartApplications=False
VersionInfoVersion={#appVersion}
VersionInfoCompany={#appPublisher}
VersionInfoCopyright={#appCopyright}
VersionInfoProductName={#appName}
VersionInfoProductVersion={#appVersion}
WizardImageFile=userdocs:{#srcPath_short}\installer\installer_logo_big.bmp
WizardSmallImageFile=userdocs:{#srcPath_short}\installer\installer_logo_small.bmp
DisableDirPage=yes
CreateUninstallRegKey=yes
AlwaysRestart=True

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "german"; MessagesFile: "compiler:Languages\German.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#srcPath}\MHRI\start.bat"; DestDir: "{app}\App"; Flags: ignoreversion
Source: "{#srcPath}\MHRI\application\*"; DestDir: "{app}\App\application"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#srcPath}\MHRI\bin\*"; DestDir: "{app}\App\bin"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#srcPath}\MHRI\bundle\*"; DestDir: "{app}\App\bundle"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#srcPath}\MHRI\conf\*"; DestDir: "{app}\App\conf"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#srcPath}\MHRI\windows\*"; DestDir: "{app}\App\windows"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#srcPath}\README"; DestDir: "{app}\Files"; Flags: ignoreversion
Source: "{#srcPath}\README_ger"; DestDir: "{app}\Files"; Flags: ignoreversion
Source: "{#srcPath}\doc\Licenses\*"; DestDir: "{app}\Files\Licenses"; Flags: ignoreversion
Source: "{#srcPath}\doc\manual\en\Manual.pdf"; DestDir: "{app}\Files\Manual"; Flags: ignoreversion
Source: "{#srcPath}\doc\manual\ger\Bedienungsanleitung.pdf"; DestDir: "{app}\Files\Manual"; Flags: ignoreversion
Source: "{#srcPath}\doc\Changelog"; DestDir: "{app}\Files\Changelog"; Flags: ignoreversion
Source: "{#srcPath}\doc\Changelog_de"; DestDir: "{app}\Files\Changelog"; Flags: ignoreversion

[Dirs]
Name: "{app}\App\database"
Name: "{app}\App\felix-cache"
Name: "{app}\App\logs"

[Icons]
Name: "{group}\{#appName}"; Filename: "{app}\App\{#appExeName}"; IconFilename: "{app}\App\windows\mhri_logo.ico"
Name: "{group}\{cm:ProgramOnTheWeb,{#appName}}"; Filename: "{#appURL}"
Name: "{group}\{cm:UninstallProgram,{#appName}}"; Filename: "{uninstallexe}"; IconFilename: "{app}\App\windows\mhri_logo.ico";
Name: "{commondesktop}\{#appName}"; Filename: "{app}\App\{#appExeName}"; IconFilename: "{app}\App\windows\mhri_logo.ico"; Tasks: desktopicon;

[Run]
Filename: schtasks.exe; Parameters:"/CREATE /TN ""{#appName}"" /F /XML ""{app}\App\windows\MHRI.xml"""
Filename: icacls.exe; Parameters: """{app}"" /grant {username}:(OI)(CI)F /T /C /Q"
Filename: powercfg.exe; Parameters: "-x -standby-timeout-ac 0"
Filename: powercfg.exe; Parameters: "-x -standby-timeout-dc 0"
Filename: powercfg.exe; Parameters: "-x -hibernate-timeout-ac 0"
Filename: powercfg.exe; Parameters: "-x -hibernate-timeout-dc 0"
Filename: "{app}\App\{#appExeName}"; Flags: nowait postinstall skipifsilent; Check: IsJREInstalled
; Filename: "{app}\App\{#appExeName}"; Description: "{cm:LaunchProgram,{#StringChange(appName, '&', '&&')}}"; Flags: nowait postinstall
; Filename: "{app}\App\{#appExeName}"; Description: "{cm:LaunchProgram,{#StringChange(appName, '&', '&&')}}"; Flags: shellexec postinstall skipifsilent

[Code]
function IsJREInstalled: Boolean;
var
  JREVersion: string;
begin
  Result := RegQueryStringValue(HKLM32, 'Software\JavaSoft\Java Runtime Environment', 'CurrentVersion', JREVersion);
  if not Result and IsWin64 then
    Result := RegQueryStringValue(HKLM64, 'Software\JavaSoft\Java Runtime Environment', 'CurrentVersion', JREVersion);
  if Result then
    Result := CompareStr(JREVersion, '{#MinJRE}') >= 0;
end;

function IsAppRunning(const FileName : string): Boolean;
var
    FSWbemLocator: Variant;
    FWMIService   : Variant;
    FWbemObjectSet: Variant;
begin
    Result := false;
    FSWbemLocator := CreateOleObject('WBEMScripting.SWBEMLocator');
    FWMIService := FSWbemLocator.ConnectServer('', 'root\CIMV2', '', '');
    FWbemObjectSet := FWMIService.ExecQuery(Format('SELECT Name FROM Win32_Process Where Name="%s"',[FileName]));
    Result := (FWbemObjectSet.Count > 0);
    FWbemObjectSet := Unassigned;
    FWMIService := Unassigned;
    FSWbemLocator := Unassigned;
end;

function InitializeSetup: Boolean;
var
  ErrorCode: Integer;
begin
  Result := True;

  if IsAppRunning('javaw.exe') then
  begin
    MsgBox('Another version of the Matterhorn Remote Inbox seems to be running.'#13#10'Please quit all other instances before you continue.'#13#10''#13#10'If no other instance is running, please ignore this message.', mbInformation, MB_OK);
  end;

  if not IsJREInstalled then
  begin
    if MsgBox('Java 8 is required. Do you want to download it now?', mbConfirmation, MB_YESNO) = IDYES then
    begin
      Result := False;
      ShellExec('', '{#WebJRE}', '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode);
    end;
  end;
end;

[UninstallDelete]
Type: filesandordirs; Name: "App\database\*"
Type: filesandordirs; Name: "App\felix-cache\*"
Type: filesandordirs; Name: "App\logs\*"
  
[UninstallRun]
Filename: schtasks.exe; Parameters:"/DELETE /TN ""{#appName}"""
