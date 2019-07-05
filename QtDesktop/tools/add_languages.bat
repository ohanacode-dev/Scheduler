SET QT_PATH=C:\Qt\5.13.0\msvc2015_64\bin

%QT_PATH%/lupdate -verbose OC-Scheduler/OC-Scheduler.pro
%QT_PATH%/linguist OC-Scheduler/languages/Translation_en.ts OC-Scheduler/languages/Translation_sr.ts
%QT_PATH%/lrelease OC-Scheduler/OC-Scheduler.pro
