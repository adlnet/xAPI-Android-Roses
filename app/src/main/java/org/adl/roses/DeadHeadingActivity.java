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

public class DeadHeadingActivity extends ContentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_deadheading_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dead_heading);

        // Set the module ID and current slide
        setAndroidId(getIntent().getExtras().getInt("requestCode"));
        setCurrentSlide(getIntent().getExtras().getInt("slideId"));

        // Set or generate the attempt ID
        String attemptId = getIntent().getExtras().getString("attemptId", null);
        if (attemptId == null){
            generateAttempt();
            // Get actor and send initialized statement and first slide statement
            Agent actor = getActor();
            Activity init_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_deadheading_path)
                            +"?attemptId=" + getCurrentAttempt(), getString(R.string.mod_deadheading_name),
                    getString(R.string.mod_deadheading_description));
            Context init_con = createContext(null, null, null, true);

            // send initialize statement
            MyStatementParams init_params = new MyStatementParams(actor, Verbs.initialized(), init_act, init_con);
            WriteStatementTask init_stmt_task = new WriteStatementTask();
            init_stmt_task.execute(init_params);
        }
        else{
            setCurrentAttempt(attemptId);
        }

        // Set onClick listeners
        Button button = (Button) findViewById(R.id.dhSuspend);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                returnResult(true);
            }
        });

        Button pbutton = (Button) findViewById(R.id.dhPrev);
        pbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                previousSlide();
            }
        });

        Button nbutton = (Button) findViewById(R.id.dhNext);
        nbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                nextSlide();
            }
        });

        // Check that the activity is u sing he layout version with
        // the fragment_container FameLayout
        if (findViewById(R.id.textFrag) != null){

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
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
