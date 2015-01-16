package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by lou on 1/12/15.
 */
public abstract class ContentActivity extends ActionBarActivity{
    private int android_id;
    private int current_slide;

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

    public void previousSlide(){
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
