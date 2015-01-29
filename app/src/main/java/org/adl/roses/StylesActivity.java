package org.adl.roses;

import android.os.Bundle;

public class StylesActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_styles_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_styles);
        mOnCreate(savedInstanceState);
    }
}
