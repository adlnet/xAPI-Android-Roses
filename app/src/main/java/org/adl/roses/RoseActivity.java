package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.Context;
import gov.adlnet.xapi.model.Verbs;

public class RoseActivity extends ContentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_what_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rose);

        // Set the module ID and current slide
        setAndroidId(getIntent().getExtras().getInt("requestCode"));
        setCurrentSlide(getIntent().getExtras().getInt("slideId"));

        // Set or generate the attempt ID
        String attemptId = getIntent().getExtras().getString("attemptId", null);
        if (attemptId == null){
            generateAttempt();
        }
        else{
            setCurrentAttempt(attemptId);
        }

        // Get actor and send initialized statement
        Agent actor = getActor();
        Activity what_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_what_path) + "#" +
                        getCurrentSlide() +"?attemptId=" + getCurrentAttempt(),
                getString(R.string.mod_what_name), getString(R.string.mod_what_description));
        Context what_con = createContext(getString(R.string.mod_what_path), getCurrentAttempt(),
                getString(R.string.mod_what_name), getString(R.string.mod_what_description));

        // send initialize statements and launch activity
        MyStatementParams what_init_params = new MyStatementParams(actor, Verbs.initialized(), what_act, what_con);
        WriteStatementTask what_init_stmt_task = new WriteStatementTask();
        what_init_stmt_task.execute(what_init_params);


        Button button = (Button) findViewById(R.id.whatSuspend);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                returnResult(true);
            }
        });

        Button pbutton = (Button) findViewById(R.id.whatPrev);
        pbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                previousSlide();
            }
        });

        Button nbutton = (Button) findViewById(R.id.whatNext);
        nbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                nextSlide();
            }
        });

        // Check that the activity is u sing he layout version with
        // the fragment_container FameLayout
        if (findViewById(R.id.textFrag) != null){

            // However, if we'ere being restored from a previous state,
            // then we don't need to do anything and hsould return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null){
                return;
            }

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SlideFragment frag = new SlideFragment();
            fragmentTransaction.add(R.id.textFrag, frag).commit();
        }
    }
}
