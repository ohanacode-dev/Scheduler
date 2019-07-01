#include "application.h"
#include "ui_application.h"
#include <QDebug>
#include <QDate>
#include <QMessageBox>
#include <QSettings>
#include "accountsetupdialog.h"
#include "globalvars.h"
#include <QTableWidgetItem>
#include <QDir>
#include <QCheckBox>
#include <QDesktopServices>

Application::Application(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::Application)
{    
    /* Setup the UI components */
    ui->setupUi(this);

    /* Get globals instance so we can set global parameters */
    globals = GlobalVars::getInstance();
    globals->selectedDate = QDate::currentDate().toString("dd.MM.yyyy");

    /* Load saved settings if any. */
    QString settingsLocation = QStandardPaths::writableLocation(QStandardPaths::ConfigLocation);
    QDir settingsDir(settingsLocation);
    if (!settingsDir.exists()){
        settingsDir.mkdir(".");
    }

    m_settingsFile = settingsLocation + SETTINGS_FILE_NAME;
    loadSettings();

    /* Continuing with UI now that we have the locale setup */
    ui->calendarWidget->setMinimumDate(QDate::currentDate());
    ui->calendarWidget->setSelectedDate(QDate::currentDate());

    ui->comboBox_nameList->setDisabled(true);
    ui->lineEdit_email->setDisabled(true);    
    ui->lineEdit_email->setText(globals->email);
    ui->lineEdit_message->setDisabled(true);
    ui->timeEdit->setDisplayFormat("HH:mm");
    ui->tableWidget_apointments->setColumnCount(5);
    ui->tableWidget_apointments->setSortingEnabled(true);
    ui->tableWidget_apointments->setWordWrap(true);
    ui->tableWidget_apointments->setHorizontalHeaderLabels(QStringList() << tr("Time") << tr("Message") << tr("To user") << tr("From") << "");

    m_tableDataColumnCount = ui->tableWidget_apointments->columnCount() - 1;

    QHeaderView* header = ui->tableWidget_apointments->horizontalHeader();
    header->setDefaultSectionSize(160);
    header->setSectionResizeMode(1, QHeaderView::Stretch);

    ui->tableWidget_apointments->setColumnWidth(4, 1);

    /* Setup the server communication state machine */
    m_comms = new ServerComms(&appointmentMap);

    bool ok = connect(m_comms, SIGNAL(signalStatus(QString)), this, SLOT(displayStatus(QString)));
    Q_ASSERT(ok);

    ok = connect(m_comms, SIGNAL(signalNotification(QString)), this, SLOT(displayNotification(QString)));
    Q_ASSERT(ok);

    ok = connect(m_comms, SIGNAL(signalSetupUserAccount()), this, SLOT(on_actionAccount_triggered()));
    Q_ASSERT(ok);

    ok = connect(m_comms, SIGNAL(signalUpdateDone()), this, SLOT(refreshUi()));
    Q_ASSERT(ok);

    ok = connect(m_comms, SIGNAL(signalAppointmentOccupied()), this, SLOT(notifyAppOccupied()));
    Q_ASSERT(ok);

    ok = connect(m_comms, SIGNAL(signalNewVersionAvailable(QString)), this, SLOT(notifyNewVersionAvailable(QString)));
    Q_ASSERT(ok);

    QActionGroup* langGroup = new QActionGroup(ui->menuLanguage);
    langGroup->setExclusive(true);
    ok = connect(langGroup, SIGNAL (triggered(QAction *)), this, SLOT (slotLanguageChanged(QAction *)));
    Q_ASSERT(ok);

    // format systems language
    m_langPath = QApplication::applicationDirPath();
    m_langPath.append("/languages");
    QDir dir(m_langPath);
    QStringList fileNames = dir.entryList(QStringList("Translation_*.qm"));

    for (int i = 0; i < fileNames.size(); ++i) {
        // get locale extracted by filename
        QString locale;
        locale = fileNames[i]; // "Translation_de.qm"
        locale.truncate(locale.lastIndexOf('.')); // "Translation_en"
        locale.remove(0, locale.indexOf('_') + 1); // "en"

        QString lang = QLocale::languageToString(QLocale(locale).language());
        QIcon ico(QString("%1/%2.png").arg(m_langPath).arg(locale));

        QAction *action = new QAction(ico, lang, this);
        action->setCheckable(true);
        action->setData(locale);

        ui->menuLanguage->addAction(action);
        langGroup->addAction(action);

        // set default translators and language checked
        if (m_currLang == locale)
        {
            action->setChecked(true);
        }       
    }

    QLocale locale = QLocale(m_currLang);
    QLocale::setDefault(locale);
    QString languageName = QLocale::languageToString(locale.language());
    switchTranslator(m_translator, QString("Translation_%1.qm").arg(m_currLang));
    switchTranslator(m_translatorQt, QString("qt_%1.qm").arg(m_currLang));

    /* Check if we have valid account credentials */
    if((globals->password.length() < 6) || !globals->validateEmail(globals->email)){
        on_actionAccount_triggered();
    }

    m_comms->restartStateMachine();

    if(globals->checkForUpdates){
        m_comms->checkForUpdates();
    }
}

Application::~Application()
{
    saveSettings();
    delete globals;
    delete ui;
}

void Application::deserializeRecipients(QString data)
{
    if(data.length() > 3){
        QString separator = QString(SEPARATOR);
        QStringList recList = data.split(separator);
        recipients.clear();
        ui->comboBox_nameList->clear();

        for ( const auto& record : recList  )
        {
            QStringList nameAndEmail = record.split("':'");
            QString email = nameAndEmail[0].replace("'", "");
            QString name = nameAndEmail[1].replace("'", "");
            recipients.insert(email, name);
            ui->comboBox_nameList->insertItem(0, name + " (" + email + ")");
        }
    }
}

QString Application::serializeRecipients()
{
    QString retVal;
    QString separator = QString(SEPARATOR);
    QMapIterator<QString, QString> i(recipients);

    QString currentRecipient = ui->comboBox_nameList->currentText();

    ui->comboBox_nameList->clear();

    while (i.hasNext()) {
        i.next();
        if(i.value().length() > 2){
            retVal += separator + "'" + i.key() + "':'" + i.value() + "'";
        }
        ui->comboBox_nameList->insertItem(0, i.value() + " (" + i.key() + ")");
    }

    int curRecipientIdx = ui->comboBox_nameList->findText(currentRecipient);
    if( curRecipientIdx != -1){
        ui->comboBox_nameList->setCurrentIndex(curRecipientIdx);
    }

    retVal.remove(0, separator.length());
    return retVal;
}


void Application::loadSettings()
{
    QSettings settings(m_settingsFile, QSettings::IniFormat);
    QString saveAccountFlag = settings.value("save_acc", "No").toString();
    m_currLang = settings.value("locale", LOCALE_EN).toString();

    if(saveAccountFlag.compare("Yes", Qt::CaseInsensitive) == 0)
    {        
        globals->email = settings.value("email", "").toString();
        globals->password = settings.value("pass", "").toString();
        rememberCredentialsFlag = true;

        QString serializedRecipientList = settings.value("recipient_list", "").toString();
        deserializeRecipients(serializedRecipientList);
    }

    QString checkUpdatesFlag = settings.value("check_updates", "Yes").toString();
    globals->checkForUpdates = (checkUpdatesFlag.compare("Yes", Qt::CaseInsensitive) == 0);

}

void Application::saveSettings()
{
    QSettings settings(m_settingsFile, QSettings::IniFormat);

    QString saveAccountFlag = "No";
    if(rememberCredentialsFlag){
        saveAccountFlag = "Yes";
    }

    QString checkUpdatesFlag = "No";
    if(globals->checkForUpdates){
        checkUpdatesFlag = "Yes";
    }

    QString serializedRecipientList = serializeRecipients();
    settings.setValue("recipient_list", serializedRecipientList);
    settings.setValue("save_acc", saveAccountFlag);
    settings.setValue("email", globals->email);
    settings.setValue("pass", globals->password);
    settings.setValue("locale", m_currLang);
    settings.setValue("check_updates", checkUpdatesFlag);
}

void Application::displayStatus(QString message){
    ui->statusBar->showMessage(message);
}

void Application::displayNotification(QString message){
    QMessageBox Msgbox;
    Msgbox.setText(message);
    Msgbox.exec();
}

void Application::notifyNewVersionAvailable(QString message){
    QMessageBox msgBox;
    msgBox.setText(tr("A new version is available. Do you wish to update?"));
    QPixmap pixmap = QPixmap(":/oc_logo.png");
    msgBox.setWindowIcon(QIcon(pixmap));

    QCheckBox *cb = new QCheckBox(tr("Do not check for new version again."));
    msgBox.setCheckBox(cb);

    msgBox.addButton(tr("No"), QMessageBox::NoRole);
    QAbstractButton* pButtonYes = msgBox.addButton(tr("Yes"), QMessageBox::YesRole);

    msgBox.exec();

    globals->checkForUpdates = (cb->checkState() == Qt::Unchecked);

    if (msgBox.clickedButton()==pButtonYes) {
        qDebug() << "Open browser at:" << message ;
        QDesktopServices::openUrl (QUrl(message));
        QApplication::quit();
    }
}

void Application::on_actionAbout_triggered()
{
    QMessageBox Msgbox;
    Msgbox.setText(tr("Author: Rada Berar\nCompany: Ohana Code Development\nEmail: rada.berar@ohanacode-dev.com"));
    QPixmap pixmap = QPixmap(":/oc_logo.png");
    Msgbox.setWindowIcon(QIcon(pixmap));
    Msgbox.exec();
}


void Application::on_actionAccount_triggered()
{
    AccountSetupDialog* accountSetupDialog = new AccountSetupDialog(rememberCredentialsFlag, this);
    accountSetupDialog->exec();

    if(accountSetupDialog->result()){
        rememberCredentialsFlag = accountSetupDialog->rememberCredentials;
    }

    ui->lineEdit_email->setText(globals->email);

    globals->token = "";
    m_comms->restartStateMachine();

}

void Application::on_calendarWidget_selectionChanged()
{
    calendarJustChangedFlag = true;
    globals->selectedDate = ui->calendarWidget->selectedDate().toString("dd.MM.yyyy");
    m_comms->updateAppointments();
}

void Application::refreshUi()
{
    /* Check if we have a user name available */
    if(globals->name.length() > 2){
        // Logged in
        ui->lineEdit_message->setDisabled(false);
        if(ui->comboBox_nameList->findText(globals->name + " (" + globals->email + ")") == -1){
            /* User name is not yet in the list. Add it. */
            recipients.insert(globals->email, globals->name);
            saveSettings();
        }

        /* Check account role */
        if(globals->role.compare(ROLE_MULTICLIENT) == 0){
            ui->comboBox_nameList->setDisabled(false);
            ui->lineEdit_email->setDisabled(false);
        }
    }
    /* Clear appointment table */
    ui->tableWidget_apointments->setRowCount(0);

    /* Populate with latest query results. */
    QMapIterator<QString, QStringList> i(appointmentMap[globals->selectedDate]);

    while (i.hasNext()) {
        i.next();
        int rowCount = i.value().length() / (m_tableDataColumnCount - 1);

        int vIdx = 0;
        for(int r = 1; r <= rowCount; r++){
            int rowId = ui->tableWidget_apointments->rowCount();
            ui->tableWidget_apointments->insertRow(rowId);

            ui->tableWidget_apointments->setItem( rowId, 0, new QTableWidgetItem(i.key()));
            for (int c = 1; c < m_tableDataColumnCount; c++){
                ui->tableWidget_apointments->setItem( rowId, c, new QTableWidgetItem(i.value().at(vIdx)));
                vIdx++;
            }

            QTableWidgetItem* item = new QTableWidgetItem;
            item->setIcon(QIcon(":/close.png"));

            ui->tableWidget_apointments->setItem(rowId, m_tableDataColumnCount, item);
        }
    }

    ui->tableWidget_apointments->resizeRowsToContents();
    ui->tableWidget_apointments->update();
}


void Application::on_actionPresetRecipients_triggered()
{
    recipientDialog = new PresetRecipientsDialog(&recipients, this);
    recipientDialog->exec();   
    saveSettings();
}

void Application::on_comboBox_nameList_activated(const QString &arg1)
{
    QString email = arg1.split(" (")[1].replace(")", "");

    ui->lineEdit_email->setText(email);
}

void Application::on_btn_send_clicked()
{
    QString message = ui->lineEdit_message->text();
    QString time = ui->timeEdit->time().toString("HH:mm");
    QString toEmail = ui->lineEdit_email->text();

    m_comms->setAppointment(toEmail, time, message);
}

void Application::on_btn_AddRecipient_clicked()
{
    on_actionPresetRecipients_triggered();
}

void Application::on_calendarWidget_clicked(const QDate &date)
{
    Q_UNUSED(date);

    if(calendarJustChangedFlag){
        calendarJustChangedFlag = false;
    }else{
        m_comms->updateAppointments();
    }
}


void Application::slotLanguageChanged(QAction* action)
{
    if(Q_NULLPTR != action) {
        // load the language dependant on the action content
        loadLanguage(action->data().toString());
        setWindowIcon(action->icon());

        QString newLang = action->data().toString();
        if(newLang.compare("sr") == 0){
            ui->calendarWidget->setLocale(QLocale::Serbian);
        }else{
            ui->calendarWidget->setLocale(QLocale::English);
        }
    }
}

void Application::switchTranslator(QTranslator& translator, const QString& filename)
{
    // remove the old translator
    qApp->removeTranslator(&translator);

    // load the new translator
    QString path = QApplication::applicationDirPath();
    path.append("/languages/");
    if(translator.load(path + filename)) //Here Path and Filename has to be entered because the system didn't find the QM Files else
        qApp->installTranslator(&translator);
}

void Application::loadLanguage(const QString& rLanguage)
{
    if(m_currLang != rLanguage) {
        m_currLang = rLanguage;

        QLocale locale = QLocale(m_currLang);        
        QLocale::setDefault(locale);
        QString languageName = QLocale::languageToString(locale.language());
        switchTranslator(m_translator, QString("Translation_%1.qm").arg(rLanguage));
        switchTranslator(m_translatorQt, QString("qt_%1.qm").arg(rLanguage));
    }    
}

void Application::changeEvent(QEvent* event)
{
    if(Q_NULLPTR != event) {
        if(event->type() == QEvent::LanguageChange){
            ui->retranslateUi(this);
        }else if(event->type() == QEvent::LocaleChange){
            QString locale = QLocale::system().name();
            locale.truncate(locale.lastIndexOf('_'));
            loadLanguage(locale);            
        }
    }
    QMainWindow::changeEvent(event);
}

void Application::notifyAppOccupied()
{
    QMessageBox::StandardButton reply;
    reply = QMessageBox::question(this, tr("OC Scheduler"), tr("The appointment you requested is already occupied. Do you wish to overwrite it?"),
                                QMessageBox::Yes|QMessageBox::No);

    if (reply == QMessageBox::Yes) {
        QString message = ui->lineEdit_message->text();
        QString time = ui->timeEdit->time().toString("HH:mm");
        QString toEmail = ui->lineEdit_email->text();
        m_comms->setAppointment(toEmail, time, "");
        m_comms->setAppointment(toEmail, time, message);
        m_comms->processOperationQueue();
    }
}

void Application::on_tableWidget_apointments_cellClicked(int row, int column)
{
    QStringList selectedTime = ui->tableWidget_apointments->item(row, 0)->text().split(":");
    ui->timeEdit->setTime(QTime(selectedTime[0].toInt(), selectedTime[1].toInt()));

    QString selectedRecipient = ui->tableWidget_apointments->item(row, 2)->text().split("(")[1];
    selectedRecipient.chop(1);
    ui->lineEdit_email->setText(selectedRecipient);

    if(column == m_tableDataColumnCount){
        QString message = tr("Remove the appointment for ");
        message.append(selectedRecipient);
        message.append(tr(" at "));
        message.append(ui->timeEdit->text());
        message.append(" ?");

        QMessageBox::StandardButton reply;
        reply = QMessageBox::question(this, tr("OC Scheduler"), message, QMessageBox::Yes|QMessageBox::No);

        if (reply == QMessageBox::Yes) {
            QString time = ui->timeEdit->time().toString("HH:mm");
            QString toEmail = ui->lineEdit_email->text();
            m_comms->setAppointment(toEmail, time, "");
        }
    }
}
