package com.syncedsoftware.iassembly;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DocActivity extends AppCompatActivity {

    public static final int GET_TUTORIAL_LINK = 0;
    public static final String EXTRAS_TUTORIAL_ID = "TUTORIAL_ID";
    @Bind(R.id.web_view_progressBar) ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc);

        ButterKnife.bind(this);

        mProgressBar.setVisibility(View.VISIBLE);

        Intent extras = getIntent();

        CharSequence title;
        String content;

        final int lessonId = (extras.getIntExtra("index", -1));

        switch(lessonId){

            case 0:
                title = getText(R.string.docs_title_lesson_0);
                content = getString(R.string.docs_content_lesson_0);
                break;

            case 1:
                title = getText(R.string.docs_title_lesson_1);
                content = getString(R.string.docs_content_lesson_1);
                break;

            case 2:
                title = getText(R.string.docs_title_lesson_2);
                content = getString(R.string.docs_content_lesson_2);
                break;

            default:
                title = getString(R.string.docs_title_notfound);
                content = getString(R.string.docs_content_notfound);

        }

        ((TextView)findViewById(R.id.doc_detail_title_textView)).setText(title);
        WebView webView = ((WebView)findViewById(R.id.doc_detail_content_textView));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(content, "text/html", "utf-8");
        mProgressBar.setVisibility(View.GONE);

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
