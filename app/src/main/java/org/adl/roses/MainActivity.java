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
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.StatementResult;
import gov.adlnet.xapi.model.Verb;
import gov.adlnet.xapi.model.Verbs;

public class MainActivity extends ActionBarActivity {
    private String _actor_name;
    private String _actor_email;

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
        String[] modules = getResources().getStringArray(R.array.modules_name);
        ArrayList<String> moduleList = new ArrayList<String>();
        moduleList.addAll(Arrays.asList(modules));
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, moduleList);
        mainListView.setAdapter(listAdapter);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id){
                sendStatements(position, null, 0, false);
            }
        });

        // Try getting bookmark from activity states
        getBookmarkFromActivityState();
    }

    private void getBookmarkFromActivityState() {
        checkActor();
        Agent actor = new Agent(_actor_name, _actor_email);
        // Get activity state for app IRI as activityID and SCORM activity state IRI as stateID
        // If there are attempts, they will be listed in an Attempts array
        MyActivityStateParams get_as_params = new MyActivityStateParams(actor, null, null, getString(R.string.scorm_profile_activity_state_id),
                getString(R.string.app_activity_iri));
        GetActivityStateTask get_sus_as_task = new GetActivityStateTask();
        MyReturnActivityStateData sus_result = null;
        try{
            sus_result = get_sus_as_task.execute(get_as_params).get();
        }
        catch (Exception ex){
            // Will get thrown in GetActivityStateTask
        }

        JsonObject state = sus_result.state;
        String bookmarkID = "";
        // No state could exist on first run
        if (state != null){
            try{
                // Get the attempts element from the state which will be an array itself
                JsonArray attempts = state.get("Attempts").getAsJsonArray();
                if (attempts.size() > 0){
                    // Get the last attempt
                    bookmarkID = attempts.get(attempts.size() - 1).getAsString();
                }
            }
            catch (Exception ex){
                Toast.makeText(getApplicationContext(), getString(R.string.retrieve_attempts_error) + ex.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        // If there was an existing attempt IRI
        if (!bookmarkID.isEmpty()){
            // Try to retrieve a terminated statement with that bookmarkID
            MyStatementParams term_stmt_params = new MyStatementParams(actor, Verbs.terminated(), bookmarkID);
            GetStatementsTask get_term_stmt_task = new GetStatementsTask();
            MyReturnStatementData return_term_stmt = null;

            try{
                return_term_stmt = get_term_stmt_task.execute(term_stmt_params).get();
            }
            catch (Exception ex){
                // Will get thrown in GetStatementTask
            }
            StatementResult result = return_term_stmt.stmt_result;
            ArrayList<Statement> stmts = result.getStatements();

            // If it's empty, that attempt was never terminated so retrieve the attempt
            if (stmts.isEmpty()){
                // Get activity state with attempt IRI as activityID and SCORM attempt
                // state IRI as stateID to get single activity attempt state
                MyActivityStateParams get_singular_as_params = new MyActivityStateParams(actor, null, null,
                        getString(R.string.scorm_profile_attempt_state_id), bookmarkID);
                GetActivityStateTask get_singular_as_task = new GetActivityStateTask();
                MyReturnActivityStateData singular_sus_result = null;
                try{
                    singular_sus_result = get_singular_as_task.execute(get_singular_as_params).get();
                }
                catch (Exception ex){
                    // Will get thrown in GetActivityStateTask
                    // If it's empty then
                }finally{
                    // Get bookmark data from the singular attempt state
                    JsonObject sing_state = singular_sus_result.state;
                    // If singular state has location information that means it has been suspended.
                    // Else it wasn't suspended and most likely terminated
                    if (sing_state != null){
                        try{
                            String[] bookmark = sing_state.get("location").getAsString().split(" ");
                            int bookmark_module = Integer.parseInt(bookmark[0]);
                            int bookmark_slide = Integer.parseInt(bookmark[1]);
                            String attempt_id = bookmarkID.split("=")[1];
                            launchBookmarkDialog(bookmark_module, bookmark_slide, attempt_id, actor);
                        }
                        catch (Exception ex){
                            Toast.makeText(getApplicationContext(), getString(R.string.retrieve_as_attempt_error) + ex.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    }
    private void launchBookmarkDialog(final int moduleId, final int slide, final String attemptId, final Agent actor){
        String module_name = getResources().getStringArray(R.array.modules_name)[moduleId];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title))
                .setMessage(module_name + " Slide - " + (slide + 1));

        builder.setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
                sendResumeStatements(moduleId, attemptId, slide, actor);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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

    private void sendResumeStatements(int moduleId, String attemptId, int slide, Agent actor){
        ModuleData md = setModuleData(moduleId, true);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;
        Class module_class = md.module_class;

        Activity stmt_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc, getString(R.string.scorm_profile_activity_type_lesson_id));
        Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId,
                "Attempt for " + name, "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));
        Context con = createContext(attempt_act);
        MyStatementParams resume_params = new MyStatementParams(actor, Verbs.resumed(), stmt_act, con);
        WriteStatementTask resume_stmt_task = new WriteStatementTask();
        resume_stmt_task.execute(resume_params);

        Intent resumeActivity = new Intent(MainActivity.this, module_class);
        resumeActivity.putExtra(getString(R.string.intent_slide), slide);
        resumeActivity.putExtra(getString(R.string.intent_actor_name), _actor_name);
        resumeActivity.putExtra(getString(R.string.intent_actor_email), _actor_email);
        resumeActivity.putExtra(getString(R.string.intent_attempt), attemptId);
        startActivityForResult(resumeActivity, moduleId);
    }
    private void sendSuspendedStatements(int moduleId, String attemptId, int slide){
        checkActor();
        Agent actor = new Agent(_actor_name, _actor_email);
        ModuleData md = setModuleData(moduleId, false);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;

        Activity sus_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc, getString(R.string.scorm_profile_activity_type_lesson_id));
        Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId,
                "Attempt for " + name, "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));
        Context con = createContext(attempt_act);
        MyStatementParams sus_params = new MyStatementParams(actor, Verbs.suspended(), sus_act, con);
        WriteStatementTask sus_stmt_task = new WriteStatementTask();
        sus_stmt_task.execute(sus_params);

        updateActivityState(attempt_act, actor, moduleId, slide);
    }
    private void sendStatements(int moduleId, String attemptId, int slide, boolean isResult){
        checkActor();
        Agent actor = new Agent(_actor_name, _actor_email);
        ModuleData md = setModuleData(moduleId, true);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;
        Class module_class = md.module_class;

        if (!isResult){
            Intent mod_intent = new Intent(MainActivity.this, module_class);
            mod_intent.putExtra(getString(R.string.intent_slide), slide);
            mod_intent.putExtra(getString(R.string.intent_actor_name), _actor_name);
            mod_intent.putExtra(getString(R.string.intent_actor_email), _actor_email);
            startActivityForResult(mod_intent, moduleId);
        }
        else{
            // This is called when returning from a rose module - need to keep same attemptId
            Activity mod_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc, getString(R.string.scorm_profile_activity_type_lesson_id));
            Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId,
                    "Attempt for " + name, "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));
            Context con = createContext(attempt_act);
            // returned result from launched activity, send terminated
            MyStatementParams terminate_params = new MyStatementParams(actor, Verbs.terminated(), mod_act, con);
            WriteStatementTask terminate_stmt_task = new WriteStatementTask();
            terminate_stmt_task.execute(terminate_params);
        }
    }

    private void updateActivityState(Activity sus_act, Agent actor, int moduleId, int slide){
        // Write attempt state
        JsonObject attempt_state = new JsonObject();
        attempt_state.addProperty("location", String.format("%s %s", moduleId, slide));
        // Write attempt state with attemptID as registration, SCORM attempt state IRI
        // as stateID and the suspended activity's IRI as the activityId
        MyActivityStateParams as_sus_params = new MyActivityStateParams(actor, attempt_state, null, getString(R.string.scorm_profile_attempt_state_id),
                sus_act.getId());
        WriteActivityStateTask sus_as_task = new WriteActivityStateTask();
        sus_as_task.execute(as_sus_params);
    }

    private void checkActor(){
        if ((_actor_name == null || _actor_name.equals("")) || (_actor_email == null || _actor_email.equals(""))){
            _actor_name = getString(R.string.default_name);
            _actor_email = getString(R.string.default_email);
        }
    }
    private Context createContext(Activity attempt_act){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.app_activity_name), getString(R.string.app_activity_description),
                getString(R.string.scorm_profile_activity_type_course_id)));
        con_act_list.add(attempt_act);

        con_acts.setGrouping(con_act_list);
        ArrayList<Activity> cat_act_list = new ArrayList<Activity>();
        cat_act_list.add(new Activity(getString(R.string.scorm_profile_activity_category_id)));
        con_acts.setCategory(cat_act_list);
        con.setContextActivities(con_acts);
        return con;
    }
    private Activity createActivity(String act_id, String name, String desc, String type_id){
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
        intent.putExtra(getString(R.string.intent_request_code), requestCode);
        super.startActivityForResult(intent, requestCode);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Bundle extras = data.getExtras();
        String attemptId = "";
        int slide = 0;
        if (extras != null){
            attemptId = extras.getString(getString(R.string.intent_attempt), "");
            slide = extras.getInt(getString(R.string.intent_slide), 0);
        }
        if(resultCode == RESULT_OK) {
            sendStatements(requestCode, attemptId, slide, true);
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
            // All emails get 'mailto:' stored in front of them
            ed_email.setText(tmpEmail.substring(7));
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
                        _actor_email = "mailto:" + ed2.getText().toString();

                        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.preferences_key), MODE_PRIVATE).edit();
                        editor.putString(getString(R.string.preferences_name_key), _actor_name);
                        editor.putString(getString(R.string.preferences_email_key), _actor_email);
                        editor.commit();
                        // Try getting bookmark from activity states
                        getBookmarkFromActivityState();
                    }
                });

        // build and show
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected class WriteStatementTask extends AsyncTask<MyStatementParams, Void, Pair<Boolean, String>>{
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
    protected class GetStatementsTask extends AsyncTask<MyStatementParams, Void, MyReturnStatementData> {
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

        protected void onPostExecute(MyReturnStatementData sd){
            if (!sd.success){
                Toast.makeText(getApplicationContext(), getString(R.string.statement_get_error) + sd.result,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    protected class MyStatementParams {
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
    protected class MyReturnStatementData{
        boolean success;
        String result;
        StatementResult stmt_result;

        MyReturnStatementData(boolean s, String r, StatementResult sr){
            this.success = s;
            this.result = r;
            this.stmt_result = sr;
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
    protected class ModuleData{
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
                    this.module_class = StylesActivity.class;
                    break;
                case 6:
                    this.module_class = SymbolismActivity.class;
                    break;
            }
        }
    }
}