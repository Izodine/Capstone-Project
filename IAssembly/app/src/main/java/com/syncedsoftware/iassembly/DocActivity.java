package com.syncedsoftware.iassembly;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.syncedsoftware.iassembly.R;

public class DocActivity extends AppCompatActivity {

    public static final int GET_TUTORIAL_LINK = 0;
    public static final String EXTRAS_TUTORIAL_ID = "TUTORIAL_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc);

        Intent extras = getIntent();

        CharSequence title;
        String content;

        final int lessonId = (extras.getIntExtra("index", -1));

        switch(lessonId){

            case 0:
                title = getText(R.string.docs_title_lessonOne);
                content = getString(R.string.docs_content_lessonOne);
                break;

            case 1:
                title = getText(R.string.docs_title_lessonTwo);
                content = getString(R.string.docs_content_lessonTwo);
                break;

            default:
                title = getString(R.string.docs_title_notfound);
                content = getString(R.string.docs_content_notfound);

        }

        ((TextView)findViewById(R.id.doc_detail_title_textView)).setText(title);
        ((TextView)findViewById(R.id.doc_detail_content_textView)).setText(content);

        findViewById(R.id.launch_exercise_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(EXTRAS_TUTORIAL_ID, lessonId);
                setResult(GET_TUTORIAL_LINK, intent);
                finish();
            }
        });
    }
}
