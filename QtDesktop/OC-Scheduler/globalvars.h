#ifndef GLOBALVARS_H
#define GLOBALVARS_H

#include <QObject>

#define SETTINGS_FILE_NAME  "/settings.txt"
#define NEW_ACCOUNT_LINK    "<a href=\"http://scheduleservice.ohanacode-dev.com/signup\">"
#define RESET_ACCOUNT_LINK  "<a href=\"http://scheduleservice.ohanacode-dev.com/resetpass\">"
#define BASE_SERVER_URL     "http://api.scheduleservice.ohanacode-dev.com/"
#define SEPARATOR           "~|~"
#define LOCALE_EN           "English"
#define LOCALE_SR           "Srpski"
#define ROLE_CLIENT         "client"
#define ROLE_MULTICLIENT    "multiclient"


class GlobalVars : public QObject
{
    Q_OBJECT

public:
    ~GlobalVars();

    static GlobalVars* getInstance();

    bool validateEmail(QString val);

    QString email;
    QString name;
    QString password;
    QString token;
    QString role;
    QString selectedDate;

private:
    GlobalVars();
    static GlobalVars * s_instance;

};

#endif // GLOBALVARS_H
