package com.ohanacodedev.ocscheduler;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String resetPasswordUrl = "http://scheduleservice.ohanacode-dev.com/resetpass";

    private static final String LOCALE_EN = "en";
    private static final String LOCALE_SR = "sr";
    private static final String ROLE_PENDING = "pending";
    private static final String ROLE_CLIENT = "client";
    private static final String ROLE_MULTICLIENT = "multiclient";
    private static final String ROLE_SUPERCLIENT = "superclient";

    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ALIAS = "alias";
    private static final String KEY_LOCALE = "locale";
    private static final String KEY_THEME = "theme";

    private static final int ACTION_TYPE_DEFAULT = 0;
    private static final int ACTION_TYPE_UP = 1;
    private static final int ACTION_TYPE_RIGHT = 2;
    private static final int ACTION_TYPE_DOWN = 3;
    private static final int ACTION_TYPE_LEFT = 4;
    private static final int SLIDE_RANGE = 100;
    private static final int HISTORY_DAYS = -180;

    private float mTouchStartPointX;
    private float mTouchStartPointY;
    private int mActionType = ACTION_TYPE_DEFAULT;
    private String selectedSender = "";
    private String selectedRecipient = "";
    private TextView current_day;
    private TextView status_label;
    private Calendar cal;

    private Handler serverQueryStateMachineHandler = new Handler();
    private final Handler stateMachineRestartHandler = new Handler();
    private List<String> senderListItems;
    private List<String> recipientListItems;

    private ArrayList<String> pacijenti = new ArrayList<>();
    private Map<String, String> idList= new HashMap<>();
    private ArrayAdapter<String> pacijentiAdapter;

    private static boolean accountSetupInProgressFlag = false;
    private static String appLocale;
    private final int displayInterval = 30;
    private ImageButton weekPrev;
    private ImageButton weekNext;
    private ImageButton dayPrev;
    private ImageButton dayNext;
    private ListView termini;

    private Spinner senderList;
    private Spinner recipientList;
    private FloatingActionButton addAppointmentBtn;

    private int themeNumber = 0;
    private boolean stateMachineRunning = false;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* Get credentials from saved settings */
        getSavedSettings();

        setLocale();

        /* Get current date */
        cal = Calendar.getInstance();
        current_day = findViewById(R.id.text_date);
        current_day.setText(formatTitleDate());

        /* Set drop down list of senders */
        senderList = findViewById(R.id.senderList);
        senderListItems = new ArrayList<>();
        senderListItems.add( getString(R.string.all_senders));
        selectedSender = getString(R.string.all_senders);

        ArrayAdapter<String> senderDataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, senderListItems);
        senderList.setAdapter(senderDataAdapter);
        senderList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                /* User selected a new sender. */
                selectedSender = senderListItems.get(position);
                restartStateMachine();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        /* Set drop down list of recipients */
        recipientList = findViewById(R.id.recipientList);
        recipientListItems = new ArrayList<>();
        recipientListItems.add( getString(R.string.all_recipients));
        selectedRecipient = getString(R.string.all_recipients);

        ArrayAdapter<String> recipientDataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recipientListItems);
        recipientList.setAdapter(recipientDataAdapter);
        recipientList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                /* User selected a new sender. */
                selectedRecipient = recipientListItems.get(position);
                restartStateMachine();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        /* Display list of appointments */
        termini = findViewById(R.id.list_termini);
        pacijentiAdapter = new patientsListAdapter(this, pacijenti);

        termini.setAdapter(pacijentiAdapter);

        termini.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent event) {

                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchStartPointX = event.getRawX();
                        mTouchStartPointY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mTouchStartPointX - x > SLIDE_RANGE) {
                            mActionType = ACTION_TYPE_LEFT;
                        } else if (x - mTouchStartPointX > SLIDE_RANGE) {
                            mActionType = ACTION_TYPE_RIGHT;
                        } else if (mTouchStartPointY - y > SLIDE_RANGE) {
                            mActionType = ACTION_TYPE_UP;
                        } else if (y - mTouchStartPointY > SLIDE_RANGE) {
                            mActionType = ACTION_TYPE_DOWN;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mActionType == ACTION_TYPE_RIGHT) {
                            setPreviousDay();
                        } else if (mActionType == ACTION_TYPE_LEFT) {
                            setNextDay();
                        } else if (mActionType == ACTION_TYPE_UP) {

                        } else if (mActionType == ACTION_TYPE_DOWN) {
                            restartStateMachine();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        termini.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                changeTableEntry(position);
            }
        });

        /* Set button for previous week */
        weekPrev = findViewById(R.id.imgBtn_w_prev);
        weekPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreviousWeek();
            }
        });

        /* Set button for next week */
        weekNext = findViewById(R.id.imgBtn_w_next);
        weekNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNextWeek();
            }
        });

        /* Set button for previous day */
        dayPrev = findViewById(R.id.imgBtn_d_prev);
        dayPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreviousDay();
            }
        });

        /* Set button for next day */
        dayNext = findViewById(R.id.imgBtn_d_next);
        dayNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNextDay();
            }
        });

        /* Add appointment button */
        addAppointmentBtn = findViewById(R.id.addAppointment);
        addAppointmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlobalVars.addAppointmentId = "";
                addAppointment();
            }
        });

        /* Status label */
        status_label = findViewById(R.id.text_status);
        status_label.setText("");

        /* Start server query state machine */
        GlobalVars.m_operation = GlobalVars.OP_START;
        stateMachineProcessor.run();

        changeTheme(false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Auto-generated method stub
    }

    @Override
    protected void onResume(){
        super.onResume();
        restartStateMachine();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopStateMachine();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        stopStateMachine();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.account:
                updateAccount();
                return true;
            case R.id.set_locale:
                changeLocale();
                return true;
            case R.id.change_theme:
                changeTheme(true);
                return true;
            case R.id.about:
                aboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeTheme(boolean toggle){

        if(toggle){
            themeNumber++;

            if(themeNumber > 1){
                themeNumber = 0;
            }

            saveAccountSettings();
        }

        if(themeNumber == 0) {
            GlobalVars.colorBg1 = getString(R.string.colorBlack);
            GlobalVars.colorBg2 = getString(R.string.colorGrayDark);
            GlobalVars.colorBg3 = getString(R.string.colorGrayLight);
            GlobalVars.colorText1 = getString(R.string.colorOrange);
            GlobalVars.colorText2 = getString(R.string.colorBlack);
            GlobalVars.colorText3 = getString(R.string.colorWhite);
            GlobalVars.colorText4 = getString(R.string.colorWhite);

            senderList.setBackground(getResources().getDrawable(R.drawable.spinner_gray));
            recipientList.setBackground(getResources().getDrawable(R.drawable.spinner_gray));
        }else{
            GlobalVars.colorBg1 = getString(R.string.colorPinkDark);
            GlobalVars.colorBg2 = getString(R.string.colorPinkLight );
            GlobalVars.colorBg3 = getString(R.string.colorPinkDark);
            GlobalVars.colorText1 = getString(R.string.colorWhite);
            GlobalVars.colorText2 = getString(R.string.colorBlack);
            GlobalVars.colorText3 = getString(R.string.colorBlack);
            GlobalVars.colorText4 = getString(R.string.colorWhite);

            senderList.setBackground(getResources().getDrawable(R.drawable.spinner_pink));
            recipientList.setBackground(getResources().getDrawable(R.drawable.spinner_pink));
        }


        current_day.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        current_day.setTextColor(Color.parseColor(GlobalVars.colorText1));
        weekPrev.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        weekNext.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        termini.setBackgroundColor(Color.parseColor(GlobalVars.colorBg2));
        findViewById(R.id.mainLayout).setBackgroundColor(Color.parseColor(GlobalVars.colorBg2));
        status_label.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        status_label.setTextColor(Color.parseColor(GlobalVars.colorText1));
        addAppointmentBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(GlobalVars.colorBg1)));

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(GlobalVars.colorBg1)));

        pacijentiAdapter.notifyDataSetChanged();
    }

    private void aboutDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final TextView message = new TextView(this);
        final SpannableString s = new SpannableString(getString(R.string.about_msg));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setText(s);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        layout.addView(message);

        builder.setView(layout);

        builder.setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* Opens a dialog window to update account details */
    private void updateAccount(){
        if(accountSetupInProgressFlag){
            /* Prevent opening another dialog as the previous is still open. */
            return;
        }
        /* Set the open dialog flag so we can prevent opening a new dialog before we close this one. */
        accountSetupInProgressFlag = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.set_account));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText in_email = new EditText(this);
        in_email.setHint(getString(R.string.your_email));
        in_email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(in_email);

        final EditText in_password = new EditText(this);
        in_password.setHint(getString(R.string.password));
        in_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        layout.addView(in_password);

        final EditText in_alias = new EditText(this);
        in_alias.setHint(getString(R.string.your_name));
        in_alias.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        layout.addView(in_alias);

        final TextView reset_password = new TextView(this);
        final SpannableString s = new SpannableString(getString(R.string.reset_pass_msg) + resetPasswordUrl);
        Linkify.addLinks(s, Linkify.WEB_URLS);
        reset_password.setText(s);
        reset_password.setMovementMethod(LinkMovementMethod.getInstance());
        layout.addView(reset_password);

        final TextView status = new TextView(this);
        status.setText("");
        status.setTextColor(Color.RED);
        status.setPadding(10, 20,10,20);
        layout.addView(status);

        builder.setView(layout);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(validateEmail(GlobalVars.account_email) && (GlobalVars.account_password.length() > 5) && (GlobalVars.account_alias.length() > 3)){
                    // All seems OK. Restart the state machine process
                    restartStateMachine();
                }
                /* Allow opening a new dialog */
                accountSetupInProgressFlag = false;
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!validateEmail(in_email.getText().toString())){
                    status.setText(R.string.invalid_email);
                }else if(in_password.getText().toString().length() < 6){
                    status.setText(R.string.invalid_password);
                }else if(in_alias.getText().toString().length() < 3){
                    status.setText(R.string.invalid_name);
                }else{
                    /* All the settings seem fine now, so go ahead with server connections. */
                    GlobalVars.account_email = in_email.getText().toString();
                    GlobalVars.account_password = in_password.getText().toString();
                    GlobalVars.account_alias = in_alias.getText().toString();
                    GlobalVars.account_token = ""; // Force login

                    saveAccountSettings();
                    restartStateMachine();

                    /* Allow opening a new dialog */
                    accountSetupInProgressFlag = false;
                    dialog.dismiss();
                }
            }
        });
    }


    public class TimePickerCustom extends TimePickerDialog{

        public TimePickerCustom(Context arg0, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
            super(arg0, 2, callBack, hourOfDay, minute, is24HourView);
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            // TODO Auto-generated method stub
            //super.onTimeChanged(arg0, arg1, arg2);
            if (mIgnoreEvent)
                return;
            if (minute%displayInterval!=0){
                int minuteFloor=minute-(minute%displayInterval);
                minute=minuteFloor + (minute==minuteFloor+1 ? displayInterval : 0);
                if (minute==60)
                    minute=0;
                mIgnoreEvent=true;
                view.setCurrentMinute(minute);
                mIgnoreEvent=false;
            }
        }

        // NOTE: Change this if you want to change displayed interval
        private boolean mIgnoreEvent=false;

    }

    /* Show a dialog to add an appointment. */
    private void addAppointment(){
        if(GlobalVars.account_role.equals(ROLE_PENDING)){
            notify(getString(R.string.sending_app_not_enabled));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.new_appointment));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText in_time = new EditText(this);
        in_time.setHint(getString(R.string.time));
        /* Set last chosen time to enable editing existing appointments */
        in_time.setText(GlobalVars.addAppointmentTime);
        in_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TimePickerCustom timePickerDialog = new TimePickerCustom(MainActivity.this ,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            in_time.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
                        }
                    }, 0, 0, true);

                timePickerDialog.show();
            }
        });

        layout.addView(in_time);

        final EditText in_message = new EditText(this);
        in_message.setHint(getString(R.string.appointment_text));
        in_message.setText(GlobalVars.addAppointmentText);
        layout.addView(in_message);

        final EditText in_to = new EditText(this);
        in_to.setHint(getString(R.string.recipient_email));
        in_to.setText(GlobalVars.addAppointmentEmail);

        if(GlobalVars.account_role.equals(ROLE_MULTICLIENT) || GlobalVars.account_role.equals(ROLE_SUPERCLIENT)) {
            layout.addView(in_to);
        }

        builder.setView(layout);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                GlobalVars.addAppointmentText = in_message.getText().toString();
                GlobalVars.addAppointmentTime = in_time.getText().toString();

                boolean validFlag = (GlobalVars.addAppointmentTime.length() > 2);
                if(GlobalVars.account_role.equals(ROLE_MULTICLIENT)){
                    GlobalVars.addAppointmentEmail = in_to.getText().toString();
                    validFlag &= validateEmail(GlobalVars.addAppointmentEmail);
                }

                if(validFlag){
                    // Send this appointment
                    restartStateMachine();
                    GlobalVars.m_operation = GlobalVars.OP_ADD;
                }

                dialog.dismiss();
            }
        });
    }

    /* Creates a string representing a user readable date */
    private String formatTitleDate(){
        return String.format( new Locale(appLocale), "%1$tA, %1$td.%1$tb.%1$tY.", cal);
    }

    /* Creates a string to be used as date in the server url query */
    public String formatQueryDate(){
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        return df.format(cal.getTime());
    }

    /* Sets the date to previous week and triggers the new appointment update. */
    protected void setPreviousWeek() {
        cal.add(Calendar.DAY_OF_MONTH, -7);

        if(cal.compareTo(Calendar.getInstance()) < HISTORY_DAYS) {
            cal = Calendar.getInstance();
        }

        current_day.setText(formatTitleDate());
        restartStateMachine();
        clearAppointmentList();
        updateUi();
    }

    /* Sets the date to next week and triggers the new appointment update. */
    protected void setNextWeek() {
        cal.add(Calendar.DAY_OF_MONTH, 7);
        current_day.setText(formatTitleDate());
        restartStateMachine();
        clearAppointmentList();
        updateUi();
    }

    /* Sets the date to previous day and triggers the new appointment update. */
    protected void setPreviousDay() {
        cal.add(Calendar.DAY_OF_MONTH, -1);

        if(cal.compareTo(Calendar.getInstance()) < HISTORY_DAYS) {
            cal = Calendar.getInstance();
        }

        current_day.setText(formatTitleDate());
        restartStateMachine();
        clearAppointmentList();
        updateUi();
    }

    /* Sets the date to next day and triggers the new appointment update. */
    protected void setNextDay() {
        cal.add(Calendar.DAY_OF_MONTH, 1);
        current_day.setText(formatTitleDate());
        restartStateMachine();
        clearAppointmentList();
        updateUi();
    }

    /* Selects the clicked appointment and opens the appointment update dialog. */
    private void changeTableEntry(int id){
        try {
            String[] entry = pacijenti.get(id).split(": ");
            GlobalVars.addAppointmentTime = entry[0];
            GlobalVars.addAppointmentText = entry[1].trim();
            GlobalVars.addAppointmentId = idList.get(pacijenti.get(id));
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
            GlobalVars.addAppointmentTime =  pacijenti.get(id);
            GlobalVars.addAppointmentText = "";
            GlobalVars.addAppointmentId = "";
        }

        addAppointment();
    }

    /* Validates a provided email string*/
    private boolean validateEmail(String email)
    {
        String regExpn = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                        +"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        +"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                        +"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

        Pattern pattern = Pattern.compile(regExpn,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches();
    }

    /* State machine runnable. */
    Runnable stateMachineProcessor = new Runnable() {
        @Override
        public void run() {
//            Log.d("StateMachine", "OP:" + GlobalVars.m_operation);

            boolean keepRunningFlag = true;
            stateMachineRunning = true;

            switch(GlobalVars.m_operation){
                case GlobalVars.OP_STOP:
                    keepRunningFlag = false;
                    GlobalVars.m_operation = GlobalVars.OP_IDLE;
                    break;

                case GlobalVars.OP_START:
                    status_label.setText(getString(R.string.sync_in_progress));
                    GlobalVars.m_lastOperation = GlobalVars.m_operation;

                    if(!validateEmail(GlobalVars.account_email)){
                        // No valid email. Ask user to setup the account.
                        updateAccount();
                        GlobalVars.m_operation = GlobalVars.OP_IDLE;
                    }else if(GlobalVars.account_token.isEmpty()){
                        GlobalVars.m_operation = GlobalVars.OP_LOGIN;
                    }else{
                        GlobalVars.m_operation = GlobalVars.OP_UPDATE;
                    }
                    break;

                case GlobalVars.OP_SIGNUP:

                    String name = "";
                    try{
                        name = URLEncoder.encode(GlobalVars.account_alias, "utf-8");
                    }catch (UnsupportedEncodingException e){
                       Log.e(TAG, e.getMessage());
                    }

                    GlobalVars.pending_request_url = "?operation=signup&email=" + GlobalVars.account_email + "&pass=" + GlobalVars.account_password + "&name=" + name;
                    GlobalVars.m_lastOperation = GlobalVars.m_operation;
                    GlobalVars.m_operation = GlobalVars.OP_IDLE;
                    break;

                case GlobalVars.OP_LOGIN:
                    GlobalVars.pending_request_url = "?operation=login&email=" + GlobalVars.account_email + "&pass=" + GlobalVars.account_password;
                    GlobalVars.m_lastOperation =GlobalVars. m_operation;
                    GlobalVars.m_operation = GlobalVars.OP_IDLE;
                    break;

                case GlobalVars.OP_UPDATE:
                    GlobalVars.pending_request_url = "?operation=get_app&token=" + GlobalVars.account_token + "&date=" + formatQueryDate();
                    GlobalVars.m_lastOperation =GlobalVars. m_operation;
                    GlobalVars.m_operation = GlobalVars.OP_IDLE;
                    break;

                case GlobalVars.OP_ADD:
                    String body = "";
                    try{
                        body = URLEncoder.encode(GlobalVars.addAppointmentText, "utf-8");
                    }catch (UnsupportedEncodingException e){
                        Log.e(TAG, e.getMessage());
                    }

                    GlobalVars.pending_request_url = "?operation=set_app&token=" + GlobalVars.account_token + "&time=" + GlobalVars.addAppointmentTime + "&date=" +
                            formatQueryDate() + "&body=" + body + "&email=" + GlobalVars.addAppointmentEmail;

                    if(GlobalVars.addAppointmentId.length() > 0){
                        GlobalVars.pending_request_url += "&app_id=" + GlobalVars.addAppointmentId;
                    }

                    GlobalVars.m_lastOperation = GlobalVars.m_operation;
                    GlobalVars.m_operation = GlobalVars.OP_IDLE;
                    break;

                default:
                    break;
            }

            if(keepRunningFlag) {
                if((GlobalVars.pending_request_url.length() > 1) && !GlobalVars.connection_in_progress){
                    /* query requested url */
                    new ServerComms(MainActivity.this).execute(GlobalVars.pending_request_url);
                    GlobalVars.pending_request_url = "";
                }

                /* Restart this state machine after a timeout. */
                if(GlobalVars.m_operation == GlobalVars.OP_UPDATE) {
                    /* Wait longer for the user to stop switching dates, to optimize the network trafic. */
                    serverQueryStateMachineHandler.postDelayed(this, 800);
                }else if(GlobalVars.m_lastOperation == GlobalVars.OP_START){
                    serverQueryStateMachineHandler.postDelayed(this, 50);
                }else{
                    serverQueryStateMachineHandler.postDelayed(this, 180);
                }
            }else{
                stateMachineRunning = false;
            }
        }
    };

    /* Display a notification dialog with a requested message */
    public void notify(String message){
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(R.string.app_name);
        dlgAlert.setCancelable(false);
        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                });
        dlgAlert.create().show();
    }

    public void stopStateMachine(){
        GlobalVars.m_operation = GlobalVars.OP_STOP;
        serverQueryStateMachineHandler.removeCallbacksAndMessages(null);
    }

    public void restartStateMachine(){
        stateMachineRestartHandler.removeCallbacksAndMessages(null);

        stateMachineRestartHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayedStateMachineRestart();
            }
        }, 600);
    }

    public void delayedStateMachineRestart(){
        GlobalVars.account_token = "";
        if (GlobalVars.m_operation == GlobalVars.OP_STOP){
            stateMachineRunning = false;
            serverQueryStateMachineHandler.removeCallbacksAndMessages(null);
            GlobalVars.m_operation = GlobalVars.OP_START;
            stateMachineProcessor.run();
        }else{
            GlobalVars.m_operation = GlobalVars.OP_START;
            if(!stateMachineRunning){
                stateMachineProcessor.run();
            }
        }

        pacijenti.clear();
        idList.clear();
        pacijentiAdapter.notifyDataSetChanged();
    }

    public void clearAppointmentList(){
        pacijenti.clear();
        idList.clear();
    }

    public void addToAppointments(String data, String appId){

        if(!pacijenti.contains(data)) {
            pacijenti.add(data);
            idList.put(data, appId);
        }
    }

    public void updateUi(){
        /* The following is to populate vacant appointments for better preview. */
        Map<String, String> displayList = new HashMap<>();

        /* Put existing appointments in the map. */
        for (String ap: pacijenti) {
            String curTime = ap.substring(0, 5);

            if(displayList.containsKey(curTime)){
                String currentData = displayList.get(curTime);
                currentData += GlobalVars.DIVIDER + ap;
                displayList.put(curTime, currentData);
            }else {
                displayList.put(curTime, ap);
            }
        }
        pacijenti.clear();

        boolean firstAppointmentFound = false;

        int lastT = 0;
        int resPerHour = 60 / displayInterval;
        for (int i = 0; i < (24 * resPerHour); i++) {

            int h = i / resPerHour;
            int m = (i % resPerHour) * displayInterval;

            String curTime = String.format("%02d:%02d", h, m);

            if (displayList.containsKey(curTime)) {
                if (firstAppointmentFound) {
                    /* populate free appointments with blank data for better preview */
                    for (int j = lastT; j < i; j++) {
                        h = j / resPerHour;
                        m = (j % resPerHour) * displayInterval;
                        String newTime = String.format("%02d:%02d", h, m);
                        pacijenti.add(newTime);
                    }
                }

                firstAppointmentFound = true;
                lastT = i + 1;

                String recordedAp = displayList.get(curTime);

                if (recordedAp.contains(GlobalVars.DIVIDER)) {
                    pacijenti.addAll(Arrays.asList(recordedAp.split(GlobalVars.DIVIDER)));
                }else{
                    pacijenti.add(displayList.get(curTime));
                }
            }
        }

        /* needed to refresh display */
        pacijentiAdapter.notifyDataSetChanged();
    }

    public String getSelectedSender(){
        return selectedSender;
    }

    public String getSelectedRecipient(){
        return selectedRecipient;
    }

    public void addSender(String label){
        if(!senderListItems.contains(label)) {
            senderListItems.add(label);
        }
    }

    public void addRecipient(String label){
        if(!recipientListItems.contains(label)) {
            recipientListItems.add(label);
        }
    }

    public void setStatus( String text){
        if((status_label.getText().length() < 3) || (status_label.getText().equals(getString(R.string.sync_in_progress)))){
            status_label.setText(text);
        }else if(text.contains("OK:")){
            restartStateMachine();
        }
    }

    private void setLocale(){
        Locale locale = new Locale(appLocale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        setTitle(getString(R.string.app_name));

    }

    private void changeLocale(){
        if(appLocale.equals(LOCALE_EN)){
            appLocale = LOCALE_SR;
        }else{
            appLocale = LOCALE_EN;
        }

        setLocale();
        saveAccountSettings();
        /* Restart the application to apply the new locale. */
        //finish();
        restart();
    }

    /* Restarts the application. */
    public void restart(){
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        finish();
        startActivity(i);

    }

    /* Reads saved settings from shared preferences. */
    private void getSavedSettings(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        GlobalVars.account_email = sharedPref.getString(KEY_EMAIL, "");
        GlobalVars.account_password = sharedPref.getString(KEY_PASSWORD, "");
        GlobalVars.account_alias = sharedPref.getString(KEY_ALIAS, "");
        appLocale = sharedPref.getString(KEY_LOCALE, LOCALE_EN);
        themeNumber = sharedPref.getInt(KEY_THEME, 0);

        /* Set the email of the recipient to our own until changed. */
        GlobalVars.addAppointmentEmail = GlobalVars.account_email;
    }

    /* Saves the settings to the shared preferences. */
    private void saveAccountSettings(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_EMAIL, GlobalVars.account_email);
        editor.putString(KEY_PASSWORD, GlobalVars.account_password);
        editor.putString(KEY_ALIAS, GlobalVars.account_alias);
        editor.putString(KEY_LOCALE, appLocale);
        editor.putInt(KEY_THEME, themeNumber);

        editor.apply();
    }

}
