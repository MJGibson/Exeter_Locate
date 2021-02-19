package com.riba2reality.wifimapper.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.riba2reality.wifimapper.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ManualScanFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ManualScanFragment extends Fragment {


    private int selectedLocation = -1;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_SECTION_NUMBER = "section_number";

    public ManualScanFragment() {
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment HomescreenFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static ManualScanFragment newInstance(String param1, String param2) {
//        ManualScanFragment fragment = new ManualScanFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }

    public static ManualScanFragment newInstance(int index) {
        ManualScanFragment fragment = new ManualScanFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            // TODO: Rename and change types of parameters
//            String mParam1 = getArguments().getString(ARG_PARAM1);
//            String mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_manual_scan, container, false);

        Spinner dropdown = rootView.findViewById(R.id.spinner1);

        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(this.getActivity(), R.array.locations1,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        staticAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        dropdown.setAdapter(staticAdapter);

        dropdown.setOnItemSelectedListener(dropDownListerner);


        return rootView;
    }// end of on create view

    private AdapterView.OnItemSelectedListener dropDownListerner = new AdapterView.OnItemSelectedListener() {


        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selectedLocation = position;

            ImageView mImageView= (ImageView) getActivity().findViewById(R.id.imageView);

            String imageName = getCharForNumber(position);

            if(imageName==null){
                return;//bail
            }

            if(imageName.compareTo("n")==0){
                imageName="o"; // there is no "n"
            }

            //imageName += ".png";

            //Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            Log.d("locImage: ", imageName);

            int image_id = getResources().getIdentifier(imageName, "drawable", getActivity().getPackageName());

            mImageView.setImageResource(image_id);




        }// end of onItenSelected

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    };// end of dropDownListerner

    private String getCharForNumber(int i) {
        return i >= 0 && i < 27 ? String.valueOf((char)(i + 97)) : null;
    }

}// end of class