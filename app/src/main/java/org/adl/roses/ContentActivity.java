package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.JsonObject;

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

/**
 * Created by lou on 1/12/15.
 */
public abstract class ContentActivity extends ActionBarActivity{
    private int android_id;
    private int current_slide;
    private String attempt;

    protected int getAndroidId(){
        return this.android_id;
    }
    protected void setAndroidId(int a_id){
        this.android_id = a_id;
    }
    protected int getCurrentSlide(){
        return this.current_slide;
    }
    protected void setCurrentSlide(int s_id){
        this.current_slide = s_id;
    }
    protected String getCurrentAttempt(){return this.attempt;}
    protected void setCurrentAttempt(String att){this.attempt = att;}
    protected void generateAttempt(){this.attempt = UUID.randomUUID().toString();}

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
        String path = "";
        String name = "";
        String desc = "";

        switch(getAndroidId()){
            case 0:
                path = getString(R.string.mod_what_path);
                name = getString(R.string.mod_what_name);
                desc = getString(R.string.mod_what_description);
                break;
            case 1:
                path = getString(R.string.mod_pruning_path);
                name = getString(R.string.mod_pruning_name);
                desc = getString(R.string.mod_pruning_description);
                break;
            case 2:
                path = getString(R.string.mod_deadheading_path);
                name = getString(R.string.mod_deadheading_name);
                desc = getString(R.string.mod_deadheading_description);
                break;
            case 3:
                path = getString(R.string.mod_shearing_path);
                name = getString(R.string.mod_shearing_name);
                desc = getString(R.string.mod_shearing_description);
                break;
            case 4:
                path = getString(R.string.mod_hybrids_path);
                name = getString(R.string.mod_hybrids_name);
                desc = getString(R.string.mod_hybrids_description);
                break;
            case 5:
                path = getString(R.string.mod_styles_path);
                name = getString(R.string.mod_styles_name);
                desc = getString(R.string.mod_styles_description);
                break;
            case 6:
                path = getString(R.string.mod_symbolism_path);
                name = getString(R.string.mod_symbolism_name);
                desc = getString(R.string.mod_symbolism_description);
                break;
        }
        Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc,
                getString(R.string.scorm_profile_activity_type_lesson_id));
        Activity act = createActivity(getString(R.string.app_activity_iri) + path + "#" +
                        getCurrentSlide(), name + " - Slide " + (getCurrentSlide() + 1),
                        desc + " - Slide " + (getCurrentSlide() + 1), getString(R.string.scorm_profile_activity_type_lesson_id));
        Activity slide_act = createActivity(getString(R.string.app_activity_iri) + path + "#" +
                        getCurrentSlide() + "?attemptId=" + getCurrentAttempt(),
                         "Attempt for " + name + " - Slide " + (getCurrentSlide() + 1),
                         "Attempt for " + desc + " - Slide " + (getCurrentSlide() + 1), getString(R.string.scorm_profile_activity_type_attempt_id));
        Activity parent_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + getCurrentAttempt(),
                "Attempt for " + name, "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));

        Context slide_con = createContext(attempt_act, slide_act, parent_act, false);

        HashMap<String, String> verb_lang = new HashMap<String, String>();
        verb_lang.put("en-US", "read");
        Verb verb = new Verb("http://example.com/verbs/read", verb_lang);
        Agent actor = getActor();
        MyStatementParams slide_init_params = new MyStatementParams(actor, verb, act, slide_con);
        WriteStatementTask slide_init_stmt_task = new WriteStatementTask();
        slide_init_stmt_task.execute(slide_init_params);
    }

    protected Agent getActor(){
        return new Agent(getIntent().getExtras().getString("actorName"), getIntent().getExtras().getString("actorEmail"));
    }
    protected Context createContext(Activity attempt_act, Activity slide_act, Activity parent_act, boolean init){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.context_name_desc), getString(R.string.context_name_desc),
                getString(R.string.scorm_profile_activity_type_course_id)));
        con_act_list.add(attempt_act);

        if (!init){
            con_act_list.add(slide_act);
            ArrayList<Activity> parent_act_list = new ArrayList<Activity>();
            parent_act_list.add(parent_act);
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
        sendSlideChangeStatement();
        returnResult(false);
    }

    protected void returnResult(boolean suspended){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("attemptId", getCurrentAttempt());
        returnIntent.putExtra("slideId", getCurrentSlide());
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
                String msg = "Write Statement Error: ";
                Toast.makeText(getApplicationContext(), msg + p.second, Toast.LENGTH_LONG).show();
            }
        }
    }
    protected class MyStatementParams{
        Agent ag;
        Verb v;
        Activity a;
        Context c;

        MyStatementParams(Agent ag, Verb v, Activity a, Context c){
            this.ag = ag;
            this.v = v;
            this.a = a;
            this.c = c;
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
                Toast.makeText(getApplicationContext(), "No activity state returned (could not exist yet)", Toast.LENGTH_LONG).show();
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
                String msg = "Write State Error: ";
                Toast.makeText(getApplicationContext(), msg + p.second, Toast.LENGTH_LONG).show();
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
}
