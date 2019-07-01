#!/bin/bash

QTBINDIR=~/Qt/5.13.0/gcc_64/bin/
QTLIBDIR=~/Qt/5.13.0/gcc_64/lib/
QTIFWDIR=~/Qt/Tools/QtInstallerFramework/3.1/bin
BUILDDIR=Release
APPEXE=OC-Scheduler

rm -rf installer
mkdir data
mkdir data/languages

cp $BUILDDIR/$APPEXE data/$APPEXE
copy OC-Scheduler/languages data/languages
copy OC-Scheduler/oc_logo* data

./linuxdeployqt data/$APPEXE

cp -r OC-Scheduler/installer/* installer/ 
mv data installer/packages/com.ohanacodedev.OCScheduler/ 

$QTIFWDIR/binarycreator --offline-only -t $QTIFWDIR/installerbase -p installer/packages -c installer/config/config_linux.xml OC_Scheduler_installer
