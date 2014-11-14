package com.haru.ui.helpcenter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.haru.helpcenter.HelpCenter;
import com.haru.ui.R;

public class SendQuestionDialogBuilder extends AlertDialog.Builder {


    private void init(Context context) {

        setTitle("Send Question");

        View contentView = LayoutInflater.from(context)
                .inflate(R.layout.haru_send_question, null, false);
        this.setView(contentView);

        final EditText questionEdit = (EditText) contentView.findViewById(R.id.haru_question_edit);
        this.setPositiveButton("보내기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // send question to server
                HelpCenter.sendQuestion("", questionEdit.getText().toString());
            }
        });
        this.setCancelable(true);
        this.setNegativeButton(android.R.string.cancel, null);
    }

    public SendQuestionDialogBuilder(Context context) {
        super(context);
        init(context);
    }

    @SuppressLint("NewApi")
    public SendQuestionDialogBuilder(Context context, int theme) {
        super(context, theme);
        init(context);
    }
}
