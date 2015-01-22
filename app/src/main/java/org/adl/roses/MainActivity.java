package org.adl.roses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import gov.adlnet.xapi.client.ActivityClient;
import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.ActivityDefinition;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.Context;
import gov.adlnet.xapi.model.ContextActivities;
import gov.adlnet.xapi.model.Result;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.StatementResult;
import gov.adlnet.xapi.model.Verb;
import gov.adlnet.xapi.model.Verbs;

public class MainActivity extends ActionBarActivity {
    private String _actor_name;
    private String _actor_email;

    // result codes (based off of order of modules in arrays.xml)
    private final int _result_what = 0;
    private final int _result_pruning = 1;
    private final int _result_deadheading = 2;
    private final int _result_shearing = 3;
    private final int _result_hybrids = 4;
    private final int _result_styles = 5;
    private final int _result_symbolism = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set basic info
        setTitle(R.string.app_title);
        setContentView(R.layout.activity_main);
        // get attempt info and any store credentials, if none, pop settings
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preferences_key), MODE_PRIVATE);
        String tmpName = prefs.getString(getString(R.string.preferences_name_key), null);
        String tmpEmail = prefs.getString(getString(R.string.preferences_email_key), null);
        if(tmpName == null || tmpEmail == null)
        {
            launchSettings();
        }
        else
        {
            _actor_email = tmpEmail;
            _actor_name = tmpName;
        }

        // setup the list of content options
        ListView mainListView = (ListView)findViewById(R.id.list);
        // get the array resource
        String[] modules = getResources().getStringArray(R.array.modules);
        ArrayList<String> moduleList = new ArrayList<String>();
        moduleList.addAll(Arrays.asList(modules));
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, moduleList);
        mainListView.setAdapter(listAdapter);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id){
                sendStatements(position, false, null, 0);
            }
        });

        getBookmarkFromStatements();
        getBookmarkFromLocalStorage();
    }

    private void getBookmarkFromLocalStorage(){
        // Look for bookmark data in local storage
        checkActor();
        Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
        
    }
    private void getBookmarkFromStatements(){
        // Look for suspended statement for bookmark
        checkActor();
        Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
        MyStatementParams get_stmt_params = new MyStatementParams(actor, Verbs.suspended(), getString(R.string.app_activity_iri));
        GetStatementsTask get_sus_stmt_task = new GetStatementsTask();

        MyReturnStatementData sus_result = null;
        try{
            sus_result = get_sus_stmt_task.execute(get_stmt_params).get();
        }
        catch (Exception ex){
            // Will get thrown in GetStatementTask
        }

        ArrayList<Statement> stmts = sus_result.stmt_result.getStatements();
        if (stmts.size() > 0){
            Statement stmt = stmts.get(0);
            if (stmt.getVerb().getId().equals(Verbs.suspended().getId())){
                try{
                    JsonObject exts = stmt.getResult().getExtensions();
                    int bookmark_module = exts.get(getString(R.string.app_activity_iri) + getString(R.string.result_ext_module_path)).getAsInt();
                    int bookmark_slide = exts.get(getString(R.string.app_activity_iri) + getString(R.string.result_ext_slide_path)).getAsInt();
                    String attempt_id = stmt.getContext().getRegistration();
                    sendResumeStatements(bookmark_module, attempt_id, bookmark_slide, actor);
                }
                catch (Exception ex){
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void sendResumeStatements(int moduleId, String attemptId, int slide, Agent actor){
        String path = "";
        String name = "";
        String desc = "";
        Class module_class = null;
        switch(moduleId)
        {
            case _result_what:
                path = getString(R.string.mod_what_path);
                name = getString(R.string.mod_what_name);
                desc = getString(R.string.mod_what_description);
                module_class = RoseActivity.class;
                break;
            case _result_pruning:
                path = getString(R.string.mod_pruning_path);
                name = getString(R.string.mod_pruning_name);
                desc = getString(R.string.mod_pruning_description);
                module_class = PruningActivity.class;
                break;
            case _result_deadheading:
                path = getString(R.string.mod_deadheading_path);
                name = getString(R.string.mod_deadheading_name);
                desc = getString(R.string.mod_deadheading_description);
                module_class = DeadHeadingActivity.class;
                break;
            case _result_shearing:
                path = getString(R.string.mod_shearing_path);
                name = getString(R.string.mod_shearing_name);
                desc = getString(R.string.mod_shearing_description);
                module_class = ShearingActivity.class;
                break;
            case _result_hybrids:
                path = getString(R.string.mod_hybrids_path);
                name = getString(R.string.mod_hybrids_name);
                desc = getString(R.string.mod_hybrids_description);
                module_class = HybridsActivity.class;
                break;
            case _result_styles:
                path = getString(R.string.mod_styles_path);
                name = getString(R.string.mod_styles_name);
                desc = getString(R.string.mod_styles_description);
                module_class = FloristryActivity.class;
                break;
            case _result_symbolism:
                path = getString(R.string.mod_symbolism_path);
                name = getString(R.string.mod_symbolism_name);
                desc = getString(R.string.mod_symbolism_description);
                module_class = SymbolismActivity.class;
                break;
        }
        Activity stmt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId, name, desc);
        Context con = createContext(attemptId);

        MyStatementParams resume_params = new MyStatementParams(actor, Verbs.resumed(), stmt_act, con);
        WriteStatementTask resume_stmt_task = new WriteStatementTask();
        resume_stmt_task.execute(resume_params);
        Intent resumeActivity = new Intent(MainActivity.this, module_class);
        resumeActivity.putExtra("slideId", slide);
        resumeActivity.putExtra("actorName", _actor_name);
        resumeActivity.putExtra("actorEmail", "mailto:" + _actor_email);
        resumeActivity.putExtra("attemptId", attemptId);
        startActivityForResult(resumeActivity, moduleId);
    }
    private void sendSuspendedStatements(int moduleId, String attemptId, int slide){
        checkActor();
        Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
        String path = "";
        String name = "";
        String desc = "";
        switch(moduleId)
        {
            case _result_what:
                path = getString(R.string.mod_what_path);
                name = getString(R.string.mod_what_name);
                desc = getString(R.string.mod_what_description);
                break;
            case _result_pruning:
                path = getString(R.string.mod_pruning_path);
                name = getString(R.string.mod_pruning_name);
                desc = getString(R.string.mod_pruning_description);
                break;
            case _result_deadheading:
                path = getString(R.string.mod_deadheading_path);
                name = getString(R.string.mod_deadheading_name);
                desc = getString(R.string.mod_deadheading_description);
                break;
            case _result_shearing:
                path = getString(R.string.mod_shearing_path);
                name = getString(R.string.mod_shearing_name);
                desc = getString(R.string.mod_shearing_description);
                break;
            case _result_hybrids:
                path = getString(R.string.mod_hybrids_path);
                name = getString(R.string.mod_hybrids_name);
                desc = getString(R.string.mod_hybrids_description);
                break;
            case _result_styles:
                path = getString(R.string.mod_styles_path);
                name = getString(R.string.mod_styles_name);
                desc = getString(R.string.mod_styles_description);
                break;
            case _result_symbolism:
                path = getString(R.string.mod_symbolism_path);
                name = getString(R.string.mod_symbolism_name);
                desc = getString(R.string.mod_symbolism_description);
                break;
        }
        Activity sus_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId, name, desc);
        Context con = createContext(attemptId);
        Result result = new Result();
        JsonObject res_ext = new JsonObject();
        res_ext.addProperty(getString(R.string.app_activity_iri) + getString(R.string.result_ext_module_path), moduleId);
        res_ext.addProperty(getString(R.string.app_activity_iri) + getString(R.string.result_ext_slide_path), slide);
        result.setExtensions(res_ext);

        MyStatementParams sus_params = new MyStatementParams(actor, Verbs.suspended(), sus_act, con, result);
        WriteStatementTask sus_stmt_task = new WriteStatementTask();
        sus_stmt_task.execute(sus_params);
    }
    private void sendStatements(int moduleId, boolean isResult, String mod_attempt_id, int slide){
        checkActor();
        Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
        Class mod_class = null;
        String path = "";
        String name = "";
        String desc = "";
        switch(moduleId)
        {
            case _result_what:
                path = getString(R.string.mod_what_path);
                name = getString(R.string.mod_what_name);
                desc = getString(R.string.mod_what_description);
                mod_class = RoseActivity.class;
                break;
            case _result_pruning:
                path = getString(R.string.mod_pruning_path);
                name = getString(R.string.mod_pruning_name);
                desc = getString(R.string.mod_pruning_description);
                mod_class = PruningActivity.class;
                break;
            case _result_deadheading:
                path = getString(R.string.mod_deadheading_path);
                name = getString(R.string.mod_deadheading_name);
                desc = getString(R.string.mod_deadheading_description);
                mod_class = DeadHeadingActivity.class;
                break;
            case _result_shearing:
                path = getString(R.string.mod_shearing_path);
                name = getString(R.string.mod_shearing_name);
                desc = getString(R.string.mod_shearing_description);
                mod_class = ShearingActivity.class;
                break;
            case _result_hybrids:
                path = getString(R.string.mod_hybrids_path);
                name = getString(R.string.mod_hybrids_name);
                desc = getString(R.string.mod_hybrids_description);
                mod_class = HybridsActivity.class;
                break;
            case _result_styles:
                path = getString(R.string.mod_styles_path);
                name = getString(R.string.mod_styles_name);
                desc = getString(R.string.mod_styles_description);
                mod_class = FloristryActivity.class;
                break;
            case _result_symbolism:
                path = getString(R.string.mod_symbolism_path);
                name = getString(R.string.mod_symbolism_name);
                desc = getString(R.string.mod_symbolism_description);
                mod_class = SymbolismActivity.class;
                break;
        }
        if (!isResult){
            Intent mod_intent = new Intent(MainActivity.this, mod_class);
            mod_intent.putExtra("slideId", slide);
            mod_intent.putExtra("actorName", _actor_name);
            mod_intent.putExtra("actorEmail", "mailto:" + _actor_email);
            startActivityForResult(mod_intent, moduleId);
        }
        else{
            Activity mod_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + mod_attempt_id, name, desc);
            // This is called when returning from a rose module - need to keep same attemptId
            Context con = createContext(mod_attempt_id);
            // returned result from launched activity, send terminated
            MyStatementParams terminate_params = new MyStatementParams(actor, Verbs.terminated(), mod_act, con);
            WriteStatementTask terminate_stmt_task = new WriteStatementTask();
            terminate_stmt_task.execute(terminate_params);
        }
    }

    private Context createContext(String registration){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.context_name_desc), getString(R.string.context_name_desc)));

        con_acts.setParent(con_act_list);
        con.setRegistration(registration);
        con.setContextActivities(con_acts);
        return con;
    }
    private Activity createActivity(String act_id, String name, String desc){
        Activity act = new Activity(act_id);
        ActivityDefinition act_def = new ActivityDefinition();
        act_def.setName(new HashMap<String, String>());
        act_def.getName().put("en-US", name);
        act_def.setDescription(new HashMap<String, String>());
        act_def.getDescription().put("en-US", desc);
        act.setDefinition(act_def);
        return act;
    }
    private void checkActor(){
        if ((_actor_name == null || _actor_name.equals("")) || (_actor_email == null || _actor_email.equals(""))){
            _actor_name = "Anonymous";
            _actor_email = "anonymous@anon.com";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            //popUpDialog(true, false);
            launchSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void startActivityForResult(Intent intent, int requestCode){
        intent.putExtra("requestCode", requestCode);
        super.startActivityForResult(intent, requestCode);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Bundle extras = data.getExtras();
        String attemptId = "";
        int slide = 0;
        if (extras != null){
            attemptId = extras.getString("attemptId", "");
            slide = extras.getInt("slideId", 0);
        }
        if(resultCode == RESULT_OK) {
            sendStatements(requestCode, true, attemptId, slide);
        }
        else{
            sendSuspendedStatements(requestCode, attemptId, slide);
        }
    }
    private void launchSettings(){
        // build the view and inflate
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = this.getLayoutInflater();
        final View settings = inflater.inflate(R.layout.dialog_actor, null);

        // get any saved data
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preferences_key), MODE_PRIVATE);
        String tmpName = prefs.getString(getString(R.string.preferences_name_key), null);
        String tmpEmail = prefs.getString(getString(R.string.preferences_email_key), null);

        // populate saved data in form
        if (tmpName != null)
        {
            EditText ed_name = (EditText) settings.findViewById(R.id.name);
            ed_name.setText(tmpName);
        }
        if (tmpEmail != null)
        {
            EditText ed_email = (EditText) settings.findViewById(R.id.email);
            ed_email.setText(tmpEmail);
        }

        // set props and positive button
        builder.setTitle(getString(R.string.preferences_screen_title));
        builder.setView(settings)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Dialog d = (Dialog) dialog;
                        EditText ed1 = (EditText) d.findViewById(R.id.name);
                        _actor_name = ed1.getText().toString();

                        EditText ed2 = (EditText) d.findViewById(R.id.email);
                        _actor_email = ed2.getText().toString();

                        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.preferences_key), MODE_PRIVATE).edit();
                        editor.putString(getString(R.string.preferences_name_key), _actor_name);
                        editor.putString(getString(R.string.preferences_email_key), _actor_email);
                        editor.commit();
                    }
                });

        // build and show
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class WriteStatementTask extends AsyncTask<MyStatementParams, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(MyStatementParams... params){
            Statement stmt = new Statement();
            stmt.setActor(params[0].ag);
            stmt.setVerb(params[0].v);
            stmt.setObject(params[0].a);
            stmt.setContext(params[0].c);
            stmt.setResult(params[0].r);

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
    private class GetStatementsTask extends AsyncTask<MyStatementParams, Void, MyReturnStatementData> {
        protected MyReturnStatementData doInBackground(MyStatementParams... params) {
            // Try getting statement first and using the object ID of the suspended statement
            Agent actor = params[0].ag;
            Verb verb = params[0].v;
            String act = params[0].aID;
            boolean success = true;
            StatementResult stmt_result;
            String result;
            try{
                StatementClient sc = new StatementClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                stmt_result = sc.filterByActivity(act).filterByActor(actor)
                    .filterByVerb(verb).includeRelatedActivities(true).getStatements();
                Gson gson = new Gson();
                result = gson.toJson(stmt_result, StatementResult.class);
            }
            catch (Exception ex){
                success = false;
                stmt_result = null;
                result = ex.getLocalizedMessage();
            }
            return new MyReturnStatementData(success, result, stmt_result);
        }

        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                String msg = "Get Statement Error: ";
                Toast.makeText(getApplicationContext(), msg + p.second, Toast.LENGTH_LONG).show();
            }
        }
    }
    private class MyStatementParams {
        Agent ag;
        Verb v;
        Activity a;
        Context c;
        String aID;
        Result r;
        MyStatementParams(Agent ag, Verb v, Activity a, Context c, Result r){
            this.ag = ag;
            this.v = v;
            this.a = a;
            this.c = c;
            this.r = r;
        }
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
    private class MyReturnStatementData{
        boolean success;
        String result;
        StatementResult stmt_result;
        MyReturnStatementData(boolean s, String r, StatementResult sr){
            this.success = s;
            this.result = r;
            this.stmt_result = sr;
        }
    }
    private class GetActivityStateTask extends AsyncTask<Void, Void, String>{
        protected String doInBackground(Void... params){
            checkActor();
            Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
            String bookmarkID = "";
            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                // This will retrieve an array of states (should only be one in the array)
                JsonObject state = ac.getActivityState(getString(R.string.app_activity_iri), actor, null,
                        getString(R.string.scorm_profile_activity_state_id));
                // Get the attempts element from the state which will be an array itself
                JsonArray attempts = state.get("Attempts").getAsJsonArray();

                if (attempts.size() > 0){
                    // Get the last attempt
                    bookmarkID = attempts.get(attempts.size() - 1).getAsString();
                }
            }
            catch (Exception ex){
                System.out.println(ex.getMessage());
            }
            return bookmarkID;
        }
    }
    private class WriteActivityStateTask extends AsyncTask<JsonObject, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(JsonObject... params){
            checkActor();
            Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
            boolean success = true;
            String content;

            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                success = ac.postActivityState(getString(R.string.app_activity_iri), actor, null,
                        getString(R.string.scorm_profile_activity_state_id), params[0]);
                content = "success";
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
}