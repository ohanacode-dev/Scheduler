#include "accountsetupdialog.h"
#include "ui_accountsetupdialog.h"
#include <QPushButton>


AccountSetupDialog::AccountSetupDialog(bool defaultRemember, QWidget *parent) :
    QDialog(parent),   
    rememberCredentials(defaultRemember),
    ui(new Ui::AccountSetupDialog)
{
    ui->setupUi(this);
    globals = GlobalVars::getInstance();

    ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(false);
    ui->label_status->setStyleSheet("QLabel { color : red; }");
    ui->label_status->setText("");
    ui->label_link->setText(NEW_ACCOUNT_LINK + tr("Register a new account.") + "</a>");
    ui->label_resetPass->setText(RESET_ACCOUNT_LINK + tr("Forgot your password? Request a reset link.") + "</a>");
    ui->lineEdit_email->setText(globals->email);
    ui->lineEdit_password->setText(globals->password);
    ui->lineEdit_reppeat->setText(globals->password);
    ui->chk_remember->setChecked(rememberCredentials);
}

AccountSetupDialog::~AccountSetupDialog()
{
    delete ui;
}

void AccountSetupDialog::on_buttonBox_accepted()
{
    globals->email = ui->lineEdit_email->text();
    globals->password = ui->lineEdit_password->text();    
}


void AccountSetupDialog::on_lineEdit_email_textChanged(const QString &arg1)
{
    Q_UNUSED( arg1 );
    QString email = ui->lineEdit_email->text();

    if(globals->validateEmail(email)){
        emailValidFlag = true;
        ui->label_status->setText("");
        if(passwordValidFlag && reppeatPassValidFlag){
            ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(true);
        }
    }else{
        emailValidFlag = false;
        ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(false);
        ui->label_status->setText(tr("ERROR: Email not valid."));
    }
}

void AccountSetupDialog::on_lineEdit_password_textChanged(const QString &arg1)
{
    Q_UNUSED( arg1 );
    QString pass = ui->lineEdit_password->text();

    if(pass.length() > 5){
        passwordValidFlag = true;
        ui->label_status->setText("");
        if(emailValidFlag && reppeatPassValidFlag){
            ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(true);
        }
    }else{
        passwordValidFlag = false;
        ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(false);
        ui->label_status->setText(tr("ERROR: Password must be longer than 5 characters."));
    }
}

void AccountSetupDialog::on_lineEdit_reppeat_textChanged(const QString &arg1)
{
    Q_UNUSED( arg1 );
    QString pass = ui->lineEdit_password->text();

    if(pass.compare(ui->lineEdit_reppeat->text()) == 0){
        reppeatPassValidFlag = true;
        ui->label_status->setText("");
        if(emailValidFlag && passwordValidFlag){
            ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(true);
        }
    }else{
        reppeatPassValidFlag = false;
        ui->buttonBox->button(QDialogButtonBox::Ok)->setEnabled(false);
        ui->label_status->setText(tr("ERROR: Password do not match."));
    }
}

void AccountSetupDialog::on_chk_remember_toggled(bool checked)
{
    rememberCredentials = checked;
}


