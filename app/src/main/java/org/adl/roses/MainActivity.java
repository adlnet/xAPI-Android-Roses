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
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        // If the bookmarkID is not null and not empty then launch the bookmarked module
        // (Should only be null on first visit)
//        if (_bookmark_id != null && !_bookmark_id.trim().equals("")){
//            launchBookmarkedModule();
//        }
    }

//    private void getBookmark(SharedPreferences prefs){
//        // Try to get a bookmark to resume from the LRS
//        MyStatementParams get_stmt_params = new MyStatementParams(Verbs.initialized(), getString((R.string.app_activity_iri)));
//        GetStatementsTask get_stmt_task = new GetStatementsTask();
//        Pair<Boolean, StatementResult> res;
//        try{
//            res = get_stmt_task.execute(get_stmt_params).get();
//        }
//        catch (Exception ex)
//        {
//            res = new Pair<Boolean, StatementResult>(false, null);
//        }
//
//        // If the GET succeeded
//        if (res.first){
//            ArrayList<Statement> stmts = res.second.getStatements();
//            if (stmts.size() > 0){
//                // Since limiting to only one statement, safe to just get the first one
//                Statement statement = stmts.get(0);
//                // If the statement's verb id is suspended, we can use its object id as the bookmarkID - else look at last state
//                if (statement.getVerb().getId().equals(Verbs.suspended())){
//                    Activity act = (Activity)stmts.get(0).getObject();
//                    _bookmark = act.getId();
//                }
//                else{
//                    // Try getting from activity state
//                    try{
//                        GetActivityStateTask get_act_state_task = new GetActivityStateTask();
//                        _bookmark = get_act_state_task.execute().get();
//                    }
//                    catch (Exception ex){
//                        System.out.println(ex.getMessage());
//                    }
//                }
//            }
//            // No returned statements
//            else{
//                // Try getting from activity state
//                try{
//                    GetActivityStateTask get_act_state_task = new GetActivityStateTask();
//                    _bookmark = get_act_state_task.execute().get();
//                }
//                catch (Exception ex){
//                    System.out.println(ex.getMessage());
//                }
//            }
//        }
//        // If the get statements didn't succeed
//        else{
//            // Try getting from activity state
//            try{
//                GetActivityStateTask get_act_state_task = new GetActivityStateTask();
//                _bookmark = get_act_state_task.execute().get();
//            }
//            catch (Exception ex){
//                System.out.println(ex.getMessage());
//            }
//        }
//
//        // If both statements and states don't yield an ID - get from local storage
//        if (_bookmark == null){
//            _bookmark = prefs.getString(getString(R.string.preferences_bookmark_key), null);
//        }
//    }
//
//    private void launchBookmarkedModule(){
//        Class intentClass = null;
//        int moduleId = -1;
//        if (_bookmark_id.contains(getString(R.string.mod_what_path))){
//            intentClass = RoseActivity.class;
//            moduleId = _result_what;
//        }
//        else if(_bookmark_id.contains(getString(R.string.mod_pruning_path))){
//            intentClass = PruningActivity.class;
//            moduleId = _result_what;
//        }
//        else if(_bookmark_id.contains(getString(R.string.mod_deadheading_path))){
//            intentClass = DeadHeadingActivity.class;
//            moduleId = _result_what;
//        }
//        else if(_bookmark_id.contains(getString(R.string.mod_shearing_path))){
//            intentClass = ShearingActivity.class;
//            moduleId = _result_what;
//        }
//        else if (_bookmark_id.contains(getString(R.string.mod_hybrids_path))){
//            intentClass = HybridsActivity.class;
//            moduleId = _result_what;
//        }
//        else if (_bookmark_id.contains(getString(R.string.mod_styles_path))){
//            intentClass = FloristryActivity.class;
//            moduleId = _result_what;
//        }
//        else if(_bookmark_id.contains(getString(R.string.mod_symbolism_path))){
//            intentClass = SymbolismActivity.class;
//            moduleId = _result_what;
//        }
//        if (intentClass != null){
//            Intent bookmarkedActivity = new Intent(MainActivity.this, intentClass);
//            startActivityForResult(bookmarkedActivity, moduleId);
//        }
//    }
//
    private void sendSuspendedStatements(int moduleId, String attemptId, int slide){
        checkActor();
        Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
        switch(moduleId)
        {
            case _result_what:
                final Activity what_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_what_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_what_name), getString(R.string.mod_what_description));
                Context what_con = createContext();
                Result what_result = new Result();
                what_result.setResponse(Integer.toString(slide));

                MyStatementParams what_sus_params = new MyStatementParams(actor, Verbs.suspended(), what_act, what_con, what_result);
                WriteStatementTask what_sus_stmt_task = new WriteStatementTask();
                what_sus_stmt_task.execute(what_sus_params);
                break;
            case _result_pruning:
                final Activity prun_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_pruning_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));
                Context prun_con = createContext();
                Result prun_result = new Result();
                prun_result.setResponse(Integer.toString(slide));

                MyStatementParams prun_sus_params = new MyStatementParams(actor, Verbs.suspended(), prun_act, prun_con, prun_result);
                WriteStatementTask prun_sus_stmt_task = new WriteStatementTask();
                prun_sus_stmt_task.execute(prun_sus_params);
                break;
            case _result_deadheading:
                final Activity dh_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_deadheading_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));
                Context dh_con = createContext();
                Result dh_result = new Result();
                dh_result.setResponse(Integer.toString(slide));

                MyStatementParams dh_sus_params = new MyStatementParams(actor, Verbs.suspended(), dh_act, dh_con, dh_result);
                WriteStatementTask dh_sus_stmt_task = new WriteStatementTask();
                dh_sus_stmt_task.execute(dh_sus_params);
                break;
            case _result_shearing:
                final Activity shear_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_shearing_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));
                Context shear_con = createContext();
                Result shear_result = new Result();
                shear_result.setResponse(Integer.toString(slide));

                MyStatementParams shear_sus_params = new MyStatementParams(actor, Verbs.suspended(), shear_act, shear_con, shear_result);
                WriteStatementTask shear_sus_stmt_task = new WriteStatementTask();
                shear_sus_stmt_task.execute(shear_sus_params);
                break;
            case _result_hybrids:
                final Activity hybrid_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_hybrids_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));
                Context hybrid_con = createContext();
                Result hybrid_result = new Result();
                hybrid_result.setResponse(Integer.toString(slide));

                MyStatementParams hybrid_sus_params = new MyStatementParams(actor, Verbs.suspended(), hybrid_act, hybrid_con, hybrid_result);
                WriteStatementTask hybrid_sus_stmt_task = new WriteStatementTask();
                hybrid_sus_stmt_task.execute(hybrid_sus_params);
                break;
            case _result_styles:
                final Activity style_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_styles_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));
                Context style_con = createContext();
                Result style_result = new Result();
                style_result.setResponse(Integer.toString(slide));

                MyStatementParams style_sus_params = new MyStatementParams(actor, Verbs.suspended(), style_act, style_con, style_result);
                WriteStatementTask style_sus_stmt_task = new WriteStatementTask();
                style_sus_stmt_task.execute(style_sus_params);
                break;
            case _result_symbolism:
                final Activity sym_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_symbolism_path) +
                        "?attemptId=" + attemptId, getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));
                Context sym_con = createContext();
                Result sym_result = new Result();
                sym_result.setResponse(Integer.toString(slide));

                MyStatementParams sym_sus_params = new MyStatementParams(actor, Verbs.suspended(), sym_act, sym_con, sym_result);
                WriteStatementTask sym_sus_stmt_task = new WriteStatementTask();
                sym_sus_stmt_task.execute(sym_sus_params);
                break;
        }
    }
    private void sendStatements(int moduleId, boolean isResult, String mod_attempt_id, int slide){
        checkActor();
        Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
        switch(moduleId)
        {
            case _result_what:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent roseActivity = new Intent(MainActivity.this, RoseActivity.class);
                    roseActivity.putExtra("slideId", slide);
                    roseActivity.putExtra("actorName", _actor_name);
                    roseActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(roseActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity what_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_what_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_what_name), getString(R.string.mod_what_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context what_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams what_terminate_params = new MyStatementParams(actor, Verbs.terminated(), what_act, what_con);

                    WriteStatementTask what_terminate_stmt_task = new WriteStatementTask();
                    what_terminate_stmt_task.execute(what_terminate_params);
                }
                break;
            case _result_pruning:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent pruningActivity = new Intent(MainActivity.this, PruningActivity.class);
                    pruningActivity.putExtra("slideId", slide);
                    pruningActivity.putExtra("actorName", _actor_name);
                    pruningActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(pruningActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity prun_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_pruning_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context prun_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams prun_terminate_params = new MyStatementParams(actor, Verbs.terminated(), prun_act, prun_con);

                    WriteStatementTask prun_terminate_stmt_task = new WriteStatementTask();
                    prun_terminate_stmt_task.execute(prun_terminate_params);
                }
                break;
            case _result_deadheading:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent dhActivity = new Intent(MainActivity.this, DeadHeadingActivity.class);
                    dhActivity.putExtra("slideId", slide);
                    dhActivity.putExtra("actorName", _actor_name);
                    dhActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(dhActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity dh_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_deadheading_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context dh_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams dh_terminate_params = new MyStatementParams(actor, Verbs.terminated(), dh_act, dh_con);

                    WriteStatementTask dh_terminate_stmt_task = new WriteStatementTask();
                    dh_terminate_stmt_task.execute(dh_terminate_params);
                }
                break;
            case _result_shearing:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent shearActivity = new Intent(MainActivity.this, ShearingActivity.class);
                    shearActivity.putExtra("slideId", slide);
                    shearActivity.putExtra("actorName", _actor_name);
                    shearActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(shearActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity shear_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_shearing_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context shear_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams shear_terminate_params = new MyStatementParams(actor, Verbs.terminated(), shear_act, shear_con);

                    WriteStatementTask shear_terminate_stmt_task = new WriteStatementTask();
                    shear_terminate_stmt_task.execute(shear_terminate_params);
                }
                break;
            case _result_hybrids:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent hybridActivity = new Intent(MainActivity.this, HybridsActivity.class);
                    hybridActivity.putExtra("slideId", slide);
                    hybridActivity.putExtra("actorName", _actor_name);
                    hybridActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(hybridActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity hybrid_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_hybrids_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context hybrid_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams hybrid_terminate_params = new MyStatementParams(actor, Verbs.terminated(), hybrid_act, hybrid_con);

                    WriteStatementTask hybrid_terminate_stmt_task = new WriteStatementTask();
                    hybrid_terminate_stmt_task.execute(hybrid_terminate_params);
                }
                break;
            case _result_styles:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent stylesActivity = new Intent(MainActivity.this, FloristryActivity.class);
                    stylesActivity.putExtra("slideId", slide);
                    stylesActivity.putExtra("actorName", _actor_name);
                    stylesActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(stylesActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity styles_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_styles_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context styles_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams styles_terminate_params = new MyStatementParams(actor, Verbs.terminated(), styles_act, styles_con);

                    WriteStatementTask styles_terminate_stmt_task = new WriteStatementTask();
                    styles_terminate_stmt_task.execute(styles_terminate_params);
                }
                break;
            case _result_symbolism:
                // Launching the activity from main screen list
                if(! isResult){
                    Intent symActivity = new Intent(MainActivity.this, SymbolismActivity.class);
                    symActivity.putExtra("slideId", slide);
                    symActivity.putExtra("actorName", _actor_name);
                    symActivity.putExtra("actorEmail", "mailto:" + _actor_email);
                    startActivityForResult(symActivity, moduleId);
                }
                // Returning to main screen from activity
                else{
                    final Activity sym_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_symbolism_path)
                            + "?attemptId=" + mod_attempt_id, getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));

                    // This is called when returning from a rose module - need to keep same attemptId
                    Context sym_con = createContext();
                    // returned result from launched activity, send terminated
                    MyStatementParams sym_terminate_params = new MyStatementParams(actor, Verbs.terminated(), sym_act, sym_con);

                    WriteStatementTask sym_terminate_stmt_task = new WriteStatementTask();
                    sym_terminate_stmt_task.execute(sym_terminate_params);
                }
                break;
        }
    }

    private Context createContext(){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.context_name_desc), getString(R.string.context_name_desc)));

        con_acts.setParent(con_act_list);
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
    private class GetStatementsTask extends AsyncTask<MyStatementParams, Void, Pair<Boolean, StatementResult>> {
        protected Pair<Boolean, StatementResult> doInBackground(MyStatementParams... params) {
            // Try getting statement first and using the object ID of the suspended statement
            checkActor();
            Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);

            boolean success = true;
            StatementResult result;
            try{
                StatementClient sc = new StatementClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                result = sc.filterByActivity(getString(R.string.app_activity_iri)).filterByActor(actor)
                    .filterByVerb(Verbs.suspended()).getStatements();
            }
            catch (Exception ex){
                success = false;
                result = null;
            }
            return new Pair<Boolean, StatementResult>(success, result);
        }
    }
    private static class MyStatementParams{
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