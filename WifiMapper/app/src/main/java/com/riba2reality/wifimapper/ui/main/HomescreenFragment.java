package com.riba2reality.wifimapper.ui.main;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.riba2reality.wifimapper.DataStores.ServerMessage;
import com.riba2reality.wifimapper.R;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomescreenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomescreenFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_SECTION_NUMBER = "section_number";

    private ScrollView scroll;
    private TextView logTextView;

    private final Queue<String> messagesQueue = new ConcurrentLinkedQueue<>();


    //==============================================================================================
    public HomescreenFragment() {
        // Required empty public constructor
    }
    //==============================================================================================


    //==============================================================================================
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomescreenFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomescreenFragment newInstance(String param1, String param2) {
        HomescreenFragment fragment = new HomescreenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    //==============================================================================================

    //==============================================================================================
    public static HomescreenFragment newInstance(int index) {
        HomescreenFragment fragment = new HomescreenFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("Trace", "HomescreenFragment.onCreateView");

        CharSequence txt = null;

        if(logTextView!=null){

            Log.d("Trace", "HomescreenFragment.onCreateView(logTextView!=null)");

            txt = logTextView.getText();

        }


        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_homescreen, container, false);

        logTextView = rootView.findViewById(R.id.log);

        scroll = rootView.findViewById(R.id.logScroll);

        if(txt!=null){
            logTextView.setText(txt);
        }

        if(messagesQueue.size() > 0){
            while (this.messagesQueue.size() > 0) {
                String message = messagesQueue.poll();
                addMessage(message);
            }

        }



        return rootView;
    }
    //==============================================================================================

    //==============================================================================================
    public void addMessage(String message){

        if (logTextView != null) {

            Log.d("Trace", "HomeScreenFragment.addMessage, logTextView != null");

            // append to the log text
            logTextView.append( message );

            // count the number of lines to remove, i.e. the number of lines > the maximum
            int linesToRemove = logTextView.getLineCount() - getActivity().getBaseContext().getResources().getInteger(R.integer.max_log_lines);

            // if there some to remove
            if (linesToRemove > 0) {
                // get the text from the logger and declare some variables we'll need
                Editable txt = logTextView.getEditableText();
                int lineStart, lineEnd, i;

                for (i = 0; i < linesToRemove; i++) {
                    // get the start and end locations of the first line of the text
                    lineStart = logTextView.getLayout().getLineStart(0);
                    lineEnd = logTextView.getLayout().getLineEnd(0);

                    // remove it
                    txt.delete(lineStart, lineEnd);
                }
            }

            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });

        }else{
            messagesQueue.add(message);
        }




    }// end of addMessage
    //==============================================================================================


}// end of class