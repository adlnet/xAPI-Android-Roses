package org.adl.roses;

import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SlideFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SlideFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SlideFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SlideOneFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SlideFragment newInstance(String param1, String param2) {
        SlideFragment fragment = new SlideFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public SlideFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View myInflatedView = inflater.inflate(R.layout.fragment_slide, container, false);
        ContentActivity activity = (ContentActivity)getActivity();
        int android_act_id = activity.getAndroidId();
        int slide_id = activity.getCurrentSlide();
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

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
