package org.adl.roses;

import android.os.Bundle;

public class SymbolismActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_symbolism_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body);
        mOnCreate(savedInstanceState);
    }
}
