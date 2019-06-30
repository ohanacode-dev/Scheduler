#ifndef PRESETRECIPIENTSDIALOG_H
#define PRESETRECIPIENTSDIALOG_H

#include <QDialog>
#include "globalvars.h"

namespace Ui {
class PresetRecipientsDialog;
}

class PresetRecipientsDialog : public QDialog
{
    Q_OBJECT

public:
    explicit PresetRecipientsDialog(QMap<QString, QString>* recipientList, QWidget *parent = nullptr);
    ~PresetRecipientsDialog();
    bool accepted = false;

private slots:
    void on_pushButton_Add_clicked();

    void on_buttonBox_accepted();

    void on_tableWidget_recipients_cellClicked(int row, int column);

private:
    void displayNotification(QString message);

    Ui::PresetRecipientsDialog *ui;

    QMap<QString, QString>* p_recipients;

    GlobalVars* globals;
};

#endif // PRESETRECIPIENTSDIALOG_H
