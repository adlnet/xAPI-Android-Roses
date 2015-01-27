package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.Context;
import gov.adlnet.xapi.model.Verbs;

public class FloristryActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_styles_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floristry);
        mOnCreate(savedInstanceState);
    }
}
