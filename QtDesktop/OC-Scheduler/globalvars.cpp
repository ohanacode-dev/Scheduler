#include "globalvars.h"

GlobalVars * GlobalVars::s_instance = nullptr;

GlobalVars::GlobalVars()
{
    email = "";
    password = "";
    name = "";
    token = "";
}

GlobalVars * GlobalVars::getInstance()
{
    if(s_instance == nullptr)
    {
        s_instance = new GlobalVars();
    }
    return s_instance;
}

GlobalVars::~GlobalVars()
{

}

bool GlobalVars::validateEmail(QString val)
{
    QRegExp mailREX("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b");
    mailREX.setCaseSensitivity(Qt::CaseInsensitive);
    mailREX.setPatternSyntax(QRegExp::RegExp);

    return mailREX.exactMatch(val.trimmed().toLower());
}
