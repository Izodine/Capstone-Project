package com.syncedsoftware.iassembly;

/**
 * Created by Anthony M. Santiago on 1/26/16.
 */

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;
import com.syncedsoftware.iassembly.iasm_base.Simulation;
import com.syncedsoftware.iassembly.iasm_base.instructions.InstructionSet;
import com.syncedsoftware.iassembly.iasm_base.registers.RegisterNames;
import com.syncedsoftware.iassembly.provider.DataContract;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import butterknife.Bind;
import butterknife.ButterKnife;

public class CodeFragment extends Fragment implements TutorialLink, CodeEditorOperations,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>{

    public static final int HIGHLIGHT_DELAY_MILLIS = 500;
    public static final String FOOTER_FRAGMENT_TAG = "FOOTER_FRAGMENT_TAG";
    public static final String FILE_NAME_BUNDLE_ENTRY = "FILE_NAME";

    private static String[] mKeywordsArray;
    private CodeFooterFragment mFooterFragment;
    private static String[] mInstructionsArray;
    private static final String TAG = "drive";
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private GoogleApiClient mGoogleApiClient;
    private String mProgramToSave;
    private Handler mHighlightHandler = new Handler();
    private Editable mEditorText;
    private SimulationLink mSimulationLink;
    private String mLoadedFile = "";
    private boolean mIsStepMode;
    private boolean mAwaitingDriveSave = false;
    AnalyticsApplication application;
    Tracker mTracker;

    @Bind(R.id.line_numbers_textView)  TextView mLineNumberTextView;
    @Bind(R.id.editor_edittext)  EditText mHighlightedWidget;

    private Runnable mHighlightRunnable = new Runnable(){
        @Override
        public void run() {
            // draw lines
            String textString = mEditorText.toString();
            mLineNumberTextView.setText("");
            for(int line = 0; line < mHighlightedWidget.getLineCount(); line++){
                String lineString = mLineNumberTextView.getText().toString();
                mLineNumberTextView.setText(String.format("%s%d\n", lineString, line + 1));
            }

            for(String string: getKeywordsList()) {
                highlight( mEditorText, textString, string + "|[.]", Color.parseColor(Colors.KEYWORD_COLOR));
            }

            for(String string: getInstructionsList()){
                highlight( mEditorText, textString, string, Color.parseColor(Colors.INSTRUCTION_COLOR));
            }

            for(String string: RegisterNames.list){
                highlight( mEditorText, textString, string, Color.parseColor(Colors.REGISTER_COLOR));
            }

            if(textString.length() > 0){
                highlight( mEditorText, textString, "[^a-zA-Z][0-9][^a-zA-Z]", Color.parseColor(Colors.NUMBER_COLOR));
                highlight( mEditorText, textString, "[,|;|{|}|(|)|:]", Color.parseColor(Colors.NUMBER_COLOR));
                highlight( mEditorText, textString, ";.*", Color.parseColor(Colors.COMMENT_COLOR));
            }
        }
    };



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        authorizeDrive();
        mSimulationLink = ((MainActivity)context);
    }

    public CodeFragment() {
        setArguments(new Bundle());
    }

    private void setEditorCode(int lessonId) {

        switch(lessonId){

            case 0:
                mHighlightedWidget.setText(getString(R.string.lesson_exercise_precode_0));
                break;

            case 1:
                mHighlightedWidget.setText(getString(R.string.lesson_exercise_precode_1));
                break;
        }

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id){

            case R.id.action_about:
                startActivity(new Intent(getContext(), AboutActivity.class));
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("View About")
                        .build());
                return true;

            case R.id.action_supported_ops:
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("View Doc Index")
                        .build());
                startActivity(new Intent(getContext(),SupperedOpsActivity.class));
                return true;

            case R.id.action_submit_feedback:
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("View/Submit Feedback")
                        .build());

                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://goo.gl/forms/W4MBPehsJI")));
                return true;

            case R.id.action_delete:
                promptForDelete();
                return true;

            case R.id.action_load:
                promptForFileLoad();
                return true;

            case R.id.action_save:

                new AlertDialog.Builder(getContext())
                        .setTitle(getContext().getString(R.string.save_program_label))
                        .setMessage(getContext().getString(R.string.save_choice_drive_local_label))
                        .setPositiveButton(getContext().getString(R.string.local_storage_label), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mTracker.send(new HitBuilders.EventBuilder()
                                        .setCategory("Action")
                                        .setAction("Save Local Storage")
                                        .build());
                                if (mLoadedFile.isEmpty()) {
                                    promptForSave();
                                } else
                                    promptForOverwrite(mLoadedFile);
                            }
                        })
                        .setNeutralButton("Drive", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (getEditorText() == null) {
                                    Toast.makeText(getContext(), "No data to save!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    mTracker.send(new HitBuilders.EventBuilder()
                                            .setCategory("Action")
                                            .setAction("Save Google Drive")
                                            .build());
                                    mProgramToSave = getEditorText().toString();
                                    saveFileToDrive();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_menu_save)
                        .show();

                return true;

            case R.id.action_new:

                new AlertDialog.Builder(getContext())
                        .setTitle(getContext().getString(R.string.new_program_label))
                        .setMessage(getContext().getString(R.string.new_program_question))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mTracker.send(new HitBuilders.EventBuilder()
                                        .setCategory("Action")
                                        .setAction("New")
                                        .build());
                                setEditorText("");
                                mLoadedFile = "";
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_menu_delete)
                        .show();

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Create a new file and save it to Drive.
     */
    private void saveFileToDrive() {
        mAwaitingDriveSave = true;
        if(!authorizeDrive()) return;
        // Start by creating a new contents, and setting a callback.
        final String programToSave = mProgramToSave;
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.

                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();

                        byte[] bytes = programToSave.getBytes(Charset.forName("utf-8"));
                        try {
                            outputStream.write(bytes);
                        } catch (IOException e1) {
                            Toast.makeText(getContext(), R.string.file_error_unable, Toast.LENGTH_SHORT).show();
                        }
                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("text/plain").setTitle(getContext().getString(R.string.default_drive_file_label)).build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            getActivity().startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                        mAwaitingDriveSave = false;
                    }
                });
    }

    private boolean authorizeDrive() {
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) return true;
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
        return false;
    }

    private void promptForFileLoad() {

        String[] projection = new String[]{"name","program"};
        Cursor fileCursor =
                getContext().getContentResolver().query(DataContract.CONTENT_URI,
                        projection, null, null, null);

        final ArrayList<String> fileList = new ArrayList<>();

        if(fileCursor.moveToFirst()){
            int colNameIndex = fileCursor.getColumnIndex(DataContract.COLUMN_NAME);
            while(fileCursor.moveToNext()){
                fileList.add(fileCursor.getString(colNameIndex));
            }
        }
        else{
            fileCursor.close();
            Toast.makeText(getContext(), R.string.err_no_programs, Toast.LENGTH_SHORT).show();
            return;
        }
        fileCursor.close();

        ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(getContext(),
                R.layout.file_chooser_view_item,
                R.id.filename_text_view,
                fileList);

        final CodeFragment callbacks = this;

        new AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.load_program_label))
                .setAdapter(fileAdapter, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSimulationLink.stopSimulation();
                        Bundle args = new Bundle();
                        mLoadedFile = fileList.get(which);
                        args.putString(FILE_NAME_BUNDLE_ENTRY, mLoadedFile);
                        getActivity().getSupportLoaderManager().initLoader(1, args, callbacks);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_menu_edit)
                .show();
    }

    private void promptForSave() {
        final Context context = getContext();
        final EditText input = new EditText(context);
        new AlertDialog.Builder(getContext())
                .setTitle(context.getString(R.string.save_program_label))
                .setMessage(context.getString(R.string.save_program_description))
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = input.getText().toString();
                        if (fileName.isEmpty()) {
                            Toast.makeText(getContext(), R.string.err_save_failed_noname, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Check if file already exists
                        Cursor cursor = getContext().getContentResolver().query(DataContract.CONTENT_URI,
                                new String[]{"name"}, "name = ?", new String[]{input.getText().toString()}, null);

                        if(cursor.moveToFirst()){
                            Toast.makeText(getContext(), R.string.err_file_name_taken, Toast.LENGTH_LONG).show();
                            return;
                        }

                        Editable editable = mHighlightedWidget.getText();

                        if (editable == null) {
                            Toast.makeText(context, R.string.err_file_is_null, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SaveTask saveTask = new SaveTask(fileName,editable.toString(), false);
                        saveTask.execute(getContext());
                        Toast.makeText(context, R.string.saving_label, Toast.LENGTH_SHORT).show();

                        cursor.close();

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_menu_save)
                .show();
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
                DataContract.CONTENT_URI,
                new String[]{"name","program"},
                " name = ?",
                new String[]{args.getString(FILE_NAME_BUNDLE_ENTRY)},
                null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()){
            String editorText = data.getString(data.getColumnIndex(DataContract.COLUMN_PROGRAM));
            setEditorText(editorText);
        }
        getActivity().getSupportLoaderManager().destroyLoader(1);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private static class SaveTask extends AsyncTask<Context, Void, Void> {

        private String fileName;
        private String fileContents;
        private boolean overwrite;

        public SaveTask(String fileName, String fileContents, boolean overwrite){
            this.fileName = fileName;
            this.fileContents = fileContents;
            this.overwrite = overwrite;
        }

        @Override
        protected Void doInBackground(Context... params) {
            ContentValues values = new ContentValues();
            values.put(DataContract.COLUMN_NAME, fileName);
            values.put(DataContract.COLUMN_PROGRAM, fileContents);
            if(overwrite){
                params[0].getContentResolver().update(DataContract.CONTENT_URI, values, null, null);
            }
            else {
                params[0].getContentResolver().insert(DataContract.CONTENT_URI, values);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void promptForOverwrite(final String fileName) {
        final Context context = getContext();
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.overwrite_label))
                .setMessage(context.getString(R.string.overwrite_message))
                .setPositiveButton(R.string.overwrite_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (fileName.isEmpty()) {
                            Toast.makeText(getContext(), R.string.err_save_failed_noname, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Editable editable = mHighlightedWidget.getText();

                        SaveTask saveTask = new SaveTask(fileName, editable.toString(), true);
                        saveTask.execute(getContext());
                        Toast.makeText(context, R.string.overwriting_label, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_menu_save)
                .show();
    }

    private void promptForDelete() {
        final Context context = getContext();
        String[] projection = new String[]{"id","name","program"};
        final Cursor fileCursor =
                getContext().getContentResolver().query(DataContract.CONTENT_URI, projection, null, null, null);

        final ArrayList<String> fileList = new ArrayList<>();

        if(fileCursor.moveToFirst()){
            int colNameIndex = fileCursor.getColumnIndex(DataContract.COLUMN_NAME);
            while(fileCursor.moveToNext()){
                fileList.add(fileCursor.getString(colNameIndex));
            }
        }
        else{
            Toast.makeText(getContext(), R.string.err_no_programs, Toast.LENGTH_SHORT).show();
            return;
        }
        fileCursor.close();
        ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(context,
                R.layout.file_chooser_view_item,
                R.id.filename_text_view,
                fileList);

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_program_label))
                .setAdapter(fileAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getContext().getContentResolver().delete(DataContract.CONTENT_URI,
                                "name = ?", new String[]{fileList.get(which)});
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFooterFragment.setEditorOperationsLink(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        application = (AnalyticsApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        View rootView = inflater.inflate(R.layout.fragment_code, container, false);
        ButterKnife.bind(this, rootView);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        mFooterFragment = new CodeFooterFragment();

        manager.beginTransaction().replace(R.id.code_footer_container, mFooterFragment, FOOTER_FRAGMENT_TAG).commit();

        final Button executeButton = (Button)rootView.findViewById(R.id.execute_button);
        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsStepMode)
                    mSimulationLink.startSimulation(getLines(), Simulation.EXECUTE_MODE, new Handler());
                else
                    mSimulationLink.stopSimulation();
            }
        });

        Button stepButton = (Button)rootView.findViewById(R.id.step_button);

        stepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsStepMode) {
                    mIsStepMode = true;
                    executeButton.setText(R.string.button_label_stop);
                    mSimulationLink.startSimulation(getLines(), Simulation.STEP_MODE, new Handler());
                } else {
                    mSimulationLink.stepSimulation();
                }
            }
        });

        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mHighlightedWidget.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mLineNumberTextView.scrollTo(0, view.getScrollY());
                return false;
            }
        });

        //mLineNumberTextView = (TextView)rootView.findViewById(R.id.line_numbers_textView);
        mHighlightedWidget.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mHighlightHandler.removeCallbacks(mHighlightRunnable);
            }

            @Override
            public void afterTextChanged(Editable text) {
                mHighlightHandler.removeCallbacks(mHighlightRunnable);
                mEditorText = text;
                mHighlightHandler.postDelayed(mHighlightRunnable, HIGHLIGHT_DELAY_MILLIS);
            }
        });

        mHighlightedWidget.setHorizontallyScrolling(true);

        return rootView;
    }


    private ArrayList<String> getLines() {
        if(mEditorText == null){
            return new ArrayList<>(0);
        }
        String[] splitLines  = mEditorText.toString().split(System.getProperty("line-separator", "\n"));
        ArrayList<String> lines = new ArrayList<>(50);

        Collections.addAll(lines, splitLines);

        return lines;
    }

    private void highlight(Editable text, String textString, String string, int color) {
        Matcher matcher = Pattern.compile(string, Pattern.CASE_INSENSITIVE).matcher(textString);

        while(matcher.find()){
            text.setSpan(new ForegroundColorSpan(color),
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @NonNull
    private String[] getInstructionsList() {
        if(mInstructionsArray == null) mInstructionsArray = InstructionSet.allInstructions();
        return mInstructionsArray;
    }

    @NonNull
    private static String[] getKeywordsList() {
        if(mKeywordsArray == null) mKeywordsArray = InstructionSet.allKeywords();
        return mKeywordsArray;
    }

    @Override
    public void setTutorial(int tutorialId) {
        CodeFooterFragment footerFragment = new CodeFooterFragment();
        footerFragment.setEditorOperationsLink(this);
        Bundle args = new Bundle();
        args.putBoolean(CodeFooterFragment.BUNDLE_TUTORIAL_MODE, true);
        args.putInt(CodeFooterFragment.BUNDLE_TUTORIAL_ID, tutorialId);
        footerFragment.setArguments(args);
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.code_footer_container, footerFragment, "FOOTER_FRAGMENT")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void closeTutorialMode() {
        CodeFooterFragment footer = ((CodeFooterFragment) getActivity()
                .getSupportFragmentManager()
                .findFragmentByTag("FOOTER_FRAGMENT"));

        if(footer != null)
            footer.closeTutorialMode();
    }

    @Override
    public Editable getEditorText() {
        return mHighlightedWidget.getText();
    }

    @Override
    public void setEditorText(String text) {
        mHighlightedWidget.setText(text);
    }

    @Override
    public void notifyStepIsOver() {
        mIsStepMode = false;
        ((Button)getView().findViewById(R.id.execute_button)).setText(R.string.execute);
    }


    @Override
    public void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnectionFailed(final ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), result.getErrorCode(), 0).show();
            return;
        }
        promptForDriveAuth(result);

    }

    private void promptForDriveAuth(final ConnectionResult result) {
        new AlertDialog.Builder(getContext())
                .setTitle("Drive Connect Failed")
                .setMessage("Would you like to try again?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            result.startResolutionForResult(getActivity(), REQUEST_CODE_RESOLUTION);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Exception while starting resolution activity", e);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient onconnected");
        if(mAwaitingDriveSave){
            saveFileToDrive();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }


    public static class Colors{
        private Colors(){}

        public static final String KEYWORD_COLOR = "#93C763";
        public static final String COMMENT_COLOR = "#006400";
        public static final String INSTRUCTION_COLOR = "#678CB1";
        public static final String NUMBER_COLOR = "#FFCD22";
        public static final String REGISTER_COLOR = "#A082BD";

    }
}
