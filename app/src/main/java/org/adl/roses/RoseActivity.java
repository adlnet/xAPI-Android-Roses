package org.adl.roses;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class RoseActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_what_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rose);
        TextView txt = (TextView)findViewById(R.id.roseText);
        txt.setText(getString(R.string.mod_what_text));
        Button button = (Button) findViewById(R.id.whatSuspend);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                returnResult(true);
            }
        });

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
        Bundle extras = getIntent().getExtras();
        String sessionId = "";
        if (extras != null){
            sessionId = extras.getString("sessionId");
        }
        Intent returnIntent = new Intent();
        returnIntent.putExtra("sessionId", sessionId);
        if (suspended){
            setResult(RESULT_CANCELED, returnIntent);
        }
        else{
            setResult(RESULT_OK, returnIntent);
        }
        finish();
    }
}
