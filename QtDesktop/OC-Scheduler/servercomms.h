#ifndef SERVERCOMMS_H
#define SERVERCOMMS_H

#include <QWidget>
#include <QStateMachine>
#include <QState>
#include <QFinalState>
#include <QtNetwork/QNetworkAccessManager>
#include <QtNetwork/QNetworkRequest>
#include <QtNetwork/QNetworkReply>
#include "globalvars.h"
#include <QTimer>

#define MAX_RETRIES                 3
#define SERVER_COMMS_RESET_TIMEOUT  (1000 * 60 * 5) /* retry every 5 mins */

class ServerComms : public QWidget
{
    Q_OBJECT
public:
    ServerComms(QMap<QString, QMap<QString, QStringList>> *appointmentResultMap);
    ~ServerComms();


signals:
    void signalStatus(QString);
    void signalNotification(QString);
    void signalAppointmentOccupied();
    void signalSetupUserAccount();
    void signalLogin();    
    void signalUpdateAppointments();
    void signalUpdateDone();
    void signalDone();
    void signalStartStateMachine();

public slots:
    void restartStateMachine();
    void setAppointment(QString toEmail, QString time, QString text);
    void updateAppointments();
    void processOperationQueue();
    void loginToServer();

private slots:
    void initServerCommunication();


    void networkManagerFinished(QNetworkReply *reply);
    void makeUrlRequest(QString relativeUrl);
    void restartComms();



private:


    void processServerError(QString message);

    enum class serverOperation
    {
        NONE,
        LOGIN,
        UPDATE,
        ADD
    };

    struct queueItem
    {
        QString queryUrl;
        serverOperation operation;
    };

    queueItem m_currentItem;
    QList <queueItem> m_operationQueue;

    quint8 serverRetries;

    QNetworkAccessManager *networkManager;
    QNetworkRequest request;

    QStateMachine * m_StateMachine;
    QState * m_stateExecuteNextQueueItem;
    QState * m_stateStart;
    QState * m_stateLogin;

    serverOperation m_op = serverOperation::NONE;
    GlobalVars* globals;
    bool m_repeatLastOperationFlag = false;

    QTimer* m_restartCommsTimer;
    QMap<QString, QMap<QString, QStringList>>* p_appointmentMap;

};

#endif // SERVERCOMMS_H
