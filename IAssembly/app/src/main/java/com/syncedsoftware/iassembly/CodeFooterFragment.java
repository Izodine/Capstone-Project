package com.syncedsoftware.iassembly;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;

public class CodeFooterFragment extends Fragment implements Interpreter.InterpreterListener {

    public static final String BUNDLE_TUTORIAL_MODE = "TUTORIAL_MODE";
    public static final String BUNDLE_TUTORIAL_ID   = "TUTORIAL_ID";

    @Bind(R.id.code_output_textView) TextView mOutputTextView;
    @Bind(R.id.output_text_scrollView) ScrollView mOutputScroll;

    private Button mTutorialExitButton;
    private Button mTutorialCheckButton;
    private Button mDisplayTaskButton;
    private Button mResetEditorButton;
    private LinearLayout mLessonButtonContainer;
    private SimulationLink mSimulationLink;
    private CodeEditorOperations mCodeEditorLink;

    public CodeFooterFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSimulationLink.removeSimulationListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSimulationLink = (MainActivity)getActivity();
        mSimulationLink.addSimulationListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mSimulationLink = (MainActivity)getActivity();
        int layoutId;

        if((getArguments() != null && getArguments().getBoolean(BUNDLE_TUTORIAL_MODE, false))){
            layoutId= R.layout.fragment_code_footer_tutorial;
        }
        else{
            layoutId = R.layout.fragment_code_footer;
        }

        final View rootView = inflater.inflate(layoutId, container, false);

        ButterKnife.bind(this, rootView);

        mLessonButtonContainer = (LinearLayout)rootView.findViewById(R.id.lesson_button_container);

        if(mLessonButtonContainer != null){
            mTutorialCheckButton = (Button)rootView.findViewById(R.id.lesson_check_button);

            mDisplayTaskButton = (Button)rootView.findViewById(R.id.display_task_button);

            mDisplayTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawTaskDialog();
                }
            });

            mResetEditorButton = (Button)rootView.findViewById(R.id.reset_tutorial_editor_button);

            mResetEditorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawResetConfirmDialog();
                }
            });

            mCodeEditorLink.setEditorText(getPrecode());

            mTutorialCheckButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkWork()) {
                        drawSuccessDialog();
                    } else {
                        //drawUnsuccessDialog();
                        Toast.makeText(getContext(), "Try Again!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mTutorialExitButton = (Button)rootView.findViewById(R.id.lesson_exit_button);

            mTutorialExitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeTutorialMode();
                }
            });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawTaskDialog();
                }
            }, 550);

        }

        return rootView;
    }

//    private void drawUnsuccessDialog() {
//        Activity activity = getActivity();
//        new AlertDialog.Builder(activity)
//                .setTitle("Whoops! That is not correct!")
//                .setMessage(getHintText())
//                .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .setIcon(android.R.drawable.ic_dialog_info)
//                .show();
//    }

//    private String getHintText() {
//
//    }

    private String getPrecode() {
        StringBuilder builder = new StringBuilder();
        String[] lines;
        switch(getArguments().getInt(CodeFooterFragment.BUNDLE_TUTORIAL_ID, -1)){

            case 0:
                lines = getString(R.string.lesson_exercise_precode_0).split("\n");
                break;

            case 1:
                lines = getString(R.string.lesson_exercise_precode_1).split("\n");
                break;

            case 2:
                lines = getString(R.string.lesson_exercise_precode_2).split("\n");
                break;

            default:
                return getString(R.string.lesson_exercise_text_notfound);

        }
        for(String line: lines){
            builder.append(line.trim()).append("\n");
        }
        return builder.toString();
    }

    public void closeTutorialMode() {
        mSimulationLink.removeSimulationListener(this);
        getActivity().getSupportFragmentManager().popBackStack();
    }

    private void drawTaskDialog() {
        Activity activity = getActivity();
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.lesson_task_label))
                .setMessage(getTaskText())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void drawSuccessDialog() {
        Activity activity = getActivity();
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.lesson_success))
                .setMessage(activity.getString(R.string.success_dialog_question))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        closeTutorialMode();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void drawResetConfirmDialog() {
        Activity activity = getActivity();
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.reset_code_label))
                .setMessage(activity.getString(R.string.reset_code_message))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mCodeEditorLink.setEditorText(getPrecode());
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_delete)
                        .show();
    }

    private boolean checkWork() {

        switch(getArguments().getInt(CodeFooterFragment.BUNDLE_TUTORIAL_ID, -1)){
            case 0:
                return true;

            case 1:
                return true;

            case 2:
                return mCodeEditorLink.getEditorText().toString().toUpperCase().contains("NOP")
                        && !mSimulationLink.fail();

        }

        return false;
    }

    private String getTaskText() {

        switch(getArguments().getInt(CodeFooterFragment.BUNDLE_TUTORIAL_ID, -1)){

            case 0:
                return getString(R.string.lesson_exercise_text_0);

            case 1:
                return getString(R.string.lesson_exercise_text_1);

            case 2:
                return getString(R.string.lesson_exercise_text_2);

        }
        return getString(R.string.lesson_exercise_text_notfound);
    }

    public void setEditorOperationsLink(CodeEditorOperations link){
        mCodeEditorLink = link;
    }

    public void print(String msg) {
        mOutputTextView.append(System.getProperty("line-separator", "\n") + msg);
        forceScrollToBottom();
    }


    public void printError(String msg) {
        mOutputTextView.append(System.getProperty("line-separator", "\n") + msg);
        highlight(mOutputTextView.getEditableText(), mOutputTextView.getText().toString(),
                msg, Color.parseColor(CodeFragment.Colors.REGISTER_COLOR));
        forceScrollToBottom();
    }

    private void highlight(Editable text, String textString, String string, int color) {
        Matcher matcher = Pattern.compile(string).matcher(textString);

        while(matcher.find()){
            text.setSpan(new ForegroundColorSpan(color),
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void clearOutput(){
        mOutputTextView.setText("");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("output_text", mOutputTextView.getText().toString());
    }

    public void forceScrollToBottom() {
        mOutputScroll.post(new Runnable() {
            @Override
            public void run() {
                mOutputScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onStartSimulation() {
        clearOutput();
        print(getContext().getString(R.string.simulation_started));
    }


    @Override
    public void onSendOutput(String msg) {
        print(msg);
    }

    @Override
    public void onSendErrorOutput(String msg) {
        printError(msg);
    }

    @Override
    public void onEndSimulation() {
        print(getContext().getString(R.string.simulation_ended));
        mCodeEditorLink.notifyStepIsOver();
    }
}
