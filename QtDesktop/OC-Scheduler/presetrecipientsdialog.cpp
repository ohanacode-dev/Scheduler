#include "presetrecipientsdialog.h"
#include "ui_presetrecipientsdialog.h"
#include <QDebug>
#include <QMessageBox>

PresetRecipientsDialog::PresetRecipientsDialog(QMap<QString, QString> *recipientList, QWidget *parent) :
    QDialog(parent),
    ui(new Ui::PresetRecipientsDialog),
    p_recipients(recipientList)
{
    ui->setupUi(this);

    ui->tableWidget_recipients->setColumnCount(3);
    ui->tableWidget_recipients->setHorizontalHeaderLabels(QStringList() << tr("Name") << tr("Email") << " ");
    ui->tableWidget_recipients->setSortingEnabled(true);

    QHeaderView* header = ui->tableWidget_recipients->horizontalHeader();
    header->setSectionResizeMode(0, QHeaderView::Stretch);
    header->setSectionResizeMode(1, QHeaderView::Stretch);


    QMapIterator<QString, QString> i(*p_recipients);

    while (i.hasNext()) {
        i.next();
        int rowId = ui->tableWidget_recipients->rowCount();
        ui->tableWidget_recipients->insertRow(rowId);

        ui->tableWidget_recipients->setItem( rowId, 0, new QTableWidgetItem(i.key()));
        ui->tableWidget_recipients->setItem( rowId, 1, new QTableWidgetItem(i.value()));

        QTableWidgetItem* item = new QTableWidgetItem;
        item->setIcon(QIcon(":/close.png"));

        ui->tableWidget_recipients->setItem(rowId, 2, item);
    }

    ui->tableWidget_recipients->update();

    globals = GlobalVars::getInstance();
}

PresetRecipientsDialog::~PresetRecipientsDialog()
{
    delete ui;
}

void PresetRecipientsDialog::on_pushButton_Add_clicked()
{
    QString name = ui->lineEdit_Name->text();
    QString email = ui->lineEdit_Email->text();

    if(name.length() < 3){
        displayNotification(tr("ERROR: Name must be longer than 2 characters!"));
    }else if(!globals->validateEmail(email)){
        displayNotification(tr("ERROR: email is not valid!"));
    }else{
        int rowId = ui->tableWidget_recipients->rowCount();
        ui->tableWidget_recipients->insertRow(rowId);

        ui->tableWidget_recipients->setItem( rowId, 0, new QTableWidgetItem(name));
        ui->tableWidget_recipients->setItem( rowId, 1, new QTableWidgetItem(email));

        QTableWidgetItem* item = new QTableWidgetItem;
        item->setIcon(QIcon(":/close.png"));

        ui->tableWidget_recipients->setItem(rowId, 2, item);
        ui->tableWidget_recipients->update();
    }
}

void PresetRecipientsDialog::on_buttonBox_accepted()
{
    p_recipients->clear();

    for(int r = 0; r < ui->tableWidget_recipients->rowCount(); r++){
        QString name = ui->tableWidget_recipients->item(r, 0)->text();
        QString email = ui->tableWidget_recipients->item(r, 1)->text();

        p_recipients->insert(name, email);
    }

    accepted = true;
}

void PresetRecipientsDialog::displayNotification(QString message){
    QMessageBox Msgbox;
    Msgbox.setText(message);
    Msgbox.exec();
}

void PresetRecipientsDialog::on_tableWidget_recipients_cellClicked(int row, int column)
{
    if(column == 2){
        qDebug() << "Delete row:" << row;
        ui->tableWidget_recipients->removeRow(row);
    }
}

