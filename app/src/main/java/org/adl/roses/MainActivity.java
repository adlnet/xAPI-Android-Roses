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
import java.util.UUID;

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
    private String _attempt_id;
    private String _bookmark_id;

    // result codes
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
        _attempt_id = UUID.randomUUID().toString();
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
                sendStatements(position, false, null);
            }
        });

        // If the bookmarkID is not null and not empty then launch the bookmarked module
        // (Should only be null on first visit)
        setBookmarkID(prefs);
        if (_bookmark_id != null && !_bookmark_id.trim().equals("")){
            launchBookmarkedModule();
        }
    }

    private void setBookmarkID(SharedPreferences prefs){
        // Try to get a bookmark to resume from the LRS
        MyStatementParams get_stmt_params = new MyStatementParams(Verbs.initialized(), getString((R.string.app_activity_iri)));
        GetStatementsTask get_stmt_task = new GetStatementsTask();
        Pair<Boolean, StatementResult> res;
        try{
            res = get_stmt_task.execute(get_stmt_params).get();
        }
        catch (Exception ex)
        {
            res = new Pair<Boolean, StatementResult>(false, null);
        }

        // If the GET succeeded
        if (res.first){
            ArrayList<Statement> stmts = res.second.getStatements();
            if (stmts.size() > 0){
                // Since limiting to only one statement, safe to just get the first one
                Statement statement = stmts.get(0);
                // If the statement's verb id is suspended, we can use its object id as the bookmarkID - else look at last state
                if (statement.getVerb().getId().equals(Verbs.suspended())){
                    Activity act = (Activity)stmts.get(0).getObject();
                    _bookmark_id = act.getId();
                }
                else{
                    // Try getting from activity state
                    try{
                        GetActivityStateTask get_act_state_task = new GetActivityStateTask();
                        _bookmark_id = get_act_state_task.execute().get();
                    }
                    catch (Exception ex){
                        System.out.println(ex.getMessage());
                    }
                }
            }
            // No returned statements
            else{
                // Try getting from activity state
                try{
                    GetActivityStateTask get_act_state_task = new GetActivityStateTask();
                    _bookmark_id = get_act_state_task.execute().get();
                }
                catch (Exception ex){
                    System.out.println(ex.getMessage());
                }
            }
        }
        // If the get statements didn't succeed
        else{
            // Try getting from activity state
            try{
                GetActivityStateTask get_act_state_task = new GetActivityStateTask();
                _bookmark_id = get_act_state_task.execute().get();
            }
            catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        }

        // If both statements and states don't yield an ID - get from local storage
        if (_bookmark_id == null || _bookmark_id.trim().equals("")){
            _bookmark_id = prefs.getString(getString(R.string.preferences_bookmark_key), null);
        }
    }

    private void launchBookmarkedModule(){
        Class intentClass = null;
        int moduleId = -1;
        if (_bookmark_id.contains(getString(R.string.mod_what_path))){
            intentClass = RoseActivity.class;
            moduleId = _result_what;
        }
        else if(_bookmark_id.contains(getString(R.string.mod_pruning_path))){
            intentClass = PruningActivity.class;
            moduleId = _result_what;
        }
        else if(_bookmark_id.contains(getString(R.string.mod_deadheading_path))){
            intentClass = DeadHeadingActivity.class;
            moduleId = _result_what;
        }
        else if(_bookmark_id.contains(getString(R.string.mod_shearing_path))){
            intentClass = ShearingActivity.class;
            moduleId = _result_what;
        }
        else if (_bookmark_id.contains(getString(R.string.mod_hybrids_path))){
            intentClass = HybridsActivity.class;
            moduleId = _result_what;
        }
        else if (_bookmark_id.contains(getString(R.string.mod_styles_path))){
            intentClass = FloristryActivity.class;
            moduleId = _result_what;
        }
        else if(_bookmark_id.contains(getString(R.string.mod_symbolism_path))){
            intentClass = SymbolismActivity.class;
            moduleId = _result_what;
        }
        if (intentClass != null){
            Intent bookmarkedActivity = new Intent(MainActivity.this, intentClass);
            startActivityForResult(bookmarkedActivity, moduleId);
        }
    }

    private void sendSuspendedStatements(int moduleId, String attemptId){
        Gson gson = new Gson();
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preferences_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        switch(moduleId)
        {
            case _result_what:
                final Activity what_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_what_path),
                        getString(R.string.mod_what_name), getString(R.string.mod_what_description));
                Context what_con = createContext(getString(R.string.mod_what_path), attemptId,
                        getString(R.string.mod_what_name), getString(R.string.mod_what_description));

                MyStatementParams what_sus_params = new MyStatementParams(Verbs.suspended(), what_act, what_con);
                WriteStatementTask what_sus_stmt_task = new WriteStatementTask();
                what_sus_stmt_task.execute(what_sus_params);

                MyActivityStateParams what_sus_state_params = new MyActivityStateParams(getString(R.string.mod_what_path),
                        attemptId);
                UpdateActivityStateTask what_update_state_task = new UpdateActivityStateTask();
                List<String> what_updated_attempt_list = new ArrayList<String>();
                JsonObject whatUpdatedState = new JsonObject();
                try {
                    what_updated_attempt_list = what_update_state_task.execute(what_sus_state_params).get();
                    // Create state with new attempts array
                    whatUpdatedState.addProperty("Attempts", gson.toJson(what_updated_attempt_list));
                }
                catch (Exception ex){
                    // Toast here
                }

                // If it was successful updating current attempt list - write the new state
                if (what_updated_attempt_list != null || what_updated_attempt_list.size() > 0){
                    WriteActivityStateTask what_write_state_task = new WriteActivityStateTask();
                    what_write_state_task.execute(whatUpdatedState);
                }
                // Either way - set ID locally
                JsonArray attempts = whatUpdatedState.get("Attempts").getAsJsonArray();
                editor.putString(getString(R.string.preferences_bookmark_key), attempts.get(attempts.size() - 1).getAsString());
                editor.commit();
                break;
            case _result_pruning:
                final Activity pruning_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_pruning_path),
                        getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));
                Context pruning_con = createContext(getString(R.string.mod_pruning_path), attemptId,
                        getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));

                MyStatementParams pruning_sus_params = new MyStatementParams(Verbs.suspended(), pruning_act, pruning_con);
                WriteStatementTask pruning_sus_stmt_task = new WriteStatementTask();
                pruning_sus_stmt_task.execute(pruning_sus_params);
                break;
            case _result_deadheading:
                final Activity dh_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_deadheading_path),
                        getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));
                Context dh_con = createContext(getString(R.string.mod_deadheading_path), attemptId,
                        getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));

                MyStatementParams dh_sus_params = new MyStatementParams(Verbs.suspended(), dh_act, dh_con);
                WriteStatementTask dh_sus_stmt_task = new WriteStatementTask();
                dh_sus_stmt_task.execute(dh_sus_params);
                break;
            case _result_shearing:
                final Activity shear_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_shearing_path),
                        getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));
                Context shear_con = createContext(getString(R.string.mod_shearing_path), attemptId,
                        getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));

                MyStatementParams shear_sus_params = new MyStatementParams(Verbs.suspended(), shear_act, shear_con);
                WriteStatementTask shear_sus_stmt_task = new WriteStatementTask();
                shear_sus_stmt_task.execute(shear_sus_params);
                break;
            case _result_hybrids:
                final Activity hybrid_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_hybrids_path),
                        getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));
                Context hybrid_con = createContext(getString(R.string.mod_hybrids_path), attemptId,
                        getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));

                MyStatementParams hybrid_sus_params = new MyStatementParams(Verbs.suspended(), hybrid_act, hybrid_con);
                WriteStatementTask hybrid_sus_stmt_task = new WriteStatementTask();
                hybrid_sus_stmt_task.execute(hybrid_sus_params);
                break;
            case _result_styles:
                final Activity florist_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_styles_path),
                        getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));
                Context florist_con = createContext(getString(R.string.mod_styles_path), attemptId,
                        getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));

                MyStatementParams florist_sus_params = new MyStatementParams(Verbs.suspended(), florist_act, florist_con);
                WriteStatementTask florist_sus_stmt_task = new WriteStatementTask();
                florist_sus_stmt_task.execute(florist_sus_params);
                break;
            case _result_symbolism:
                final Activity sym_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_symbolism_path),
                        getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));
                Context sym_con = createContext(getString(R.string.mod_symbolism_path), attemptId,
                        getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));

                MyStatementParams sym_sus_params = new MyStatementParams(Verbs.suspended(), sym_act, sym_con);
                WriteStatementTask sym_sus_stmt_task = new WriteStatementTask();
                sym_sus_stmt_task.execute(sym_sus_params);
                break;
        }
    }

    private void sendStatements(int moduleId, boolean isResult, String attemptId){
        switch(moduleId)
        {
            case _result_what:
                final Activity what_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_what_path),
                        getString(R.string.mod_what_name), getString(R.string.mod_what_description));

                if(! isResult){
                    Intent roseActivity = new Intent(MainActivity.this, RoseActivity.class);
                    // Initial launch so create attempt id
                    String what_attempt_id = UUID.randomUUID().toString();
                    Context what_con = createContext(getString(R.string.mod_what_path), what_attempt_id,
                            getString(R.string.mod_what_name), getString(R.string.mod_what_description));

                    // send initialize statements and launch activity
                    MyStatementParams what_init_params = new MyStatementParams(Verbs.initialized(), what_act, what_con);
                    WriteStatementTask what_init_stmt_task = new WriteStatementTask();
                    what_init_stmt_task.execute(what_init_params);

                    roseActivity.putExtra("sessionId", what_attempt_id);
                    startActivityForResult(roseActivity, _result_what);
                }
                else{
                    // This is called when returning from a rose module - need to keep same attemptId
                    Context what_con = createContext(getString(R.string.mod_what_path), attemptId,
                            getString(R.string.mod_what_name), getString(R.string.mod_what_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams what_ex_params = new MyStatementParams(Verbs.experienced(), what_act, what_con);
                    MyStatementParams what_terminate_params = new MyStatementParams(Verbs.terminated(), what_act, what_con);

                    WriteStatementTask what_terminate_stmt_task = new WriteStatementTask();
                    WriteStatementTask what_stmt_task = new WriteStatementTask();

                    what_stmt_task.execute(what_ex_params);
                    what_terminate_stmt_task.execute(what_terminate_params);
                }
                break;
            case _result_pruning:
                final Activity pruning_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_pruning_path),
                        getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));

                if (! isResult){
                    Intent pruningActivity = new Intent(MainActivity.this, PruningActivity.class);
                    // Initial launch so create attempt id
                    String pruning_attempt_id = UUID.randomUUID().toString();
                    Context pruning_con = createContext(getString(R.string.mod_pruning_path), pruning_attempt_id,
                            getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));
                    // send initialize statements and launch activity
                    MyStatementParams pruning_init_params = new MyStatementParams(Verbs.initialized(), pruning_act, pruning_con);
                    WriteStatementTask pruning_init_task = new WriteStatementTask();
                    pruning_init_task.execute(pruning_init_params);

                    pruningActivity.putExtra("sessionId", pruning_attempt_id);
                    startActivityForResult(pruningActivity, _result_pruning);
                }
                else{
                    Context pruning_con = createContext(getString(R.string.mod_pruning_path), attemptId,
                            getString(R.string.mod_pruning_name), getString(R.string.mod_pruning_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams pruning_exp_params = new MyStatementParams(Verbs.experienced(), pruning_act, pruning_con);
                    MyStatementParams pruning_terminate_params = new MyStatementParams(Verbs.terminated(), pruning_act, pruning_con);

                    WriteStatementTask pruning_exp_task = new WriteStatementTask();
                    WriteStatementTask pruning_terminate_stmt_task = new WriteStatementTask();

                    pruning_exp_task.execute(pruning_exp_params);
                    pruning_terminate_stmt_task.execute(pruning_terminate_params);
                }
                break;
            case _result_deadheading:
                final Activity dh_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_deadheading_path),
                        getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));

                if(! isResult){
                    Intent deadheadingActivity = new Intent(MainActivity.this, DeadHeadingActivity.class);
                    String dh_attempt_id = UUID.randomUUID().toString();
                    Context dh_con = createContext(getString(R.string.mod_deadheading_path), dh_attempt_id,
                            getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));
                    // send initialize statements and launch activity
                    MyStatementParams dh_init_params = new MyStatementParams(Verbs.initialized(), dh_act, dh_con);
                    WriteStatementTask dh_init_task = new WriteStatementTask();
                    dh_init_task.execute(dh_init_params);

                    deadheadingActivity.putExtra("sessionId", dh_attempt_id);
                    startActivityForResult(deadheadingActivity, _result_deadheading);
                }
                else
                {
                    Context dh_con = createContext(getString(R.string.mod_deadheading_path), attemptId,
                            getString(R.string.mod_deadheading_name), getString(R.string.mod_deadheading_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams dh_exp_params = new MyStatementParams(Verbs.experienced(), dh_act, dh_con);
                    MyStatementParams dh_terminate_params = new MyStatementParams(Verbs.terminated(), dh_act, dh_con);

                    WriteStatementTask dh_exp_stmt_task = new WriteStatementTask();
                    WriteStatementTask dh_terminate_stmt_task = new WriteStatementTask();

                    dh_exp_stmt_task.execute(dh_exp_params);
                    dh_terminate_stmt_task.execute(dh_terminate_params);
                }
                break;
            case _result_shearing:
                final Activity shear_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_shearing_path),
                        getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));

                if(! isResult) {
                    Intent shearingActivity = new Intent(MainActivity.this, ShearingActivity.class);
                    String shear_attempt_id = UUID.randomUUID().toString();
                    Context shear_con = createContext(getString(R.string.mod_shearing_path), shear_attempt_id,
                            getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));
                    // send initialize statements and launch activity
                    MyStatementParams shear_init_params = new MyStatementParams(Verbs.initialized(), shear_act, shear_con);
                    WriteStatementTask shear_init_task = new WriteStatementTask();
                    shear_init_task.execute(shear_init_params);

                    shearingActivity.putExtra("sessionId", shear_attempt_id);
                    startActivityForResult(shearingActivity, _result_shearing);
                }
                else
                {
                    Context shear_con = createContext(getString(R.string.mod_shearing_path), attemptId,
                            getString(R.string.mod_shearing_name), getString(R.string.mod_shearing_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams shear_exp_params = new MyStatementParams(Verbs.experienced(), shear_act, shear_con);
                    MyStatementParams shear_terminate_params = new MyStatementParams(Verbs.terminated(), shear_act, shear_con);

                    WriteStatementTask shear_exp_stmt_task = new WriteStatementTask();
                    WriteStatementTask shear_terminate_stmt_task = new WriteStatementTask();

                    shear_exp_stmt_task.execute(shear_exp_params);
                    shear_terminate_stmt_task.execute(shear_terminate_params);
                }
                break;
            case _result_hybrids:
                final Activity hybrid_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_hybrids_path),
                        getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));

                if(! isResult) {
                    Intent hybridsActivity = new Intent(MainActivity.this, HybridsActivity.class);
                    String hybrid_attempt_id = UUID.randomUUID().toString();
                    Context hybrid_con = createContext(getString(R.string.mod_hybrids_path), hybrid_attempt_id,
                            getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));
                    // send initialize statements and launch activity
                    MyStatementParams hybrid_init_params = new MyStatementParams(Verbs.initialized(), hybrid_act, hybrid_con);
                    WriteStatementTask hybrid_init_task = new WriteStatementTask();
                    hybrid_init_task.execute(hybrid_init_params);

                    hybridsActivity.putExtra("sessionId", hybrid_attempt_id);
                    startActivityForResult(hybridsActivity, _result_hybrids);
                }
                else
                {
                    Context hybrid_con = createContext(getString(R.string.mod_hybrids_path), attemptId,
                            getString(R.string.mod_hybrids_name), getString(R.string.mod_hybrids_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams hybrid_exp_params = new MyStatementParams(Verbs.experienced(), hybrid_act, hybrid_con);
                    MyStatementParams hybrid_terminate_params = new MyStatementParams(Verbs.terminated(), hybrid_act, hybrid_con);

                    WriteStatementTask hybrid_exp_stmt_task = new WriteStatementTask();
                    WriteStatementTask hybrid_terminate_stmt_task = new WriteStatementTask();

                    hybrid_exp_stmt_task.execute(hybrid_exp_params);
                    hybrid_terminate_stmt_task.execute(hybrid_terminate_params);
                }
                break;
            case _result_styles:
                final Activity florist_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_styles_path),
                        getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));

                if(! isResult) {
                    Intent floristryActivity = new Intent(MainActivity.this, FloristryActivity.class);
                    String florist_attempt_id = UUID.randomUUID().toString();
                    Context florist_con = createContext(getString(R.string.mod_styles_path), florist_attempt_id,
                            getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));
                    // send initialize statements and launch activity
                    MyStatementParams florist_init_params = new MyStatementParams(Verbs.initialized(), florist_act, florist_con);
                    WriteStatementTask florist_init_task = new WriteStatementTask();
                    florist_init_task.execute(florist_init_params);

                    floristryActivity.putExtra("sessionId", florist_attempt_id);
                    startActivityForResult(floristryActivity, _result_styles);
                }
                else
                {
                    Context florist_con = createContext(getString(R.string.mod_styles_path), attemptId,
                            getString(R.string.mod_styles_name), getString(R.string.mod_styles_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams florist_exp_params = new MyStatementParams(Verbs.experienced(), florist_act, florist_con);
                    MyStatementParams florist_terminate_params = new MyStatementParams(Verbs.terminated(), florist_act, florist_con);

                    WriteStatementTask florist_exp_stmt_task = new WriteStatementTask();
                    WriteStatementTask florist_terminate_stmt_task = new WriteStatementTask();

                    florist_exp_stmt_task.execute(florist_exp_params);
                    florist_terminate_stmt_task.execute(florist_terminate_params);
                }
                break;
            case _result_symbolism:
                final Activity sym_act = createActivity(getString(R.string.app_activity_iri) + getString(R.string.mod_symbolism_path),
                        getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));

                if(! isResult) {
                    Intent symbolismActivity = new Intent(MainActivity.this, SymbolismActivity.class);
                    String sym_attempt_id = UUID.randomUUID().toString();
                    Context sym_con = createContext(getString(R.string.mod_symbolism_path), sym_attempt_id,
                            getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));
                    // send initialize statements and launch activity
                    MyStatementParams sym_init_params = new MyStatementParams(Verbs.initialized(), sym_act, sym_con);
                    WriteStatementTask sym_init_task = new WriteStatementTask();
                    sym_init_task.execute(sym_init_params);

                    symbolismActivity.putExtra("sessionId", sym_attempt_id);
                    startActivityForResult(symbolismActivity, _result_symbolism);
                }
                else
                {
                    Context sym_con = createContext(getString(R.string.mod_symbolism_path), attemptId,
                            getString(R.string.mod_symbolism_name), getString(R.string.mod_symbolism_description));
                    // returned result from launched activity, send experienced and terminated
                    MyStatementParams sym_exp_params = new MyStatementParams(Verbs.experienced(), sym_act, sym_con);
                    MyStatementParams sym_terminate_params = new MyStatementParams(Verbs.terminated(), sym_act, sym_con);

                    WriteStatementTask sym_exp_stmt_task = new WriteStatementTask();
                    WriteStatementTask sym_terminate_stmt_task = new WriteStatementTask();

                    sym_exp_stmt_task.execute(sym_exp_params);
                    sym_terminate_stmt_task.execute(sym_terminate_params);
                }
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Bundle extras = data.getExtras();
        String sessionId = "";
        if (extras != null){
            sessionId = extras.getString("sessionId");
        }
        if(resultCode == RESULT_OK) {
            sendStatements(requestCode, true, sessionId);
        }
        else{
            sendSuspendedStatements(requestCode, sessionId);
        }
    }

    private Context createContext(String path, String mod_attempt_id, String name, String desc){
        Context con = new Context();
        ContextActivities con_acts = new ContextActivities();

        ArrayList<Activity> con_act_list = new ArrayList<Activity>();
        con_act_list.add(createActivity(getString(R.string.app_activity_iri),
                getString(R.string.context_name_desc), getString(R.string.context_name_desc)));

        con_act_list.add(createActivity(getString(R.string.app_activity_iri) + "?attemptId=" + _attempt_id,
                getString(R.string.context_name_desc), getString(R.string.context_name_desc)));
        con_acts.setParent(con_act_list);

        ArrayList<Activity> group_act_list = new ArrayList<Activity>();
        group_act_list.add(createActivity(getString(R.string.app_activity_iri) + path + "?attemptId=" + mod_attempt_id, name, desc));
        con_acts.setGrouping(group_act_list);
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

    private class WriteStatementTask extends AsyncTask<MyStatementParams, Void, Pair<Boolean, String>>{
        protected Pair<Boolean, String> doInBackground(MyStatementParams... params){
            checkActor();
            Agent agent = new Agent(_actor_name, "mailto:" + _actor_email);
            Statement stmt = new Statement();
            stmt.setActor(agent);
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
        Verb v;
        Activity a;
        Context c;
        String aID;
        MyStatementParams(Verb v, Activity a, Context c){
            this.v = v;
            this.a = a;
            this.c = c;
        }
        MyStatementParams(Verb v, String a){
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
                JsonArray stateIDs = ac.getActivityStates(getString(R.string.app_activity_iri), actor, null, null);
                JsonObject state = stateIDs.get(0).getAsJsonObject();
                // Get the attempts element from the state which will be an array itself
                JsonArray attempts = state.get("Attempts").getAsJsonArray();

                if (attempts.size() != 0){
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

    private class UpdateActivityStateTask extends AsyncTask<MyActivityStateParams, Void, List<String>>{
        protected List<String> doInBackground(MyActivityStateParams... params){
            android.os.Debug.waitForDebugger();
            checkActor();
            Agent actor = new Agent(_actor_name, "mailto:" + _actor_email);
            List<String> attempt_list = new ArrayList<String>();

            try{
                ActivityClient ac = new ActivityClient(getString(R.string.lrs_endpoint), getString(R.string.lrs_user),
                        getString(R.string.lrs_password));
                // This will retrieve an array of states (should only be one in the array)
                JsonArray stateIDs = ac.getActivityStates(getString(R.string.app_activity_iri), actor, null, null);

                if (stateIDs.size() > 0){
                    JsonObject state = stateIDs.get(0).getAsJsonObject();
                    // Get the attempts element from the state which will be an array itself
                    JsonArray attempts = state.get("Attempts").getAsJsonArray();
                }


                // Copy attempts into a string list and add the current attempt IRI
                for (int i =0; i < attempts.size(); i++){
                    attempt_list.add(attempts.get(i).getAsString());
                }
                attempt_list.add(getString(R.string.app_activity_iri)+params[0].path+"?attemptId="+params[0].attemptID);
            }
            catch (Exception ex){
                System.out.println(ex.getMessage());
                // List will still be null, no action needed
            }
            return attempt_list;
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
                success = ac.postActivityState(getString(R.string.app_activity_iri), actor, null, "", params[0]);
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

    private static class MyActivityStateParams{
        String path;
        String attemptID;
        MyActivityStateParams(String path, String attemptID){
            this.path = path;
            this.attemptID = attemptID;
        }
    }
}