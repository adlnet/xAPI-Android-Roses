package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import gov.adlnet.xapi.client.ActivityClient;
import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.ActivityDefinition;
import gov.adlnet.xapi.model.ActivityState;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.Context;
import gov.adlnet.xapi.model.ContextActivities;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.Verb;
import gov.adlnet.xapi.model.Verbs;

public abstract class ContentActivity extends android.app.Activity{
    private int _android_id;
    private int _current_slide;
    private Agent _actor;
    private String _attempt;
    private String _path;
    private String _name;
    private String _desc;

    protected void mOnCreate(Bundle savedInstanceState){
        // Set the module ID, current slide and current actor
        setAndroidId(getIntent().getExtras().getInt(getString(R.string.intent_request_code)));
        setCurrentSlide(getIntent().getExtras().getInt(getString(R.string.intent_slide)));
        setActor();
        // Be sure to set moduleId before setting path, name, and desc
        setPath(getResources().getStringArray(R.array.modules_path)[getAndroidId()]);
        setName(getResources().getStringArray(R.array.modules_name)[getAndroidId()]);
        setDesc(getResources().getStringArray(R.array.modules_desc)[getAndroidId()]);

        // Try to get attemptID, if not there generate a new attempt
        String attemptId = getIntent().getStringExtra(getString(R.string.intent_attempt));
        if (attemptId == null){
            //Generate attempt
            generateAttempt();
            // Create init act and the attempt act
            Activity init_act = createActivity(getString(R.string.app_activity_iri) + getPath(),
                    getName(), getDesc(), getString(R.string.scorm_profile_activity_type_lesson_id));

            Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + getPath() +"?attemptId=" + getCurrentAttempt(),
                    "Attempt for " + getName(),
                    "Attempt for " + getDesc(), getString(R.string.scorm_profile_activity_type_attempt_id));

            Context init_con = createContext(attempt_act, null, null, true);
            // send initialize statement
            WriteStatementTask init_stmt_task = new WriteStatementTask();
            Statement stmt = new Statement(getActor(), Verbs.initialized(), init_act);
            stmt.setContext(init_con);
            init_stmt_task.execute(stmt);

            // Update activity state
            // Get existing activity state by using SCORM activity state IRI as stateID
            // and app IRI as activityId
            MyActivityStateParams init_as_params = new MyActivityStateParams(getActor(), null,
                    getString(R.string.scorm_profile_activity_state_id), getString(R.string.app_activity_iri));

            GetActivityStateTask get_init_as_task = new GetActivityStateTask();
            MyReturnActivityStateData init_as_result = null;
            try{
                init_as_result = get_init_as_task.execute(init_as_params).get();
            }
            catch (Exception ex){
                // Will get thrown in GetActivityStateTask
            }
            JsonObject act_state;
            // Make sure there was a result
            if (init_as_result != null) {
                act_state = init_as_result.state;
            }
            else{
                act_state = null;
            }
            JsonArray attempts = new JsonArray();
            // State could not exist first time, have to make it
            if (act_state != null){
                try{
                    // Get the attempts element from the state
                    attempts = act_state.get("attempts").getAsJsonArray();
                }
                catch (Exception ex){
                    Toast.makeText(getApplicationContext(), getString(R.string.updating_as_error) + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
            // If there is an existing activity state but it doesn't have the attempts field
            // (which is wrong), this will add it
            // Update existing attempts array with the new attempt
            JsonPrimitive element = new JsonPrimitive(attempt_act.getId());
            attempts.add(element);

            JsonObject updated_state = new JsonObject();
            updated_state.add("attempts", attempts);

            // Write attempt state with updated attempts array
            // Write to attempt state that has attemptID as registration, SCORM activity state IRI
            // as stateID and app IRI as activityID
            MyActivityStateParams write_updated_as_params = new MyActivityStateParams(getActor(), updated_state,
                    getString(R.string.scorm_profile_activity_state_id), getString(R.string.app_activity_iri));
            WriteActivityStateTask write_updated_as_task = new WriteActivityStateTask();
            write_updated_as_task.execute(write_updated_as_params);
        }
        else{
            // If resuming the module, set the current attemptID
            setCurrentAttempt(attemptId);
        }

        // Set onClick listeners
        Button button = (Button) findViewById(R.id.suspend);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                returnResult(true);
            }
        });

        Button pbutton = (Button) findViewById(R.id.prev);
        pbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                previousSlide();
            }
        });

        Button nbutton = (Button) findViewById(R.id.next);
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

    protected int getAndroidId(){
        return this._android_id;
    }
    protected void setAndroidId(int a_id){
        this._android_id = a_id;
    }
    protected int getCurrentSlide(){
        return this._current_slide;
    }
    protected void setCurrentSlide(int s_id){
        this._current_slide = s_id;
    }
    protected String getCurrentAttempt(){return this._attempt;}
    protected void setCurrentAttempt(String att){this._attempt = att;}
    protected void generateAttempt(){this._attempt = UUID.randomUUID().toString();}
    protected void setActor(){
        this._actor =  new Agent(getIntent().getStringExtra(getString(R.string.intent_actor_name)),
                getIntent().getStringExtra(getString(R.string.intent_actor_email)));
    }
    protected Agent getActor(){return this._actor;}
    protected void setName(String n){this._name = n; }
    protected String getName(){return this._name;}
    protected void setPath(String p){this._path = p; }
    protected String getPath(){return this._path;}
    protected void setDesc(String d){this._desc = d; }
    protected String getDesc(){return this._desc;}

    protected void previousSlide(){
        // Send read statement then set the prev slide and replace fragment
        sendSlideChangeStatement();
        switch (getCurrentSlide()){
            case 0:
                setCurrentSlide(2);
                break;
            case 1:
                setCurrentSlide(0);
                break;
            case 2:
                setCurrentSlide(1);
                break;
        }
        replaceFragment();
    }
    protected void nextSlide(){
        // Send read statement then set the next slide and replace fragment
        sendSlideChangeStatement();
        switch (getCurrentSlide()){
            case 0:
                setCurrentSlide(1);
                break;
            case 1:
                setCurrentSlide(2);
                break;
            case 2:
                setCurrentSlide(0);
                break;
        }
        replaceFragment();
    }
    protected void replaceFragment(){
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SlideFragment frag = new SlideFragment();
        fragmentTransaction.replace(R.id.textFrag, frag).commit();
    }

    protected void sendSlideChangeStatement(){
        // Create parent module activity
        Activity lesson_attempt_act = createActivity(getString(R.string.app_activity_iri) + getPath(), getName(), getDesc(),
                getString(R.string.scorm_profile_activity_type_lesson_id));

        // Create module activity that will be object of the statement
        Activity object_act = createActivity(getString(R.string.app_activity_iri) + getPath() + "#" +
                getCurrentSlide(), getName() + " - Slide " + (getCurrentSlide() + 1),
                getDesc() + " - Slide " + (getCurrentSlide() + 1),
                getString(R.string.scorm_profile_activity_type_lesson_id));

        // Create the slide attempt activity
        Activity slide_attempt_act = createActivity(getString(R.string.app_activity_iri) + getPath() + "#" +
                getCurrentSlide() + "?attemptId=" + getCurrentAttempt(),
                "Attempt for " + getName() + " - Slide " + (getCurrentSlide() + 1),
                "Attempt for " + getDesc() + " - Slide " + (getCurrentSlide() + 1),
                getString(R.string.scorm_profile_activity_type_attempt_id));

        // Create the module attempt activity
        Activity parent_attempt_act = createActivity(getString(R.string.app_activity_iri) + getPath() + "?attemptId=" + getCurrentAttempt(),
                "Attempt for " + getName(), "Attempt for " + getDesc(),
                getString(R.string.scorm_profile_activity_type_attempt_id));

        // Create context and verb for the statement, then create statement and send it
        Context slide_con = createContext(lesson_attempt_act, slide_attempt_act, parent_attempt_act, false);
        HashMap<String, String> verb_lang = new HashMap<>();
        verb_lang.put("en-US", "read");
        Verb verb = new Verb(getString(R.string.read_verb), verb_lang);
        WriteStatementTask slide_init_stmt_task = new WriteStatementTask();
        Statement stmt = new Statement(getActor(), verb, object_act);
        stmt.setContext(slide_con);
        slide_init_stmt_task.execute(stmt);
    }

    protected Context createContext(Activity lesson_attempt_act, Activity slide_attempt_act, Activity parent_attempt_act, boolean init){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<>();
        // Add application activity
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.app_activity_name), getString(R.string.app_activity_description),
                getString(R.string.scorm_profile_activity_type_course_id)));
        con_act_list.add(lesson_attempt_act);

        // If the statement isn't init then add the slide attempt activity
        /// and parent attempt activity
        if (!init){
            con_act_list.add(slide_attempt_act);
            ArrayList<Activity> parent_act_list = new ArrayList<>();
            parent_act_list.add(parent_attempt_act);
            con_acts.setParent(parent_act_list);
        }

        ArrayList<Activity> cat_act_list = new ArrayList<>();
        // Add category activity per the SCORM profile
        cat_act_list.add(new Activity(getString(R.string.scorm_profile_activity_category_id)));
        con_acts.setCategory(cat_act_list);
        con_acts.setGrouping(con_act_list);
        con.setContextActivities(con_acts);
        return con;
    }
    protected Activity createActivity(String act_id, String name, String desc, String type_id){
        Activity act = new Activity(act_id);
        ActivityDefinition act_def = new ActivityDefinition();
        act_def.setName(new HashMap<String, String>());
        act_def.getName().put("en-US", name);
        act_def.setDescription(new HashMap<String, String>());
        act_def.getDescription().put("en-US", desc);
        act_def.setType(type_id);
        act.setDefinition(act_def);
        return act;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rose, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed(){
        // Pressing back terminates the module, this assumes you read the current slide
        // you terminated it on
        sendSlideChangeStatement();
        returnResult(false);
    }
    protected void returnResult(boolean suspended){
        Intent returnIntent = new Intent();
        // Include the current attemptId and slideId to send back to
        // the main activity
        returnIntent.putExtra(getString(R.string.intent_attempt), getCurrentAttempt());
        returnIntent.putExtra(getString(R.string.intent_slide), getCurrentSlide());
        // If suspended button is pushed, it will send RESULT_CANCELLED
        if (suspended){
            setResult(RESULT_CANCELED, returnIntent);
        }
        else{
            setResult(RESULT_OK, returnIntent);
        }
        finish();
    }

    // Inner class to write statements to the LRS - returns boolean success and string result
    private class WriteStatementTask extends AsyncTask<Statement, Void, Pair<Boolean, String>> {
        protected Pair<Boolean, String> doInBackground(Statement... params){
            boolean success = true;
            String content;
            // Try to send statement, if error set success and content to error message
            try{
                StatementClient client = new StatementClient(getString(R.string.lrs_endpoint),
                        getString(R.string.lrs_user), getString(R.string.lrs_password));
                content = client.postStatement(params[0]);
            }catch(Exception ex){
                success = false;
                content = ex.getLocalizedMessage();
            }
            return new Pair<>(success, content);
        }

        // Called after doInBackground for updating UI
        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                // Send toast message with error
                Toast.makeText(getApplicationContext(), getString(R.string.statement_write_error) + p.second,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to get activity states from the LRS - returns activity state data
    private class GetActivityStateTask extends AsyncTask<MyActivityStateParams, Void, MyReturnActivityStateData>{
        protected MyReturnActivityStateData doInBackground(MyActivityStateParams... params){
            JsonObject state;
            boolean success = true;
            // Try to get the activity state
            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                // This will retrieve an array of states (should only be one in the array)
                ActivityState as = new ActivityState(params[0].actID, params[0].stId, params[0].a);
                state = ac.getActivityState(as);
            }
            catch (Exception ex){
                success = false;
                state = null;
            }
            return new MyReturnActivityStateData(success, state);
        }

        // Called after doInBackground for UI
        protected void onPostExecute(MyReturnActivityStateData asd){
            if (!asd.success){
                // Return toast with error message
                Toast.makeText(getApplicationContext(), getString(R.string.get_as_error), Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to write activity state to the LRS - returns boolean success and string result
    private class WriteActivityStateTask extends AsyncTask<MyActivityStateParams, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(MyActivityStateParams... params){
            boolean success;
            String content;
            // Try to write the activity state
            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                ActivityState as = new ActivityState(params[0].actID, params[0].stId, params[0].a);
                as.setState(params[0].state);
                success = ac.postActivityState(as);
                content = "";
            }
            catch (Exception ex){
                success = false;
                content = ex.getLocalizedMessage();
            }
            return new Pair<>(success, content);
        }

        // Called after doInBackground for UI
        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                // Return toast with error message
                Toast.makeText(getApplicationContext(), getString(R.string.write_as_error) + p.second,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to pass activity state data to tasks
    protected class MyActivityStateParams{
        final Agent a;
        final JsonObject state;
        final String actID;
        final String stId;

        MyActivityStateParams(Agent a, JsonObject s, String stID, String actID){
            this.a = a;
            this.state = s;
            this.actID = actID;
            this.stId = stID;
        }
    }
    // inner class to return activity state data back to activity
    protected class MyReturnActivityStateData{
        final boolean success;
        final JsonObject state;

        MyReturnActivityStateData(boolean s, JsonObject state){
            this.success = s;
            this.state = state;
        }
    }
}
