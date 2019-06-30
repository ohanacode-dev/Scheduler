#include "servercomms.h"
#include <QDebug>
#include "globalvars.h"
#include <QJsonDocument>
#include <QJsonArray>
#include <QJsonObject>
#include <QJsonValue>

#define JSON_KEY_SENDER_NAME        "sender_name"
#define JSON_KEY_SENDER_EMAIL       "sender_email"
#define JSON_KEY_RECIPIENT_NAME     "recipient_name"
#define JSON_KEY_RECIPIENT_EMAIL    "recipient_email"
#define JSON_KEY_MESSAGE            "message"
#define JSON_KEY_TIME               "time"
#define JSON_KEY_DATE               "date"
#define JSON_KEY_APPOINTMENTS       "appointments"


#define ERR_EMAIL_NOT_FOUND         10
#define ERR_EMAIL_NOT_CONFIRMED     11
#define ERR_PASSWORD_MISSMATCH      12
#define ERR_EMAIL_INVALID           13
#define ERR_PASS_BAD_FORMAT         14
#define ERR_NAME_TOO_SHORT          15
#define ERR_TOKEN_INVALID           16
#define ERR_UNAUTHORIZED            17
#define ERR_OPERATION_INVALID       18
#define ERR_NOT_IMPLEMENTED         19
#define ERR_EMAIL_EXISTS            20
#define ERR_BAD_DATE_FORMAT         21
#define ERR_BAD_TIME_FORMAT         22
#define ERR_UNAVAILABLE             23



ServerComms::~ServerComms()
{
    delete networkManager;
}

ServerComms::ServerComms(QMap<QString, QMap<QString, QStringList>> *appointmentResultMap):
    p_appointmentMap(appointmentResultMap)
{
    /* Setup state machine and its states */
    m_StateMachine = new QStateMachine(this);
    Q_CHECK_PTR(m_StateMachine);

    m_stateStart = new QState();
    Q_CHECK_PTR(m_stateStart);

    m_stateLogin = new QState();
    Q_CHECK_PTR(m_stateLogin);

    m_stateExecuteNextQueueItem = new QState();
    Q_CHECK_PTR(m_stateExecuteNextQueueItem);

    bool ok = connect(m_stateStart, SIGNAL(entered()), this, SLOT(initServerCommunication()));
    Q_ASSERT(ok);

    ok = connect(m_stateLogin, SIGNAL(entered()), this, SLOT(loginToServer()));
    Q_ASSERT(ok);

    ok = connect(m_stateExecuteNextQueueItem, SIGNAL(entered()), this, SLOT(processOperationQueue()));
    Q_ASSERT(ok);

    /* Add states to the state machine */
    m_StateMachine->addState(m_stateStart);
    m_StateMachine->addState(m_stateLogin);
    m_StateMachine->addState(m_stateExecuteNextQueueItem);

    /* Setup state transitions */
    m_stateStart->addTransition(this, SIGNAL(signalLogin()), m_stateLogin);

    m_stateLogin->addTransition(this, SIGNAL(signalDone()), m_stateExecuteNextQueueItem);
    m_stateLogin->addTransition(this, SIGNAL(signalStartStateMachine()), m_stateStart);    

    m_stateExecuteNextQueueItem->addTransition(this, SIGNAL(signalStartStateMachine()), m_stateStart);

    m_StateMachine->setInitialState(m_stateStart);

    ok = connect(this, SIGNAL(signalDone()),  this, SLOT(processOperationQueue()));
    Q_ASSERT(ok);

    /* Setup network manager */
    networkManager = new QNetworkAccessManager(this);
    QObject::connect(networkManager, SIGNAL(finished(QNetworkReply*)), this, SLOT(networkManagerFinished(QNetworkReply*)));

    globals = GlobalVars::getInstance();

    m_restartCommsTimer = new QTimer(this);
    Q_CHECK_PTR(m_restartCommsTimer);
    ok = connect(m_restartCommsTimer, SIGNAL(timeout()),  this, SLOT(restartComms()));
    Q_ASSERT(ok);

}

void ServerComms::restartStateMachine()
{   
    if(!m_StateMachine->isRunning()){
        m_StateMachine->start();
    }    

    emit signalStartStateMachine();
}

void ServerComms::restartComms()
{
    qDebug() << "Line:" << __LINE__ << "Restarting server comms";

    serverRetries = 0;
    restartStateMachine();
}


void ServerComms::initServerCommunication()
{

    if(!globals->validateEmail(globals->email)){
        // No valid email. Ask user to setup the account.
        emit signalSetupUserAccount();
        return;
    }

    emit signalStatus("Sync in progress");

    if(globals->token.length() < 3){
        // No token. Need to login.
        emit signalLogin();
        return;
    }
}

void ServerComms::loginToServer()
{
    queueItem item;
    item.queryUrl = "?operation=login&email=" + globals->email + "&pass=" + globals->password;
    item.operation = serverOperation::LOGIN;

    m_operationQueue.append(item);
    processOperationQueue();
}

void ServerComms::updateAppointments()
{
    queueItem item;
    item.queryUrl = "?operation=get_app&token=" + globals->token + "&date=" + globals->selectedDate;
    item.operation = serverOperation::UPDATE;

    m_operationQueue.append(item);
    qDebug() << __FILE__ << __LINE__ << "processOperationQueue" ;
    processOperationQueue();
}

void ServerComms::setAppointment(QString toEmail, QString time, QString text)
{
    queueItem item;
    item.queryUrl = "?operation=set_app&token=" +globals->token + "&date=" + globals->selectedDate + "&body=" + text + "&email=" + toEmail + "&time=" + time;
    item.operation = serverOperation::ADD;

    m_operationQueue.append(item);
    qDebug() << __FILE__ << __LINE__ << "processOperationQueue" ;

    processOperationQueue();
}


void ServerComms::processOperationQueue()
{
    if(m_op != serverOperation::NONE){
        qDebug() << __FILE__ << __LINE__ << "processOperationQueue serverOperation in progress.";
        return;
    }

    if(m_repeatLastOperationFlag){
        qDebug() << __FILE__ << __LINE__;
        m_repeatLastOperationFlag = false;
        m_op = m_currentItem.operation;
        makeUrlRequest(m_currentItem.queryUrl);

    }else if(m_operationQueue.length() > 0){
        // Execute next operation
        m_currentItem = m_operationQueue.takeFirst();
        m_op = m_currentItem.operation;
        makeUrlRequest(m_currentItem.queryUrl);

    }else if(m_currentItem.operation != serverOperation::UPDATE){
        // Finish always by updating appointments.
        m_currentItem.operation = serverOperation::UPDATE;
        m_currentItem.queryUrl = "?operation=get_app&token=" + globals->token + "&date=" + globals->selectedDate;
        m_op = m_currentItem.operation;
        makeUrlRequest(m_currentItem.queryUrl);

    }else{
        qDebug() << "COMMS GOING IDLE";
    }
}

void ServerComms::makeUrlRequest(QString relativeUrl){
    qDebug() << __FILE__ << __LINE__ << "makeUrlRequest:" << relativeUrl;
    request.setUrl(QUrl(BASE_SERVER_URL + relativeUrl));
    networkManager->get(request);
}


void ServerComms::networkManagerFinished(QNetworkReply *reply)
{
    if (reply->error()) {
        qDebug() << "Line:" << __LINE__ << "SERVER_RESPONSE_ERROR:" << reply->errorString();

        m_repeatLastOperationFlag = true;
        serverRetries++;

        if(serverRetries < MAX_RETRIES){
            restartStateMachine();
        }else{
            emit(signalStatus("Server error!"));
            m_restartCommsTimer->start(SERVER_COMMS_RESET_TIMEOUT);
        }
        return;
    }

    serverRetries = 0;
    QString answer = reply->readAll();

    //qDebug() << __FILE__ << __LINE__ << "response:" << answer;
    /* process response */
    try {
        if (answer.startsWith("OK")) {
            answer.remove(0, 3);

            switch(m_op)
            {
                case serverOperation::LOGIN:{
                    qDebug() << __FILE__ << __LINE__ << "m_op LOGIN";

                    bool successFlag = false;

                    if (answer.contains("role=")) {
                        globals->role = answer.split("role=")[1].split(" ")[0].replace(" ", "");
                        successFlag = true;
                    }

                    if (successFlag && answer.contains("name=")) {
                        globals->name = answer.split("name=")[1].split("' ")[0].replace("'", "");
                        successFlag = true;
                    }

                    if (successFlag && answer.contains("token=")) {
                        globals->token = answer.split("token=")[1].split(" ")[0];

                    } else {
                        emit signalStatus("Server error!");
                        qDebug() << "Line:" << __LINE__ << "UNEXPECTED RESPONSE:" << answer;
                    }                    

                }break;

                case serverOperation::UPDATE:{
                    qDebug() << __FILE__ << __LINE__ << "m_op UPDATE";

                    if(p_appointmentMap != Q_NULLPTR){
                        answer.replace("'", "\"");
                        //qDebug() << "Line:" << __LINE__ << "SERVER RESPONSE" << answer;

                        QJsonDocument jsonDoc = QJsonDocument::fromJson(answer.toUtf8());
                        QJsonObject jsonObject = jsonDoc.object();
                        QString responseDate = jsonObject.value(JSON_KEY_DATE).toString();
                        QJsonArray appointments = jsonObject.value(JSON_KEY_APPOINTMENTS).toArray();

                        QMap<QString, QStringList> val;

                        QString recipientName = globals->name;
                        QString recipientEmail = globals->email;

                        foreach (const QJsonValue & app, appointments){
                            if(globals->role.compare(ROLE_MULTICLIENT) == 0){
                                recipientName = app.toObject().value(JSON_KEY_RECIPIENT_NAME).toString();
                                recipientEmail = app.toObject().value(JSON_KEY_RECIPIENT_EMAIL).toString();
                            }

                            QStringList appData = QStringList()
                                    << app.toObject().value(JSON_KEY_MESSAGE).toString()
                                    << recipientName + "\n(" + recipientEmail + ")"
                                    << app.toObject().value(JSON_KEY_SENDER_NAME).toString() + "\n(" + app.toObject().value(JSON_KEY_SENDER_EMAIL).toString() + ")";

                            QString appTime = app.toObject().value(JSON_KEY_TIME).toString();

                            if(val.contains(appTime)){
                                val.insert(appTime, val[appTime] << appData);
                            }else{
                                val.insert(appTime, appData);
                            }
                        }

                        /* We will only ger server response for one date at a time. Clear existing data and then add received data.*/
                        p_appointmentMap->remove(responseDate);
                        p_appointmentMap->insert(responseDate, val);

                        m_repeatLastOperationFlag = false;
                        emit signalUpdateDone();
                    }

                    emit signalStatus("Done.");

                }break;

                case serverOperation::ADD:{
                    qDebug() << __FILE__ << __LINE__ << "m_op ADD";
                    m_repeatLastOperationFlag = false;

                }break;

                default:
                    m_repeatLastOperationFlag = false;
                    break;
            }

            m_op = serverOperation::NONE;
            emit signalDone();
        }else{
            m_op = serverOperation::NONE;
            processServerError(answer);
        }

    } catch (...) {
        m_op = serverOperation::NONE;
        qDebug() << __FILE__ << __LINE__ << "Error parsing server response:" << answer;
    }

}

void ServerComms::processServerError(QString message)
{
    try {
        QString errorTextAndCode = message.split(":")[0];
        quint32 errorCode = (errorTextAndCode.split(" ")[1]).toUInt();

        QString userMessage = "";

        switch (errorCode) {
            case ERR_TOKEN_INVALID:
                qDebug() << __FILE__ << __LINE__;
                m_repeatLastOperationFlag = false;
                globals->token = "";
                restartStateMachine();
                break;

            case ERR_UNAUTHORIZED:
                emit signalNotification("ERROR: The appointment you requested is not available.");
                break;

            case ERR_BAD_DATE_FORMAT:
                qDebug() <<  __FILE__ << __LINE__ << "Bad date format";
                break;

            case ERR_BAD_TIME_FORMAT:
                qDebug() <<  __FILE__ << __LINE__ << "Bad time format";
                break;

            case ERR_EMAIL_NOT_FOUND:
                emit signalStatus("ERROR: Account email not found. Did you register?");
                emit signalNotification("ERROR: Account email not found. Did you register?");
                break;

            case ERR_EMAIL_NOT_CONFIRMED:
                emit signalStatus("ERROR: Account not confirmed. Please check your inbox.");
                emit signalNotification("ERROR: Your email account is not yet confirmed. Please check your inbox. You should receive an email with an activation link.");
                break;

            case ERR_PASSWORD_MISSMATCH:
                emit signalStatus("ERROR: Password missmatch.");
                emit signalNotification("ERROR: The password you enetered is not valid. Please check your password or request a reset.");
                break;

            case ERR_EMAIL_INVALID:
                emit signalStatus("ERROR: Invalid email.");
                emit signalNotification("ERROR: The email you provided was not found on the server. Please check your account settings, or register a new one.");
                break;

            case ERR_UNAVAILABLE:
                emit signalStatus("ERROR: Appointment already scheduled.");
                emit signalAppointmentOccupied();
                break;

            default:
                qDebug() << __FILE__ << __LINE__ << message;
                emit signalStatus("ERROR: Sync failed.");
                break;
        }

    } catch (...) {
        qDebug() << "Line:" << __LINE__ << message;
        emit signalStatus("ERROR: Sync failed.");
    }
}

