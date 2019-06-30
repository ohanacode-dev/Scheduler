#include "application.h"
#include <QApplication>
#include <QStyle>
#include <QDesktopWidget>
#include <QGuiApplication>
#include <QScreen>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);

//    QLocale::setDefault(QLocale::English);

    Application w;
    w.show();

    /* Center window on the screen */
    w.setGeometry(
        QStyle::alignedRect(
            Qt::LeftToRight,
            Qt::AlignCenter,
            w.size(),
            QGuiApplication::screens().first()->geometry()
        )
    );

    return a.exec();
}
