#!/bin/bash

QTBINDIR=~/Qt/5.13.0/gcc_64/bin/
QTLIBDIR=~/Qt/5.13.0/gcc_64/lib/
QTINCLUDEDIR=~/Qt/5.13.0/gcc_64/include/
QTIFWDIR=~/Qt/Tools/QtInstallerFramework/3.1/bin
SYSLIBPATH=/usr/lib/x86_64-linux-gnu
SYSLIBPATH2=/lib/x86_64-linux-gnu/

APPEXE=OC-Scheduler

PATH=$QTBINDIR:$QTIFWDIR:$QTINCLUDEDIR:$QTLIBSDIR:$PATH

rm -rf build
cp -r OC-Scheduler/ build
cd build

qmake -config release
make

mkdir data
# Copy libraries

cp $QTLIBDIR/libQt5Widgets.so.5 data/
cp $QTLIBDIR/libQt5Gui.so.5 data/
cp $QTLIBDIR/libQt5Network.so.5 data/
cp $QTLIBDIR/libQt5Core.so.5 data/
cp $QTLIBDIR/libicui18n.so.56 data/
cp $QTLIBDIR/libicuuc.so.56 data/
cp $QTLIBDIR/libicudata.so.56 data/

cp $SYSLIBPATH/libgtk3-nocsd.so.0 data/
cp $SYSLIBPATH/libQt5Widgets.so.5 data/
cp $SYSLIBPATH/libQt5Gui.so.5 data/
cp $SYSLIBPATH/libQt5Network.so.5 data/
cp $SYSLIBPATH/libQt5Core.so.5 data/
cp $SYSLIBPATH/libstdc++.so.6 data/
cp $SYSLIBPATH2/libgcc_s.so.1 data/
cp $SYSLIBPATH2/libc.so.6 data/
cp $SYSLIBPATH2/libdl.so.2 data/ 
cp $SYSLIBPATH2/libpthread.so.0 data/
cp $SYSLIBPATH2/libm.so.6 data/
cp $SYSLIBPATH/libGL.so.1 data/ 
cp $SYSLIBPATH/libpng16.so.16 data/
cp $SYSLIBPATH/libharfbuzz.so.0 data/
cp $SYSLIBPATH2/libz.so.1 data/
cp $SYSLIBPATH/libicui18n.so.60 data/
cp $SYSLIBPATH/libicuuc.so.60 data/
cp $SYSLIBPATH/libdouble-conversion.so.1 data/
cp $SYSLIBPATH/libglib-2.0.so.0 data/
cp $SYSLIBPATH/libGLX.so.0 data/
cp $SYSLIBPATH/libGLdispatch.so.0 data/
cp $SYSLIBPATH/libfreetype.so.6 data/
cp $SYSLIBPATH/libgraphite2.so.3 data/ 
cp $SYSLIBPATH/libicudata.so.60 data/
cp $SYSLIBPATH2/libpcre.so.3 data/
cp $SYSLIBPATH/libX11.so.6 data/  
cp $SYSLIBPATH/libxcb.so.1 data/   
cp $SYSLIBPATH/libXau.so.6 data/
cp $SYSLIBPATH/libXdmcp.so.6 data/ 
cp $SYSLIBPATH2/libbsd.so.0 data/
cp $SYSLIBPATH2/librt.so.1 data/

cp $APPEXE data/$APPEXE
mkdir data/languages
cp languages/* data/languages/
cp oc_logo* data

cp -r data/* installer/packages/com.ohanacodedev.OCScheduler/data/ 

binarycreator --offline-only -p installer/packages -c installer/config/config_linux.xml OC_Scheduler_linux_installer
