package org.adl.roses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import gov.adlnet.xapi.model.ActivityState;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.Context;
import gov.adlnet.xapi.model.ContextActivities;
import gov.adlnet.xapi.model.Result;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.StatementResult;
import gov.adlnet.xapi.model.Verbs;

public class MainActivity extends android.app.Activity{
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
        ArrayList<String> moduleList = new ArrayList<>();
        moduleList.addAll(Arrays.asList(modules));
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, R.layout.simplerow, moduleList);
        mainListView.setAdapter(listAdapter);
        // Set the onclick listener to launch the module and send statements
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
        // Make sure to set actor name and email if not already before checking activity state
        checkActor();
        Agent actor = new Agent(_actor_name, _actor_email);

        // Get activity state for app IRI as activityID and SCORM activity state IRI as stateID
        // If there are attempts, they will be listed in an attempts array
        MyActivityStateParams get_as_params = new MyActivityStateParams(actor, null, getString(R.string.scorm_profile_activity_state_id),
                getString(R.string.app_activity_iri));
        GetActivityStateTask get_sus_as_task = new GetActivityStateTask();
        MyReturnActivityStateData sus_result = null;
        try{
            sus_result = get_sus_as_task.execute(get_as_params).get();
        }
        catch (Exception ex){
            // Will get thrown in GetActivityStateTask
        }

        // Make sure you can retrieve the state
        JsonObject state;
        if (sus_result != null){
            state = sus_result.state;
        }
        else{
            state = null;
        }
        String bookmarkID = "";
        // No state could exist on first run
        if (state != null){
            try{
                // Get the attempts element from the state which will be an array itself
                JsonArray attempts = state.get("attempts").getAsJsonArray();
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
            GetStatementsTask get_term_stmt_task = new GetStatementsTask();
            MyReturnStatementData return_term_stmt = null;

            try{
                return_term_stmt = get_term_stmt_task.execute(new Statement(actor, Verbs.terminated(),
                        new Activity(bookmarkID))).get();
            }
            catch (Exception ex){
                // Will get thrown in GetStatementTask
            }

            // Make sure there was a result and set statements
            StatementResult result;
            ArrayList<Statement> stmts;
            if (return_term_stmt != null){
                result = return_term_stmt.stmt_result;
                stmts = result.getStatements();
            }
            else{
                stmts = null;
            }

            // If it's empty, that attempt was never terminated so retrieve the attempt
            if (stmts != null && stmts.isEmpty()){
                // Get activity state with attempt IRI as activityID and SCORM attempt
                // state IRI as stateID to get single activity attempt state
                MyActivityStateParams get_singular_as_params = new MyActivityStateParams(actor, null,
                        getString(R.string.scorm_profile_attempt_state_id), bookmarkID);
                GetActivityStateTask get_singular_as_task = new GetActivityStateTask();
                MyReturnActivityStateData singular_sus_result = null;
                try{
                    singular_sus_result = get_singular_as_task.execute(get_singular_as_params).get();
                }
                catch (Exception ex){
                    // Will get thrown in GetActivityStateTask
                    // If it's empty then
                }
                finally{
                    // Make sure there was a result
                    JsonObject sing_state;
                    if (singular_sus_result != null){
                        // Get bookmark data from the singular attempt state
                        sing_state = singular_sus_result.state;
                    }else{
                        sing_state = null;
                    }

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
        // Once bookmarked module and slide are retrieved from activity state
        // get the name associated with module and launch dialog to resume
        // from bookmark
        String module_name = getResources().getStringArray(R.array.modules_name)[moduleId];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title))
                .setMessage(module_name + " Slide - " + (slide + 1));

        builder.setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                sendResumeStatements(moduleId, attemptId, slide, actor);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
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
        // Get the module data from the moduleId
        ModuleData md = setModuleData(moduleId, true);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;
        Class module_class = md.module_class;

        // Create parent activity that will be object of the statement
        Activity stmt_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc, getString(R.string.scorm_profile_activity_type_lesson_id));
        // Create attempt activity
        Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId,
                "Attempt for " + name, "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));
        // Create context and resume statement to send
        Context con = createContext(attempt_act);
        WriteStatementTask resume_stmt_task = new WriteStatementTask();
        Statement stmt = new Statement(actor, Verbs.resumed(), stmt_act);
        stmt.setContext(con);
        resume_stmt_task.execute(stmt);

        // Create intent and add necessary additional infor for resuming
        Intent resumeActivity = new Intent(MainActivity.this, module_class);
        resumeActivity.putExtra(getString(R.string.intent_slide), slide);
        resumeActivity.putExtra(getString(R.string.intent_actor_name), _actor_name);
        resumeActivity.putExtra(getString(R.string.intent_actor_email), _actor_email);
        resumeActivity.putExtra(getString(R.string.intent_attempt), attemptId);
        startActivityForResult(resumeActivity, moduleId);
    }
    private void sendSuspendedStatements(int moduleId, String attemptId, int slide){
        // Make sure actor name and email are set
        checkActor();
        Agent actor = new Agent(_actor_name, _actor_email);

        // Get the module data from the moduleId
        ModuleData md = setModuleData(moduleId, false);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;

        Activity sus_act = createActivity(getString(R.string.app_activity_iri) + path, name, desc, getString(R.string.scorm_profile_activity_type_lesson_id));
        Activity attempt_act = createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + attemptId,
                "Attempt for " + name, "Attempt for " + desc, getString(R.string.scorm_profile_activity_type_attempt_id));
        Context con = createContext(attempt_act);
        WriteStatementTask sus_stmt_task = new WriteStatementTask();
        Statement stmt = new Statement(actor, Verbs.suspended(), sus_act);
        stmt.setContext(con);
        sus_stmt_task.execute(stmt);

        updateActivityState(attempt_act, actor, moduleId, slide);
    }
    private void sendStatements(int moduleId, String attemptId, int slide, boolean isResult){
        // Make sure to check actor email and name are set
        checkActor();
        Agent actor = new Agent(_actor_name, _actor_email);

        // Get the module data from the moduleId
        ModuleData md = setModuleData(moduleId, true);
        String path = md.path;
        String name = md.name;
        String desc = md.desc;
        Class module_class = md.module_class;

        // If this isn't a result from suspended then create new intent and launch activity
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
            Result result = new Result();
            result.setCompletion(true);
            result.setSuccess(true);
            // returned result from launched activity, send terminated
            WriteStatementTask terminate_stmt_task = new WriteStatementTask();
            Statement stmt = new Statement(actor, Verbs.terminated(), mod_act);
            stmt.setContext(con);
            stmt.setResult(result);
            terminate_stmt_task.execute(stmt);
        }
    }

    private void updateActivityState(Activity sus_act, Agent actor, int moduleId, int slide){
        // Write attempt state
        JsonObject attempt_state = new JsonObject();
        attempt_state.addProperty("location", String.format("%s %s", moduleId, slide));
        // Write attempt state with attemptID as registration, SCORM attempt state IRI
        // as stateID and the suspended activity's IRI as the activityId
        MyActivityStateParams as_sus_params = new MyActivityStateParams(actor, attempt_state, getString(R.string.scorm_profile_attempt_state_id),
                sus_act.getId());
        WriteActivityStateTask sus_as_task = new WriteActivityStateTask();
        sus_as_task.execute(as_sus_params);
    }

    private void checkActor(){
        // Just make sure actor name and email aren't null if user doesn't put anything
        if ((_actor_name == null || _actor_name.equals("")) || (_actor_email == null || _actor_email.equals(""))){
            _actor_name = getString(R.string.default_name);
            _actor_email = getString(R.string.default_email);
        }
    }
    private Context createContext(Activity attempt_act){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<>();
        // Include app activity
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.app_activity_name), getString(R.string.app_activity_description),
                getString(R.string.scorm_profile_activity_type_course_id)));
        con_act_list.add(attempt_act);

        con_acts.setGrouping(con_act_list);
        ArrayList<Activity> cat_act_list = new ArrayList<>();
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
            launchSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode){
        // Whenever activity is started, include the moduleId
        intent.putExtra(getString(R.string.intent_request_code), requestCode);
        super.startActivityForResult(intent, requestCode);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Whenever activity is done, get moduleId, slide, and attemptId
        Bundle extras = data.getExtras();
        String attemptId = "";
        int slide = 0;
        if (extras != null){
            attemptId = data.getStringExtra(getString(R.string.intent_attempt));
            // AttemptID will be set to null if not found (can't use getString with min API)
            if (attemptId == null){
                attemptId = "";
            }
            slide = extras.getInt(getString(R.string.intent_slide), 0);
        }
        // Depending if user suspended or not - send appropriate statement
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
        ViewGroup parent = (ViewGroup) findViewById(R.id.main_layout);
        final LayoutInflater inflater = this.getLayoutInflater();
        final View settings = inflater.inflate(R.layout.dialog_actor, parent, false);

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
                        editor.apply();
                        // Try getting bookmark from activity states
                        getBookmarkFromActivityState();
                    }
                });

        // build and show
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Inner class to send statements to the LRS - returns boolean success and string result
    private class WriteStatementTask extends AsyncTask<Statement, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(Statement... params){
            boolean success = true;
            String content;
            // Try to send the statement
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

        // Called after doInBackground for UI
        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                // Send toast error message
                Toast.makeText(getApplicationContext(), getString(R.string.statement_write_error) + p.second,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to get statements from the LRS - returns statement data
    private class GetStatementsTask extends AsyncTask<Statement, Void, MyReturnStatementData> {
        protected MyReturnStatementData doInBackground(Statement... params) {
            boolean success = true;
            StatementResult stmt_result;
            String result;
            // Try to get statement data back
            try{
                StatementClient sc = new StatementClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                stmt_result = sc.filterByActivity(params[0].getId()).filterByActor(params[0].getActor())
                    .filterByVerb(params[0].getVerb()).includeRelatedActivities(true).getStatements();
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
        // Ran after doInBackground for UI
        protected void onPostExecute(MyReturnStatementData sd){
            if (!sd.success){
                // Send toast with error message
                Toast.makeText(getApplicationContext(), getString(R.string.statement_get_error) + sd.result,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to return statement data back to activity from task
    protected class MyReturnStatementData{
        final boolean success;
        final String result;
        final StatementResult stmt_result;

        MyReturnStatementData(boolean s, String r, StatementResult sr){
            this.success = s;
            this.result = r;
            this.stmt_result = sr;
        }
    }
    // Inner class to get activity states from the LRS - returns activity state data
    private class GetActivityStateTask extends AsyncTask<MyActivityStateParams, Void, MyReturnActivityStateData>{
        protected MyReturnActivityStateData doInBackground(MyActivityStateParams... params){
            JsonObject state;
            boolean success = true;
            // Try to get activity state
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

        // Ran after doInBackground for UI
        protected void onPostExecute(MyReturnActivityStateData asd){
            if (!asd.success){
                // Send toast for error messages
                Toast.makeText(getApplicationContext(), getString(R.string.get_as_error), Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to write activity states to the LRS - returns boolean success and string state data
    private class WriteActivityStateTask extends AsyncTask<MyActivityStateParams, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(MyActivityStateParams... params){
            boolean success;
            String content;
            // Try to get activity state
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

        protected void onPostExecute(Pair<Boolean, String> p){
            if (!p.first){
                Toast.makeText(getApplicationContext(), getString(R.string.write_as_error) + p.second,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // Inner class to send activity state data to task
    private class MyActivityStateParams{
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
    // Inner class to return activity state data back to activity from task
    protected class MyReturnActivityStateData{
        final boolean success;
        final JsonObject state;

        MyReturnActivityStateData(boolean s, JsonObject state){
            this.success = s;
            this.state = state;
        }
    }
    // Inner class to hold module data for statements and activity states
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
                    this.module_class = StylesActivity.class;
                    break;
                case 6:
                    this.module_class = SymbolismActivity.class;
                    break;
            }
        }
    }
}