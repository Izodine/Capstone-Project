package com.syncedsoftware.iassembly;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.syncedsoftware.iassembly.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AboutActivity extends AppCompatActivity {

    @Bind(R.id.legal_notice_textView) TextView mLegalTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        mLegalTextView.setText(GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(this));
    }
}
