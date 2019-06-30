package com.ohanacodedev.ocscheduler;

import android.graphics.Color;

/* Global variables to be used for inter-class communication. It was created to allow separating
server communication from the main activity class. */
public class GlobalVars {

    public static final int OP_IDLE = 0;
    public static final int OP_START = 1;
    public static final int OP_SIGNUP = 2;
    public static final int OP_LOGIN = 3;
    public static final int OP_UPDATE = 4;
    public static final int OP_ADD = 6;
    public static final int OP_STOP = 7;

    public static final String DIVIDER = "~~~";

    public static String account_email = "";
    public static String account_password = "";
    public static String account_alias = "";

    public static String account_role = "client";
    public static String addAppointmentTime = "";
    public static String addAppointmentText = "";
    public static String addAppointmentEmail = "";
    public static String account_token = "";
    public static String pending_request_url = "";
    public static int connection_retries = 0;
    public static boolean connection_in_progress = false;

    public static int m_operation = OP_IDLE;
    public static int m_lastOperation = OP_IDLE;
    public static final int maxretries = 3;

    public static String colorBg1;
    public static String colorBg2;
    public static String colorBg3;
    public static String colorText1;
    public static String colorText2;
    public static String colorText3;
    public static String colorText4;
}
