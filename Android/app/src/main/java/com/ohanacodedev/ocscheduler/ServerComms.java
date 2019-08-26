package com.ohanacodedev.ocscheduler;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;


/* implements server communication and response parsing. */
public class ServerComms  extends AsyncTask<String, Integer, String> {
    private static final String TAG = "ServerComms";

    private static final String apiUrl = "https://api.scheduleservice.ohanacode-dev.com";

    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 1;

    private static final String ERR_EMAIL_NOT_FOUND = "10";
    private static final String ERR_EMAIL_NOT_CONFIRMED = "11";
    private static final String ERR_PASSWORD_MISSMATCH = "12";
    private static final String ERR_EMAIL_INVALID = "13";
    private static final String ERR_PASS_BAD_FORMAT = "14";
    private static final String ERR_NAME_TOO_SHORT = "15";
    private static final String ERR_TOKEN_INVALID = "16";
    private static final String ERR_UNAUTHORIZED = "17";
    private static final String ERR_OPERATION_INVALID = "18";
    private static final String ERR_NOT_IMPLEMENTED = "19";
    private static final String ERR_EMAIL_EXISTS = "20";
    private static final String ERR_BAD_DATE_FORMAT = "21";
    private static final String ERR_BAD_TIME_FORMAT = "22";

    private static final String JSON_KEY_SENDER_NAME = "sender_name";
    private static final String JSON_KEY_SENDER_EMAIL = "sender_email";
    private static final String JSON_KEY_RECIPIENT_NAME = "recipient_name";
    private static final String JSON_KEY_RECIPIENT_EMAIL = "recipient_email";
    private static final String JSON_KEY_MESSAGE = "message";
    private static final String JSON_KEY_TIME = "time";
    private static final String JSON_KEY_DATE = "date";
    private static final String JSON_KEY_APPOINTMENTS = "appointments";
    private static final String JSON_KEY_ID = "id";
    private static final int STACK_TRACE_LEVELS_UP = 3;

    private int status = STATUS_OK;

    /* the handle of the main activity so we can use its public methods. */
    private final WeakReference<MainActivity> mainAct;

    /* Class constructor to get the handle of the main activity. */
    public ServerComms(MainActivity activity_name){
        mainAct = new WeakReference<>(activity_name);
    }

    /* This runs on its own thread in the background. */
    protected String doInBackground(String... urls) {
        /* Set the connection flag so the state machine in the main activity will wait until this query is finished. */
        GlobalVars.connection_in_progress = true;
        status = STATUS_OK;

        try{
            /* Create the full url */
            URL url = new URL(apiUrl + urls[0]);

            Log.d(TAG, "Q URL:" + url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int status = connection.getResponseCode();
            BufferedReader rd;
            if (status != HttpURLConnection.HTTP_OK) {
                rd = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }else {
                rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                content.append(line).append("\n");
            }

            return content.toString();
        }catch(Exception ex){
            Log.e(TAG, getLineNumber() + ex.getMessage());
            status = STATUS_ERROR;
            return "";
        }
    }

    public static String getLineNumber() {
        return "Line:" + String.valueOf(Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getLineNumber()) + ":";
    }

    protected void onProgressUpdate(Integer... progress) {  }

    /* Runs after the response has been received from the server. */
    protected void onPostExecute(String response) {
        Log.i(TAG, "RESPONSE:" + response);
        /* Clear the query flag so the main activity can make a new one if pending. */
        GlobalVars.connection_in_progress = false;

        try {
            if (status != STATUS_OK) {
                /* We failed to communicate. Try again. */
                if(GlobalVars.connection_retries < GlobalVars.maxretries){
                    mainAct.get().restartStateMachine();
                    GlobalVars.connection_retries++;
                }else{
                    /* failed too many times. Stop the state machine. */
                    mainAct.get().setStatus(mainAct.get().getString(R.string.server_error));
                    GlobalVars.m_operation = GlobalVars.OP_STOP;
                }
            } else {
                GlobalVars.connection_retries = 0;

                if (response.startsWith("OK")) {
                    switch (GlobalVars.m_lastOperation){

                        case GlobalVars.OP_SIGNUP: {

                            GlobalVars.m_operation = GlobalVars.OP_STOP;
                            mainAct.get().notify(mainAct.get().getString(R.string.activate_email));
                            mainAct.get().setStatus(mainAct.get().getString(R.string.waiting_for_email_activation));
                        }
                        break;

                        case GlobalVars.OP_LOGIN: {
                            if (response.contains("role=")) {
                                GlobalVars.account_role = response.split("role=")[1].split(" ")[0].trim();
                            }

                            if (response.contains("token=")) {
                                GlobalVars.account_token = response.split("token=")[1].split(" ")[0];
                                GlobalVars.m_operation = GlobalVars.OP_UPDATE;
                            } else {
                                GlobalVars.m_operation = GlobalVars.OP_STOP;
                                Log.e(TAG, getLineNumber() + "UNEXPECTED RESPONSE:" + response);
                                mainAct.get().setStatus(mainAct.get().getString(R.string.sync_failed));
                            }
                        }
                        break;

                        case GlobalVars.OP_UPDATE: {
                            mainAct.get().clearAppointmentList();

                            /* The response may have arrived late, so we need to check the currently selected date. */
                            String selectedDate = mainAct.get().formatQueryDate();

                            try {
                                JSONObject jsonResponse = new JSONObject(response.substring(3));

                                if(selectedDate.equals(jsonResponse.getString(JSON_KEY_DATE))){

                                    JSONArray appointments = jsonResponse.getJSONArray(JSON_KEY_APPOINTMENTS);

                                    for (int i = 0; i < appointments.length(); i++) {
                                        JSONObject jsonApp = new JSONObject(appointments.getString(i));

                                        if(jsonApp.getString(JSON_KEY_MESSAGE).length() > 2) {
                                            /* Message appears not to be empty */
                                            String sender = jsonApp.getString(JSON_KEY_SENDER_NAME) + "(" + jsonApp.getString(JSON_KEY_SENDER_EMAIL) + ")";
                                            String recipient = jsonApp.getString(JSON_KEY_RECIPIENT_NAME) + "(" + jsonApp.getString(JSON_KEY_RECIPIENT_EMAIL) + ")";
                                            String selectedSender = mainAct.get().getSelectedSender();
                                            String selectedRecipient = mainAct.get().getSelectedRecipient();

                                            if ((selectedSender.equals(mainAct.get().getString(R.string.all_senders)) || selectedSender.equals(sender))
                                                && (selectedRecipient.equals(mainAct.get().getString(R.string.all_recipients)) || selectedRecipient.equals(recipient))){
                                                String appointment = jsonApp.getString(JSON_KEY_TIME) + ": " + jsonApp.getString(JSON_KEY_MESSAGE);
                                                mainAct.get().addToAppointments(appointment, jsonApp.getString(JSON_KEY_ID));
                                            }
                                            mainAct.get().addSender(sender);
                                            mainAct.get().addRecipient(recipient);
                                        }
                                    }

                                    mainAct.get().updateUi();
                                    mainAct.get().setStatus(mainAct.get().getString(R.string.sync_success));
                                }else{
                                    /* We got an outdated response. Try again. */
                                    mainAct.get().clearAppointmentList();
                                    mainAct.get().updateUi();
                                    mainAct.get().restartStateMachine();
                                }

                            } catch (Exception e) {
                                Log.e(TAG, getLineNumber() + e.getMessage());
                                mainAct.get().setStatus(mainAct.get().getString(R.string.server_error));
                            }
                        }break;

                        case GlobalVars.OP_ADD: {
                            GlobalVars.m_operation = GlobalVars.OP_UPDATE;
                        }
                        break;

                        default:
                            processError(response);
                            break;
                    }
                }else{
                    processError(response);
                }
            }
        }catch(Exception e){
            Log.e(TAG, getLineNumber() + e.getMessage());
        }
    }

    /* Processes errors if received in the server response. */
    private void processError(String response){
        mainAct.get().stopStateMachine();
//        GlobalVars.m_operation = GlobalVars.OP_STOP;

        try {
            String errorTextAndCode = response.split(":")[0];
            String errorCode = errorTextAndCode.split(" ")[1];

            switch (errorCode) {
                case ERR_TOKEN_INVALID:
                    GlobalVars.m_operation = GlobalVars.OP_LOGIN;
                    Log.e(TAG, "Token expired. Logging in again");
                    break;

                case ERR_UNAUTHORIZED:
                    mainAct.get().setStatus(mainAct.get().getString(R.string.unauthorized_account));
                    break;

                case ERR_BAD_DATE_FORMAT:
                    Log.e(TAG, "Bad date format");
                    mainAct.get().setStatus(mainAct.get().getString(R.string.bad_date));
                    break;

                case ERR_BAD_TIME_FORMAT:
                    Log.e(TAG, "Bad time format");
                    mainAct.get().setStatus(mainAct.get().getString(R.string.bad_time));
                    break;

                case ERR_EMAIL_NOT_FOUND:
                    GlobalVars.m_operation = GlobalVars.OP_SIGNUP;
                    break;

                case ERR_EMAIL_NOT_CONFIRMED:
                    mainAct.get().notify(mainAct.get().getString(R.string.unactivated_email));
                    mainAct.get().setStatus(mainAct.get().getString(R.string.waiting_for_email_activation));
                    break;

                case ERR_PASSWORD_MISSMATCH:
                    mainAct.get().notify(mainAct.get().getString(R.string.password_missmatch));
                    mainAct.get().setStatus(mainAct.get().getString(R.string.sync_failed));
                    break;

                case ERR_EMAIL_INVALID:
                    mainAct.get().notify(mainAct.get().getString(R.string.invalid_email));
                    break;

                case ERR_PASS_BAD_FORMAT:
                    mainAct.get().notify(mainAct.get().getString(R.string.invalid_password));
                    break;

                case ERR_NAME_TOO_SHORT:
                    mainAct.get().notify(mainAct.get().getString(R.string.invalid_name));
                    break;

                case ERR_EMAIL_EXISTS:
                    mainAct.get().notify(mainAct.get().getString(R.string.email_exists));
                    break;

                default:
                    Log.e(TAG, "UNEXPECTED RESPONSE:" + response);
                    mainAct.get().setStatus(mainAct.get().getString(R.string.sync_failed));
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, getLineNumber() + e.getMessage());
            Log.e(TAG, "UNEXPECTED RESPONSE:" + response);
            if (mainAct.get() != null) {
                mainAct.get().setStatus(mainAct.get().getString(R.string.sync_failed));
            }

        }
    }


}
