package org.adl.roses;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SlideFragment extends Fragment {
    public SlideFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View myInflatedView = inflater.inflate(R.layout.fragment_slide, container, false);
        // Grab current activity and retrieve its moduleId and slideId
        ContentActivity activity = (ContentActivity)getActivity();
        int android_act_id = activity.getAndroidId();
        int slide_id = activity.getCurrentSlide();
        // Set the fragment's textview based on which module and slide is currently active
        TextView txt = (TextView)myInflatedView.findViewById(R.id.fragText);
        switch (android_act_id){
            case 0:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_what_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_what_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_what_3));
                        break;
                }
                break;
            case 1:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_pruning_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_pruning_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_pruning_3));
                        break;
                }
                break;
            case 2:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_deadheading_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_deadheading_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_deadheading_3));
                        break;
                }
                break;
            case 3:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_shearing_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_shearing_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_shearing_3));
                        break;
                }
                break;
            case 4:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_hybrids_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_hybrids_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_hybrids_3));
                        break;
                }
                break;
            case 5:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_styles_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_styles_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_styles_3));
                        break;
                }
                break;
            case 6:
                switch (slide_id){
                    case 0:
                        txt.setText(getString(R.string.mod_symbolism_1));
                        break;
                    case 1:
                        txt.setText(getString(R.string.mod_symbolism_2));
                        break;
                    case 2:
                        txt.setText(getString(R.string.mod_symbolism_3));
                        break;
                }
                break;
        }
        // Inflate the layout for this fragment
        return myInflatedView;
    }

}
