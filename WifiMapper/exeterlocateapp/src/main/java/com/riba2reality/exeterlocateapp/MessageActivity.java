package com.riba2reality.exeterlocateapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MessageActivity extends AppCompatActivity {


    private ImageView messageIcon;
    private TextView title;
    private TextView message;


    //==============================================================================================
    /**
     * Creates and sets up the message activity UI
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up UI
        setContentView(R.layout.message_layout);


        // setup pointers to UI elements
        messageIcon = findViewById(R.id.imageView_message_icon);
        title = findViewById(R.id.textView_title);
        message = findViewById(R.id.textView_Message);

        Intent intent = getIntent();

        if(intent != null) {
            String titleText = intent.getStringExtra("title");
            String messageText = intent.getStringExtra("message");
            int iconResource = intent.getIntExtra("icon",-1);

            if(titleText != null)
                title.setText(titleText);
            if(messageText != null)
                message.setText(messageText);
            if(iconResource != -1) {

                messageIcon.setImageResource(iconResource);
            }


        }

    }// end of onCreate
    //==============================================================================================


}//end of MessageActivity