#ifndef ACCOUNTSETUPDIALOG_H
#define ACCOUNTSETUPDIALOG_H

#include <QDialog>
#include "globalvars.h"

namespace Ui {
class AccountSetupDialog;
}

class AccountSetupDialog : public QDialog
{
    Q_OBJECT

public:
    explicit AccountSetupDialog(bool defaultRemember, QWidget *parent = nullptr);
    ~AccountSetupDialog();

    bool rememberCredentials = false;

private slots:
    void on_buttonBox_accepted();
    void on_lineEdit_email_textChanged(const QString &arg1);
    void on_lineEdit_password_textChanged(const QString &arg1);
    void on_chk_remember_toggled(bool checked);
    void on_lineEdit_reppeat_textChanged(const QString &arg1);

private:
    Ui::AccountSetupDialog *ui;
    bool passwordValidFlag = false;
    bool reppeatPassValidFlag = false;
    bool emailValidFlag = false;
    GlobalVars* globals;
};

#endif // ACCOUNTSETUPDIALOG_H
