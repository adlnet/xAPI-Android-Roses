package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.Context;
import gov.adlnet.xapi.model.ContextActivities;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.Verb;
import gov.adlnet.xapi.model.Verbs;

/**
 * Created by lou on 1/12/15.
 */
public abstract class ContentActivity extends ActionBarActivity{
    private int _android_id;
    private int _current_slide;
    private Agent _actor;
    private String _attempt;

    protected void mOnCreate(Bundle savedInstanceState){
        // Set the module ID and current slide
        setAndroidId(getIntent().getExtras().getInt(getString(R.string.intent_request_code)));
        setCurrentSlide(getIntent().getExtras().getInt(getString(R.string.intent_slide)));
        setActor();

        ModuleData md = setModuleData(getAndroidId(), false);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;

        // Set or generate the attempt ID
        String attemptId = getIntent().getExtras().getString(getString(R.string.intent_attempt), null);
        if (attemptId == null){
            generateAttempt();
            // Get actor and send initialized statement and first slide statement

            Activity init_act = createActivity(getString(R.string.app_activity_iri) + path,
                    name, desc, getString(R.string.scorm_profile_activity_type_lesson_id));

            Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path +"?attemptId=" + getCurrentAttempt(),
                    "Attempt for " + name,
                    "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));

            Context init_con = createContext(attempt_act, null, null, true);
            // send initialize statement
            MyStatementParams init_params = new MyStatementParams(getActor(), Verbs.initialized(), init_act, init_con);
            WriteStatementTask init_stmt_task = new WriteStatementTask();
            init_stmt_task.execute(init_params);

            // Update activity state
            // Get existing activity state by using SCORM activity state IRI as stateID
            // and app IRI as activityId
            MyActivityStateParams init_as_params = new MyActivityStateParams(getActor(), null, null,
                    getString(R.string.scorm_profile_activity_state_id), getString(R.string.app_activity_iri));
            GetActivityStateTask get_init_as_task = new GetActivityStateTask();
            MyReturnActivityStateData init_as_result = null;
            try{
                init_as_result = get_init_as_task.execute(init_as_params).get();
            }
            catch (Exception ex){
                // Will get thrown in GetActivityStateTask
            }

            JsonObject act_state = init_as_result.state;
            JsonArray attempts = new JsonArray();
            // State could not exist first time, have to make it
            if (act_state != null){
                try{
                    // Get the attempts element from the state
                    attempts = act_state.get("Attempts").getAsJsonArray();
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
            updated_state.add("Attempts", attempts);

            // Write attempt state with updated attempts array
            // Write to attempt state that has attemptID as registration, SCORM activity state IRI
            // as stateID and app IRI as activityID
            MyActivityStateParams write_updated_as_params = new MyActivityStateParams(getActor(), updated_state, null,
                    getString(R.string.scorm_profile_activity_state_id), getString(R.string.app_activity_iri));
            WriteActivityStateTask write_updated_as_task = new WriteActivityStateTask();
            write_updated_as_task.execute(write_updated_as_params);
        }
        else{
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
        this._actor =  new Agent(getIntent().getExtras().getString(getString(R.string.intent_actor_name)),
                getIntent().getExtras().getString(getString(R.string.intent_actor_email)));
    }
    protected Agent getActor(){return this._actor;}

    protected void previousSlide(){
        // If first opening the module - don't send extra read statement for current slide
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
        // If first opening the module - don't send extra read statement for current slide
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
        ModuleData md = setModuleData(getAndroidId(), false);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;

        Activity lesson_attempt_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc,
                getString(R.string.scorm_profile_activity_type_lesson_id));

        Activity object_act = createActivity(getString(R.string.app_activity_iri) + path + "#" +
                getCurrentSlide(), name + " - Slide " + (getCurrentSlide() + 1),
                desc + " - Slide " + (getCurrentSlide() + 1),
                getString(R.string.scorm_profile_activity_type_lesson_id));

        Activity slide_attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "#" +
                getCurrentSlide() + "?attemptId=" + getCurrentAttempt(),
                "Attempt for " + name + " - Slide " + (getCurrentSlide() + 1),
                "Attempt for " + desc + " - Slide " + (getCurrentSlide() + 1),
                getString(R.string.scorm_profile_activity_type_attempt_id));

        Activity parent_attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + getCurrentAttempt(),
                "Attempt for " + name, "Attempt for " + desc,
                getString(R.string.scorm_profile_activity_type_attempt_id));

        Context slide_con = createContext(lesson_attempt_act, slide_attempt_act, parent_attempt_act, false);
        HashMap<String, String> verb_lang = new HashMap<String, String>();
        verb_lang.put("en-US", "read");
        Verb verb = new Verb(getString(R.string.read_verb), verb_lang);
        MyStatementParams slide_init_params = new MyStatementParams(getActor(), verb, object_act, slide_con);
        WriteStatementTask slide_init_stmt_task = new WriteStatementTask();
        slide_init_stmt_task.execute(slide_init_params);
    }

    protected Context createContext(Activity lesson_attempt_act, Activity slide_attempt_act, Activity parent_attempt_act, boolean init){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        // Add application activity
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.app_activity_name), getString(R.string.app_activity_description),
                getString(R.string.scorm_profile_activity_type_course_id)));
        con_act_list.add(lesson_attempt_act);

        // If the statement isn't init then add the parent activity to the context
        if (!init){
            con_act_list.add(slide_attempt_act);
            ArrayList<Activity> parent_act_list = new ArrayList<Activity>();
            parent_act_list.add(parent_attempt_act);
            con_acts.setParent(parent_act_list);
        }
        ArrayList<Activity> cat_act_list = new ArrayList<Activity>();
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

    private ModuleData setModuleData(int moduleId, Boolean mc){
        ModuleData returnData = new ModuleData();
        returnData.setPath(getResources().getStringArray(R.array.modules_path)[moduleId]);
        returnData.setName(getResources().getStringArray(R.array.modules_name)[moduleId]);
        returnData.setDesc(getResources().getStringArray(R.array.modules_desc)[moduleId]);

        if (mc){
            returnData.setModule_class(moduleId);
        }
        return returnData;
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
        returnIntent.putExtra(getString(R.string.intent_attempt), getCurrentAttempt());
        returnIntent.putExtra(getString(R.string.intent_slide), getCurrentSlide());
        if (suspended){
            setResult(RESULT_CANCELED, returnIntent);
        }
        else{
            setResult(RESULT_OK, returnIntent);
        }
        finish();
    }

    protected class WriteStatementTask extends AsyncTask<MyStatementParams, Void, Pair<Boolean, String>> {
        protected Pair<Boolean, String> doInBackground(MyStatementParams... params){
            Statement stmt = new Statement();
            stmt.setActor(params[0].ag);
            stmt.setVerb(params[0].v);
            stmt.setObject(params[0].a);
            stmt.setContext(params[0].c);

            boolean success = true;
            String content;
            try{
                StatementClient client = new StatementClient(getString(R.string.lrs_endpoint),
                        getString(R.string.lrs_user), getString(R.string.lrs_password));
                content = client.publishStatement(stmt);
            }catch(Exception ex){
                success = false;
                content = ex.getLocalizedMessage();
            }

            return new Pair<Boolean, String>(success, content);
        }

        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                Toast.makeText(getApplicationContext(), getString(R.string.statement_write_error) + p.second,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    protected class MyStatementParams{
        Agent ag;
        Verb v;
        Activity a;
        Context c;
        String aID;

        MyStatementParams(Agent ag, Verb v, Activity a, Context c){
            this.ag = ag;
            this.v = v;
            this.a = a;
            this.c = c;
        }
        MyStatementParams(Agent ag, Verb v, String a){
            this.ag = ag;
            this.v = v;
            this.aID = a;
        }
    }
    protected class GetActivityStateTask extends AsyncTask<MyActivityStateParams, Void, MyReturnActivityStateData>{
        protected MyReturnActivityStateData doInBackground(MyActivityStateParams... params){
            JsonObject state;
            boolean success = true;
            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                // This will retrieve an array of states (should only be one in the array)
                state = ac.getActivityState(params[0].actID, params[0].a, params[0].r, params[0].stId);
            }
            catch (Exception ex){
                success = false;
                state = null;
            }
            return new MyReturnActivityStateData(success, state);
        }

        protected void onPostExecute(MyReturnActivityStateData asd){
            if (!asd.success){
                Toast.makeText(getApplicationContext(), getString(R.string.get_as_error), Toast.LENGTH_LONG).show();
            }
        }
    }
    protected class WriteActivityStateTask extends AsyncTask<MyActivityStateParams, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(MyActivityStateParams... params){
            boolean success;
            String content;
            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                success = ac.postActivityState(params[0].actID, params[0].a, params[0].r,
                        params[0].stId, params[0].state);
                content = "";
            }
            catch (Exception ex){
                success = false;
                content = ex.getLocalizedMessage();
            }
            return new Pair<Boolean, String>(success, content);
        }

        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                Toast.makeText(getApplicationContext(), getString(R.string.write_as_error) + p.second,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    protected class MyActivityStateParams{
        Agent a;
        JsonObject state;
        String r;
        String actID;
        String stId;

        MyActivityStateParams(Agent a, JsonObject s, String r, String stID, String actID){
            this.a = a;
            this.state = s;
            this.r = r;
            this.actID = actID;
            this.stId = stID;
        }
    }
    protected class MyReturnActivityStateData{
        boolean success;
        JsonObject state;

        MyReturnActivityStateData(boolean s, JsonObject state){
            this.success = s;
            this.state = state;
        }
    }
    private class ModuleData{
        String path;
        String name;
        String desc;
        Class module_class;

        ModuleData(){}
        public void setPath(String path) {
            this.path = path;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setDesc(String desc) {
            this.desc = desc;
        }
        public void setModule_class(int module_class) {
            switch(module_class){
                case 0:
                    this.module_class = RoseActivity.class;
                    break;
                case 1:
                    this.module_class = PruningActivity.class;
                    break;
                case 2:
                    this.module_class = DeadHeadingActivity.class;
                    break;
                case 3:
                    this.module_class = ShearingActivity.class;
                    break;
                case 4:
                    this.module_class = HybridsActivity.class;
                    break;
                case 5:
                    this.module_class = FloristryActivity.class;
                    break;
                case 6:
                    this.module_class = SymbolismActivity.class;
                    break;
            }
        }
    }
}
