package org.adl.roses;

import android.os.Bundle;

public class HybridsActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_hybrids_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body);
        mOnCreate(savedInstanceState);
    }
}
