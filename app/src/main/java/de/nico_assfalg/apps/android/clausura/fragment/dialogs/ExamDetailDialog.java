package de.nico_assfalg.apps.android.clausura.fragment.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.BundleCompat;
import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import de.nico_assfalg.apps.android.clausura.R;
import de.nico_assfalg.apps.android.clausura.activity.ExamEditActivity;
import de.nico_assfalg.apps.android.clausura.fragment.MainFragment;
import de.nico_assfalg.apps.android.clausura.helper.ExamDBHelper;
import de.nico_assfalg.apps.android.clausura.time.Calculator;
import de.nico_assfalg.apps.android.clausura.time.Date;

public class ExamDetailDialog extends DialogFragment {

    View layout;

    int examId;

    public ExamDetailDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.layout_dialog_exam_details, container);
        Bundle bundle = getArguments();
        examId = bundle.getInt(MainFragment.KEY_EXTRA_EXAM_ID);
        showDetails();
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); //
    }

    private void showDetails() {
        final Cursor cursor = new ExamDBHelper(getActivity()).getExam(examId);
        if (cursor.moveToFirst()) {
            TextView titleLineText = (TextView) layout.findViewById(R.id.titleLineText);
            titleLineText.setText(cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_TITLE)));

            TextView dateLineTextDate = (TextView) layout.findViewById(R.id.dateLineTextDate);
            TextView dateLineTextUntil = (TextView) layout.findViewById(R.id.dateLineTextUntil);
            Date date = new Date(cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_DATE)));
            dateLineTextDate.setText(date.toHumanString());
            dateLineTextUntil.setText(Calculator.daysUntilAsString(date, getActivity()));

            TextView timeLineText = (TextView) layout.findViewById(R.id.timeLineText);
            String time = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_TIME));
            if (time.equals("")) {
                layout.findViewById(R.id.timeLine).setVisibility(View.GONE);
            } else {
                timeLineText.setText(Date.parseTimeStringToHumanString(time));
            }

            TextView locationLineText = (TextView) layout.findViewById(R.id.locationLineText);
            String location = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_LOCATION));
            if (location.equals("")) {
                layout.findViewById(R.id.locationLine).setVisibility(View.GONE);
            } else {
                locationLineText.setText(location);
            }

            TextView notesLineText = (TextView) layout.findViewById(R.id.notesLineText);
            String notes = cursor.getString(cursor.getColumnIndex(ExamDBHelper.EXAM_COLUMN_NOTES));
            if (notes.equals("")) {
                layout.findViewById(R.id.notesLine).setVisibility(View.GONE);
            } else {
                notesLineText.setText(notes);
            }

            Button examEditButton = (Button) layout.findViewById(R.id.examEditButton);
            examEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                    Intent intent = new Intent(getActivity(), ExamEditActivity.class);
                    intent.putExtra(MainFragment.KEY_EXTRA_EXAM_ID, examId); //Start Editing Activity with id of exam to edit
                    startActivity(intent);
                }
            });

            Button examDeleteButton = (Button) layout.findViewById(R.id.examDeleteButton);
            examDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ExamDBHelper(getActivity()).deleteExam(examId);
                    dismiss();
                    getTargetFragment().getActivity().recreate();
                }
            });

            cursor.close(); //Important
        } else {
            cursor.close();
        }
    }
}