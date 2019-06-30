#ifndef APPLICATION_H
#define APPLICATION_H

#include <QMainWindow>
#include <QStandardItemModel>
#include <QTranslator>
#include <QLibraryInfo>
#include "servercomms.h"
#include "globalvars.h"
#include "presetrecipientsdialog.h"
#include <QSettings>
#include <QStandardPaths>

namespace Ui {
class Application;
}

class Application : public QMainWindow
{
    Q_OBJECT

public:
    explicit Application(QWidget *parent = nullptr);
    ~Application();

public slots:

private slots:
    void on_actionAbout_triggered();
    void on_actionAccount_triggered();
    void on_calendarWidget_selectionChanged();
    void refreshUi();
    void displayStatus(QString message);
    void displayNotification(QString message);
    void on_actionPresetRecipients_triggered();
    void on_comboBox_nameList_activated(const QString &arg1);
    void on_btn_send_clicked();
    void on_btn_AddRecipient_clicked();
    void on_calendarWidget_clicked(const QDate &date);
    void notifyAppOccupied();

    // this slot is called by the language menu actions
    void slotLanguageChanged(QAction* action);

    void on_tableWidget_apointments_cellClicked(int row, int column);

private:
    Ui::Application *ui;
    QString m_settingsFile;

    bool rememberCredentialsFlag = false;
    ServerComms* m_comms = nullptr;

    void loadSettings();
    void saveSettings();
    void deserializeRecipients(QString data);
    QString serializeRecipients();

    GlobalVars* globals;

    /*    date  ,       time  ,  the rest */
    QMap<QString, QMap<QString, QStringList>> appointmentMap;

    PresetRecipientsDialog *recipientDialog;
    QMap<QString, QString> recipients;

    bool calendarJustChangedFlag = false;
    // loads a language by the given language shortcur (e.g. de, en)
    void loadLanguage(const QString& rLanguage);
    // creates the language menu dynamically from the content of m_langPath
    void createLanguageMenu(void);

    void switchTranslator(QTranslator& translator, const QString& filename);

    QTranslator m_translator; // contains the translations for this application
    QTranslator m_translatorQt; // contains the translations for qt
    QString m_currLang; // contains the currently loaded language
    QString m_langPath; // Path of language files. This is always fixed to /languages.

    int m_tableDataColumnCount = 4;

protected:
    // this event is called, when a new translator is loaded or the system language is changed
    void changeEvent(QEvent*);
};

#endif // APPLICATION_H
