package org.adl.roses;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FloristryActivity extends ContentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.mod_styles_name));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floristry);

        setAndroidId(getIntent().getExtras().getInt("requestCode"));
        setCurrentSlide(getIntent().getExtras().getInt("slideId"));

        Button button = (Button) findViewById(R.id.florSuspend);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                returnResult(true);
            }
        });

        Button pbutton = (Button) findViewById(R.id.florPrev);
        pbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                previousSlide();
            }
        });

        Button nbutton = (Button) findViewById(R.id.florNext);
        nbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                nextSlide();
            }
        });

        // Check that the activity is u sing he layout version with
        // the fragment_container FameLayout
        if (findViewById(R.id.textFrag) != null){

            // However, if we'ere being restored from a previous state,
            // then we don't need to do anything and hsould return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null){
                return;
            }

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SlideFragment frag = new SlideFragment();
            fragmentTransaction.add(R.id.textFrag, frag).commit();
        }
    }
}
