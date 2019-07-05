#!/bin/bash

QTBINDIR=~/Qt/5.13.0/gcc_64/bin/
PATH=$QTBINDIR:$PATH

cd ..

lupdate -verbose OC-Scheduler/OC-Scheduler.pro
linguist OC-Scheduler/languages/Translation_en.ts OC-Scheduler/languages/Translation_sr.ts
lrelease OC-Scheduler/OC-Scheduler.pro
