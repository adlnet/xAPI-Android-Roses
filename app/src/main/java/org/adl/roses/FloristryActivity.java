package org.adl.roses;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class FloristryActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_styles_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floristry);

        final StyleSpan bss = new StyleSpan(Typeface.BOLD);
        final SpannableStringBuilder sb1 = new SpannableStringBuilder(getString(R.string.mod_styles_text_1));
        sb1.setSpan(bss, 0, 7, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        TextView txt1 = (TextView)findViewById(R.id.floristryText1);
        txt1.setText(sb1);

        final SpannableStringBuilder sb2 = new SpannableStringBuilder(getString(R.string.mod_styles_text_2));
        sb2.setSpan(bss, 0, 22, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        TextView txt2 = (TextView)findViewById(R.id.floristryText2);
        txt2.setText(sb2);

        final SpannableStringBuilder sb3 = new SpannableStringBuilder(getString(R.string.mod_styles_text_3));
        sb3.setSpan(bss, 0, 10, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        TextView txt3 = (TextView)findViewById(R.id.floristryText3);
        txt3.setText(sb3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_floristry, menu);
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
        returnResult();
    }

    public void returnResult(){
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}
