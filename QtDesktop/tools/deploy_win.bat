SET QTBINDIR=C:\Qt\5.13.0\mingw73_64\bin
SET COMPILERDIR=C:\Qt\Tools\mingw730_64\bin
SET QTIFWDIR=C:\Qt\Tools\QtInstallerFramework\3.1\bin
set QTINCLUDEDIR=C:\Qt\5.13.0\mingw73_64\include
set QTLIBSDIR=C:\Qt\5.13.0\mingw73_64\lib

SET APPEXE=OC-Scheduler.exe

set PATH=%PATH%;%QTBINDIR%;%COMPILERDIR%;%QTIFWDIR%;%QTINCLUDEDIR%;%QTLIBSDIR%

rmdir build /s /q
xcopy ..\OC-Scheduler\* build\ /s /e
cd build
qmake
mingw32-make release

mkdir data

copy release\%APPEXE% data\%APPEXE%
xcopy languages\* data\languages\ /s /e
copy oc_logo* data

windeployqt.exe data\%APPEXE%

move data installer\packages\com.ohanacodedev.OCScheduler\ 

del ..\..\OC_Scheduler_win_installer*
binarycreator.exe --offline-only -p installer\packages -c installer\config\config_win32.xml ..\..\OC_Scheduler_win_installer
cd ..
