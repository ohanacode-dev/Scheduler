#!/bin/bash

QTBINDIR=~/Qt/5.13.0/gcc_64/bin/
QTLIBDIR=~/Qt/5.13.0/gcc_64/lib/
QTINCLUDEDIR=~/Qt/5.13.0/gcc_64/include/
QTIFWDIR=~/Qt/Tools/QtInstallerFramework/3.1/bin
SYSLIBPATH=/usr/lib/x86_64-linux-gnu
SYSLIBPATH2=/lib/x86_64-linux-gnu/

APPEXE=OC-Scheduler

PATH=$QTBINDIR:$QTIFWDIR:$QTINCLUDEDIR:$QTLIBSDIR:$PATH

CALL_DIR=$PWD

cd "$(dirname "$0")"


rm -rf build
cp -r ../OC-Scheduler/ build
cd build
qmake -config release
make

mkdir data

cp $APPEXE data/$APPEXE
mkdir data/languages
cp languages/* data/languages/
cp oc_logo* data

../linuxdeployqt-6-x86_64.AppImage data/$APPEXE -no-translations

cp -r data installer/packages/com.ohanacodedev.OCScheduler/

rm ../../OC_Scheduler_linux_installer
binarycreator --offline-only -p installer/packages -c installer/config/config_linux.xml ../../OC_Scheduler_linux_installer

cd $CALL_DIR
