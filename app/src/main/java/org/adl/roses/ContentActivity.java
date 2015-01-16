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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

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

    public int getAndroidId(){
        return this.android_id;
    }
    public void setAndroidId(int a_id){
        this.android_id = a_id;
    }
    public int getCurrentSlide(){
        return this.current_slide;
    }
    public void setCurrentSlide(int s_id){
        this.current_slide = s_id;
    }
    public String getCurrentAttempt(){return this.attempt;}
    public void setCurrentAttempt(String att){this.attempt = att;}
    public void generateAttempt(){this.attempt = UUID.randomUUID().toString();}

    public void previousSlide(){
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
    public void nextSlide(){
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
    public void replaceFragment(){
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SlideFragment frag = new SlideFragment();
        fragmentTransaction.replace(R.id.textFrag, frag).commit();
    }

    private void sendSlideChangeStatement(){
        Activity what_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_what_path) + "#" +
                        getCurrentSlide() + "?attemptId=" + getCurrentAttempt(),
                getString(R.string.mod_what_name), getString(R.string.mod_what_description));
        Context what_con = createContext(getString(R.string.mod_what_path), getCurrentAttempt(),
                getString(R.string.mod_what_name), getString(R.string.mod_what_description));


        HashMap<String, String> verb_lang = new HashMap<String, String>();
        verb_lang.put("en-US", "read");
        Verb verb = new Verb("http://example.com/verbs/read", verb_lang);
        Agent actor = getActor();
        MyStatementParams what_init_params = new MyStatementParams(actor, verb, what_act, what_con);
        WriteStatementTask what_init_stmt_task = new WriteStatementTask();
        what_init_stmt_task.execute(what_init_params);
    }

    public Agent getActor(){
        return new Agent(getIntent().getExtras().getString("actorName"), "mailto:" + getIntent().getExtras().getString("actorEmail"));
    }

    public Context createContext(String path, String mod_attempt_id, String name, String desc){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.context_name_desc), getString(R.string.context_name_desc)));

        con_act_list.add(createActivity(getString(R.string.app_activity_iri) + path, name, desc));
        con_acts.setParent(con_act_list);

//        ArrayList<Activity> group_act_list = new ArrayList<Activity>();
//        group_act_list.add(createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + mod_attempt_id, name, desc));
//        con_acts.setGrouping(group_act_list);
        con.setContextActivities(con_acts);

        return con;
    }

    public Activity createActivity(String act_id, String name, String desc){
        Activity act = new Activity(act_id);
        ActivityDefinition act_def = new ActivityDefinition();
        act_def.setName(new HashMap<String, String>());
        act_def.getName().put("en-US", name);
        act_def.setDescription(new HashMap<String, String>());
        act_def.getDescription().put("en-US", desc);
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
        returnResult(false);
    }

    public void returnResult(boolean suspended){
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

    public class WriteStatementTask extends AsyncTask<MyStatementParams, Void, Pair<Boolean, String>> {
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
    public static class MyStatementParams{
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
}
