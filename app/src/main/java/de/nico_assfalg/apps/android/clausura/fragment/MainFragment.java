/*
    Copyright 2020 Nico Aßfalg

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package de.nico_assfalg.apps.android.clausura.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

import de.nico_assfalg.apps.android.clausura.R;
import de.nico_assfalg.apps.android.clausura.fragment.dialogs.ExamDetailDialog;
import de.nico_assfalg.apps.android.clausura.helper.ExamDBHelper;
import de.nico_assfalg.apps.android.clausura.helper.PreferenceHelper;
import de.nico_assfalg.apps.android.clausura.time.Calculator;
import de.nico_assfalg.apps.android.clausura.time.Date;

public class MainFragment extends Fragment {

    public static final String KEY_EXTRA_EXAM_ID = "KEY_EXTRA_EXAM_ID";
    final String SEPARATOR = "  ·  ";

    View layout;
    LinearLayout examList;
    Date tempDate;
    ExamDBHelper dbHelper;
    int allExamCounter;
    int pastExamCounter;
    LayoutInflater inflater;

    public MainFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        layout = inflater.inflate(R.layout.fragment_main, container, false);
        examList = (LinearLayout) layout.findViewById(R.id.examList);
        examList.removeAllViews();
        populate();

        return layout;
    }

    private void populate() {
        tempDate = new Date(0,0,0);
        dbHelper = new ExamDBHelper(getActivity());

        final Cursor cursor = dbHelper.getAllExams();

        if (cursor.moveToFirst()) {
            allExamCounter = 0;
            pastExamCounter = 0;
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
                    } else {
                        pastExamCounter++;
                    }
                } else {
                    //Add Month/Year label if necessary
                    addMonthYearLabel(date);
                    //Make Exam Element and add it to the List
                    examList.addView(examElement(title, date, time, id));
                }
                allExamCounter++;
                cursor.moveToNext();
            }
        }
        cursor.close(); //Important!

        addNoExamText();
    }

    private boolean pastAllowed() {
        String pastSetting = PreferenceHelper.getPreference(getActivity(), "showPast");
        if (pastSetting.equals("0")) {
            return false;
        } else {
            PreferenceHelper.setPreference(getActivity(), "1", "showPast");
            return true;
        }
    }

    private void addMonthYearLabel(String date) {
        Date currentDate = new Date(date);
        final LinearLayout monthYearLayout = (LinearLayout) inflater.inflate(R.layout.layout_month_year_label, null, false);
        if (tempDate.getMonth() < currentDate.getMonth() && tempDate.getYear() == currentDate.getYear()) {
            TextView monthYearText = (TextView) monthYearLayout.findViewById(R.id.monthYearText);
            String text = currentDate.getMonthAsString();
            monthYearText.setText(text);
            monthYearText.setTag(text);
            examList.addView(monthYearLayout);
        } else if (tempDate.getYear() < currentDate.getYear()) {
            TextView monthYearText = (TextView) monthYearLayout.findViewById(R.id.monthYearText);
            String text = currentDate.getMonthAsString() + " " + currentDate.getYear();
            monthYearText.setText(text);
            monthYearText.setTag(text);
            examList.addView(monthYearLayout);
        }
        tempDate = currentDate;
    }

    private LinearLayout examElement(String title, String dateString, String timeString, int id) {
        //inflate the layout file and define its TextViews
        final LinearLayout examLayout = (LinearLayout) inflater.inflate(R.layout.layout_exam, null, false);
        TextView examDay = (TextView) examLayout.findViewById(R.id.examDay);
        TextView examDayOfWeek = (TextView) examLayout.findViewById(R.id.examDayOfWeek);
        TextView examTitle = (TextView) examLayout.findViewById(R.id.examTitle);
        TextView examDaysUntil = (TextView) examLayout.findViewById(R.id.examDaysUntil);

        //Define what happens when exam is clicked
        examLayout.setId(id);
        examLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExamDetailsDialog(v.getId());
            }
        });

        //create a Date object with current exam date
        Date date = new Date(dateString);

        //set text of TextViews
        examDay.setText(String.valueOf(date.getDay()));
        examDayOfWeek.setText(date.getDayAsShortString());

        examTitle.setText(title);
        String examDaysUntilString = Calculator.daysUntilAsString(date, getActivity());
        examDaysUntilString = !timeString.equals("") ?
                examDaysUntilString + SEPARATOR + Date.parseTimeStringToHumanString(timeString) :
                examDaysUntilString;

        examDaysUntil.setText(examDaysUntilString);

        return examLayout;
    }

    public void showExamDetailsDialog(int examId) {
        ExamDetailDialog dialog = new ExamDetailDialog();

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_EXTRA_EXAM_ID, examId);
        dialog.setArguments(bundle);

        dialog.setTargetFragment(MainFragment.this, 1);

        dialog.show(getFragmentManager(), "exam_details");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        deleteExam(examList.findViewById(resultCode));
    }

    private void deleteExam(View v) {
        Cursor examToDelete = dbHelper.getExam(v.getId());
        examToDelete.moveToFirst();
        Date examDate = new Date(examToDelete.getString(examToDelete
                .getColumnIndex(ExamDBHelper.EXAM_COLUMN_DATE)));
        int examMonth = examDate.getMonth();
        int examYear = examDate.getYear();
        Cursor cursor = dbHelper.getAllExams();

        int examIndex = examList.indexOfChild(v);

        dbHelper.deleteExam(v.getId());
        examList.removeView(v);

        allExamCounter--;
        if (new Date(Calendar.getInstance()).toMs() > examDate.toMs()) {
            pastExamCounter--;
        }

        boolean sameMonthInYear = false;
        boolean allInPast = !pastAllowed();

        if (cursor.moveToFirst()) {
            while (cursor.moveToNext() && !sameMonthInYear) {
                Date date = new Date(cursor.getString(cursor.
                        getColumnIndex(ExamDBHelper.EXAM_COLUMN_DATE)));
                int month = date.getMonth();
                int year = date.getYear();

                sameMonthInYear = (month == examMonth) && (year == examYear);

                Date current = new Date(Calendar.getInstance());

                if (allInPast && month == current.getMonth()) {
                    allInPast = (month == examMonth) && (date.toMs() < current.toMs());
                }
            }
        }

        if (!sameMonthInYear || allInPast) {
            examList.removeViewAt(examIndex - 1);
        }

        addNoExamText();
    }

    private void addNoExamText() {
        if (allExamCounter < 1) {
            LinearLayout ll = (LinearLayout) inflater
                    .inflate(R.layout.layout_no_exam_text, null);

            TextView noExams = (TextView) ll.findViewById(R.id.noExamText);
            noExams.setText(getString(R.string.text_no_exams));

            Button showPastButton = (Button) ll.findViewById(R.id.showPastButton);
            showPastButton.setVisibility(View.GONE);

            examList.addView(ll);
        } else {
            if (pastExamCounter >= allExamCounter && allExamCounter > 0) {
                LinearLayout ll = (LinearLayout) inflater
                        .inflate(R.layout.layout_no_exam_text, null);

                TextView noExams = (TextView) ll.findViewById(R.id.noExamText);
                noExams.setText(getString(R.string.text_no_exams_past));

                Button showPastButton = (Button) ll.findViewById(R.id.showPastButton);
                showPastButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPast(true);
                    }
                });
                if (pastExamCounter == 1) {
                    showPastButton.setText(getString(R.string.button_show_past_singular));
                } else {
                    showPastButton.setText(getString(R.string.button_show_past_plural, pastExamCounter));
                }

                examList.addView(ll);
            }
        }
    }

    private void showPast(boolean enabled) {
        if (!enabled) {
            PreferenceHelper.setPreference(getActivity(), "0", "showPast");
            getActivity().recreate();
            //Snackbar snackbar = Snackbar.make(layout.findViewById(coordinatorLayout), "Vergangene Termine werden ausgeblendet.", Snackbar.LENGTH_LONG);
            //snackbar.show();
        } else {
            PreferenceHelper.setPreference(getActivity(), "1", "showPast");
            examList.removeAllViews();
            getActivity().recreate();
            //Snackbar snackbar = Snackbar.make(layout.findViewById(coordinatorLayout), "Vergangene Termine werden angezeigt.", Snackbar.LENGTH_LONG);
            //snackbar.show();
        }
    }
}
