SET QTBINDIR=C:\Qt\5.13.0\msvc2015_64\bin
SET QTIFWDIR=C:\Qt\Tools\QtInstallerFramework\3.0\bin
SET BUILDDIR=release
SET APPEXE=OC-Scheduler.exe

rmdir installer /s /q
mkdir data
mkdir data\languages

copy %BUILDDIR%\release\%APPEXE% data\%APPEXE%
copy OC-Scheduler\languages data\languages
copy OC-Scheduler\oc_logo* data

%QTBINDIR%/windeployqt.exe data\%APPEXE%

xcopy OC-Scheduler\installer\* installer\ /s /e
move data installer\packages\com.ohanacodedev.OCScheduler\ 

%QTIFWDIR%\binarycreator.exe --offline-only -t %QTIFWDIR%\installerbase.exe -p installer\packages -c installer\config\config_win32.xml OC_Scheduler_installer
