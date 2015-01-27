package org.adl.roses;

import android.os.Bundle;

public class PruningActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_pruning_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pruning);
        mOnCreate(savedInstanceState);
    }
}
