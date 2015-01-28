package org.adl.roses;

import android.os.Bundle;

public class RoseActivity extends ContentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_what_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rose);
        mOnCreate(savedInstanceState);
    }
}
