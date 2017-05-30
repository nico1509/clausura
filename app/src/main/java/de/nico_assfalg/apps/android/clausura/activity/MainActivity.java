package de.nico_assfalg.apps.android.clausura.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import de.nico_assfalg.apps.android.clausura.time.Calculator;
import de.nico_assfalg.apps.android.clausura.time.Date;
import de.nico_assfalg.apps.android.clausura.helper.ExamDBHelper;
import de.nico_assfalg.apps.android.clausura.ExamHandler;
import de.nico_assfalg.apps.android.clausura.helper.PreferenceHelper;
import de.nico_assfalg.apps.android.clausura.R;

import static de.nico_assfalg.apps.android.clausura.R.id.coordinatorLayout;

public class MainActivity extends AppCompatActivity
        implements DatePickerDialog.OnDateSetListener {

    public static final String KEY_EXTRA_EXAM_ID = "KEY_EXTRA_EXAM_ID";

    private LinearLayout examList;
    ExamDBHelper dbHelper;

    static String lectureEndDate;

    final String SEPARATOR = "  ·  ";

    Date tempDate; //needed for monthYear labels

    //OLD SHIT
    private static final int PERMISSION_REQUEST_ID_EXTERNAL_STORAGE = 42;
    private static final int BACKUP_SAVE = 0;
    private static final int BACKUP_RESTORE = 1;
    private static int backupMode;
    //

    /*

                ACTIVITY

     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialize FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ExamEditActivity.class);
                intent.putExtra(KEY_EXTRA_EXAM_ID, 0);
                startActivity(intent);
            }
        });

        /*TODO: remove!!!
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doBackupMode(MainActivity.BACKUP_RESTORE);
                PreferenceHelper.setPreference(getApplicationContext(), "", PreferenceHelper.IMPORT_SUCCESSFUL);
                PreferenceHelper.importOldSettings(getApplicationContext());
                return true;
            }
        });*/

        PreferenceHelper.importOldSettings(getApplicationContext());

        examList = (LinearLayout) findViewById(R.id.examList);
        examList.removeAllViews();
        populate();
    }

    /*

                LAYOUT

     */

    private void populate() {
        initLectureEnd();

        tempDate = new Date(0,0,0);

        dbHelper = new ExamDBHelper(this);

        final Cursor cursor = dbHelper.getAllExams();

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                //Get exam Title, Date and _id
                String title = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_TITLE));
                String date = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_DATE));
                String time = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_TIME));
                int id = cursor.getInt(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_ID));

                if (!pastAllowed()) {
                    Date exDate = new Date(date);
                    Date now = new Date(Calendar.getInstance());
                    if (now.toMs() <= exDate.toMs()) {
                        //Add Month/Year label if necessary
                        addMonthYearLabel(date);
                        //Make Exam Element and add it to the List
                        examList.addView(examElement(title, date, time, id));
                    }
                } else {
                    //Add Month/Year label if necessary
                    addMonthYearLabel(date);
                    //Make Exam Element and add it to the List
                    examList.addView(examElement(title, date, time, id));
                }
                cursor.moveToNext();
            }
        }
        cursor.close(); //Important!
    }

    private void initLectureEnd() {
        LinearLayout lectureEndLayout = (LinearLayout) findViewById(R.id.lectureEndLayout);
        final LinearLayout lectureEnd = (LinearLayout) lectureEndLayout.findViewById(R.id.examLayout);
        ImageButton showHideButton = (ImageButton) lectureEndLayout.findViewById(R.id.showHideLectureEndButton);
        if (PreferenceHelper.getPreference(this, "showLectureEnd").equals("0")) {
            lectureEnd.setVisibility(View.GONE);
            showHideButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_keyboard_arrow_down_black_24dp));
        }
        lectureEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment df = new DatePickerFragment();
                df.show(getFragmentManager(), "datePicker");
            }
        });
        TextView titleText = (TextView) lectureEnd.findViewById(R.id.examTitle);
        titleText.setText(getString(R.string.text_end_of_lecture));
        TextView daysUntil = (TextView) lectureEnd.findViewById(R.id.examDaysUntil);
        lectureEndDate = PreferenceHelper.getPreference(this, "endOfLectureDate");
        if (lectureEndDate.equals("")) {
            Date current = new Date(Calendar.getInstance());
            lectureEndDate = current.toString();
        }
        Date date = new Date(lectureEndDate);
        String dateAndUntil = date.toHumanString() + SEPARATOR + Calculator.daysUntilAsString(date, this);
        daysUntil.setText(dateAndUntil);
        TextView invisibleDate = (TextView) lectureEnd.findViewById(R.id.examDay);
        invisibleDate.setVisibility(View.INVISIBLE);
        TextView invisibleDay = (TextView) lectureEnd.findViewById(R.id.examDayOfWeek);
        invisibleDay.setVisibility(View.INVISIBLE);
        showHideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageButton imgBtn = (ImageButton) view;
                if (lectureEnd.getVisibility() != View.GONE) {
                    PreferenceHelper.setPreference(getApplicationContext(), "0", "showLectureEnd");
                    lectureEnd.setVisibility(View.GONE);
                    imgBtn.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_keyboard_arrow_down_black_24dp));
                } else {
                    PreferenceHelper.setPreference(getApplicationContext(), "1", "showLectureEnd");
                    lectureEnd.setVisibility(View.VISIBLE);
                    imgBtn.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_keyboard_arrow_up_black_24dp));
                }

            }
        });
    }

    private LinearLayout examElement(String title, String dateString, String timeString, int id) {
        //inflate the layout file and define its TextViews
        final LinearLayout examLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.layout_exam, null, false);
        TextView examDay = (TextView) examLayout.findViewById(R.id.examDay);
        TextView examDayOfWeek = (TextView) examLayout.findViewById(R.id.examDayOfWeek);
        TextView examTitle = (TextView) examLayout.findViewById(R.id.examTitle);
        TextView examDaysUntil = (TextView) examLayout.findViewById(R.id.examDaysUntil);

        //Define what happens when exam is clicked
        examLayout.setId(id);
        examLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExamDetailsDialog(v);
            }
        });

        //create a Date object with current exam date
        Date date = new Date(dateString);

        //set text of TextViews
        examDay.setText(String.valueOf(date.getDay()));
        examDayOfWeek.setText(date.getDayAsShortString());

        examTitle.setText(title);
        String examDaysUntilString = Calculator.daysUntilAsString(date, this);
        examDaysUntilString = !timeString.equals("") ?
                examDaysUntilString + SEPARATOR + Date.parseTimeStringToHumanString(timeString) :
                examDaysUntilString;

        examDaysUntil.setText(examDaysUntilString);

        return examLayout;
    }

    private void addMonthYearLabel(String date) {
        Date currentDate = new Date(date);
        final LinearLayout monthYearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.layout_month_year_label, null, false);
        if (tempDate.getMonth() < currentDate.getMonth() && tempDate.getYear() == currentDate.getYear()) {
            TextView monthYearText = (TextView) monthYearLayout.findViewById(R.id.monthYearText);
            monthYearText.setText(currentDate.getMonthAsString());
            examList.addView(monthYearLayout);
        } else if (tempDate.getYear() < currentDate.getYear()) {
            TextView monthYearText = (TextView) monthYearLayout.findViewById(R.id.monthYearText);
            String monthYear = currentDate.getMonthAsString() + " " + currentDate.getYear();
            monthYearText.setText(monthYear);
            examList.addView(monthYearLayout);
        }
        tempDate = currentDate;


    }

    public void showExamDetailsDialog(final View v) {
        final Cursor cursor = dbHelper.getExam(v.getId());
        if (cursor.moveToFirst()) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.layout_exam_details);
            dialog.setTitle("Details");

            TextView titleLineText = (TextView) dialog.findViewById(R.id.titleLineText);
            titleLineText.setText(cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_TITLE)));

            TextView dateLineTextDate = (TextView) dialog.findViewById(R.id.dateLineTextDate);
            TextView dateLineTextUntil = (TextView) dialog.findViewById(R.id.dateLineTextUntil);
            Date date = new Date(cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_DATE)));
            dateLineTextDate.setText(date.toHumanString());
            dateLineTextUntil.setText(Calculator.daysUntilAsString(date, this));

            TextView timeLineText = (TextView) dialog.findViewById(R.id.timeLineText);
            String time = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_TIME));
            if (time.equals("")) {
                dialog.findViewById(R.id.timeLine).setVisibility(View.GONE);
            } else {
                timeLineText.setText(Date.parseTimeStringToHumanString(time));
            }

            TextView locationLineText = (TextView) dialog.findViewById(R.id.locationLineText);
            String location = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_LOCATION));
            if (location.equals("")) {
                dialog.findViewById(R.id.locationLine).setVisibility(View.GONE);
            } else {
                locationLineText.setText(location);
            }

            TextView notesLineText = (TextView) dialog.findViewById(R.id.notesLineText);
            String notes = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_NOTES));
            if (notes.equals("")) {
                dialog.findViewById(R.id.notesLine).setVisibility(View.GONE);
            } else {
                notesLineText.setText(notes);
            }

            Button examEditButton = (Button) dialog.findViewById(R.id.examEditButton);
            examEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    Intent intent = new Intent(MainActivity.this, ExamEditActivity.class);
                    intent.putExtra(KEY_EXTRA_EXAM_ID, v.getId()); //Start Editing Activity with id of exam to edit
                    startActivity(intent);
                }
            });

            Button examDeleteButton = (Button) dialog.findViewById(R.id.examDeleteButton);
            examDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ExamDBHelper db = new ExamDBHelper(v.getContext());
                    db.deleteExam(v.getId());
                    examList.removeAllViews();
                    populate();
                    dialog.dismiss();
                }
            });
            cursor.close();
            dialog.show();
        } else {
            cursor.close();
        }
    }

    private boolean pastAllowed() {
        String pastSetting = PreferenceHelper.getPreference(getApplicationContext(), "showPast");
        if (pastSetting.equals("0")) {
            return false;
        } else {
            PreferenceHelper.setPreference(getApplicationContext(), "1", "showPast");
            return true;
        }
    }
    /*

                OLD SHIT

     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Affects the content of the options menu everytime it's displayed
        MenuItem item = menu.findItem(R.id.action_past);
        if (pastAllowed()) {
            item.setChecked(true);
        } else {
            item.setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement  //TODO: Ein kleines Fensterchen machen
        if (id == R.id.action_info) {
            Snackbar snackbar = Snackbar.make(findViewById(coordinatorLayout), "© 2016 Nico Assfalg", Snackbar.LENGTH_SHORT);
            snackbar.setAction("UPDATE", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.nico-assfalg.de/clausura_2-4.html"));
                    startActivity(browserIntent);
                }
            });
            snackbar.show();
            return true;
        }

        if (id == R.id.action_past) { //showPast € {0,1,epsilon}
            if (item.isChecked()) {
                PreferenceHelper.setPreference(getApplicationContext(), "0", "showPast");
                examList.removeAllViews();
                populate();
                item.setChecked(false);
                Snackbar snackbar = Snackbar.make(findViewById(coordinatorLayout), "Vergangene Termine werden ausgeblendet.", Snackbar.LENGTH_LONG);
                snackbar.show();
            } else {
                PreferenceHelper.setPreference(getApplicationContext(), "1", "showPast");
                examList.removeAllViews();
                populate();
                item.setChecked(true);
                Snackbar snackbar = Snackbar.make(findViewById(coordinatorLayout), "Vergangene Termine werden angezeigt.", Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }

        if (id == R.id.action_backup) {
            Intent intent = new Intent(MainActivity.this, BackupRestoreActivity.class);
            startActivity(intent);
        }

        if (id == R.id.action_restore) {
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ID_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doBackupMode(backupMode);
                } else {
                    Snackbar snackbar = Snackbar.make(findViewById(coordinatorLayout), "Zugriff verweigert,\r\nAktion nicht möglich!", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
        }
    }

    public void doBackupMode(int mode) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            backupMode = mode;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_ID_EXTERNAL_STORAGE);
        } else {
            switch (mode) {
                case BACKUP_SAVE:
                    saveBackup();
                    break;
                case BACKUP_RESTORE:
                    restoreBackup();
                    break;
            }
        }
    }

    public void saveBackup() {
        String success = "null";
        //Check if writable and do backup
        try {
            String storageState = Environment.getExternalStorageState();
            if (storageState.equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ClausuraBackup"); /*_" + new Date(Calendar.getInstance()).toString() + "_" + Date.currentTimeinMs()*/
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(ExamHandler.getAsString().getBytes());
                fos.close();
            }
            success = "Backup erfolgreich!";
        } catch (Exception e) {
            success = "Backup NICHT erfolgreich!";
            e.printStackTrace();
        } finally {
            Snackbar snackbar = Snackbar.make(findViewById(coordinatorLayout), success, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }

    public void restoreBackup() {
        String storageState = Environment.getExternalStorageState();
        String success = "nix";
        try {
            //Check if writable and restore the backup
            if (storageState.equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ClausuraBackup");
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String inputString;
                StringBuilder stringBuilder = new StringBuilder();
                while ((inputString = inputReader.readLine()) != null) {
                    stringBuilder.append(inputString);
                }
                PreferenceHelper.setPreference(getApplicationContext(), stringBuilder.toString(), "examList");

                success = "Erfolgreich wiederhergestellt";
            }
        } catch (Exception e) {
            success = "Fehler bei der Wiederherstellung";
            e.printStackTrace();
        } finally {
            Snackbar snackbar = Snackbar.make(findViewById(coordinatorLayout), success, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }

    //DatePicker Shit

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String[] dateParts = lectureEndDate.split("-");
            int[] yearMonthDay = {Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1])-1, Integer.parseInt(dateParts[2])};
            return new DatePickerDialog(getActivity(), (DatePickerDialog.OnDateSetListener)getActivity(),
                    yearMonthDay[0], yearMonthDay[1], yearMonthDay[2]);
        }
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
        month++;
        PreferenceHelper.setPreference(this, year + "-" + month + "-" + dayOfMonth, "endOfLectureDate");
        initLectureEnd();
    }
}