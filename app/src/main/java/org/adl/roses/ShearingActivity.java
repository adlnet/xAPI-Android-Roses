package org.adl.roses;

import android.os.Bundle;

public class ShearingActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_shearing_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body);
        mOnCreate(savedInstanceState);
    }
}
