SET QTBINDIR=C:\Qt\5.13.0\mingw73_64\bin
SET COMPILERDIR=C:\Qt\Tools\mingw730_64\bin
set PATH=%PATH%;%QTBINDIR%;%COMPILERDIR%

cd ..

lupdate -verbose OC-Scheduler/OC-Scheduler.pro
linguist OC-Scheduler/languages/Translation_en.ts OC-Scheduler/languages/Translation_sr.ts
lrelease OC-Scheduler/OC-Scheduler.pro
