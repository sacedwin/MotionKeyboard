package com.example.motionkeyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener {
    private String message = "";
    private String currentGroupOfChars = null;

    private SensorManager mSensorManager;
    private enum Direction
    {
        LEFT, RIGHT;
    }

    private CharacterBin keyboard;
    private int indexOnCharGroups, indexOnSlidingButtons, indexFirstButtonChar;
    private long start;
    private ArrayList<Button> buttons;
    private ArrayList<Button> slidingButtons;
    private Button buttonSpace;
    private boolean switchBetweenCharsInAGroup;

    private float[] accels = new float[3];
    private float[] mags = new float[3];
    private float[] values = new float[3];

    // azimuth, pitch and roll
    private float azimuth;
    private float pitch;
    private float roll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
        }
        keyboard = new CharacterBin(); // initial version
        indexOnCharGroups = 0;
        indexOnSlidingButtons = 0;
        indexFirstButtonChar = 0;
        switchBetweenCharsInAGroup = false;
        start = System.currentTimeMillis();
        setContentView(R.layout.activity_main);
        initialiseButtons();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void updateMessage(char letter) {
        message += letter;
        ((TextView)findViewById(R.id.inputTextView)).setText(message+" |");
    }

    /*Sensor*/
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = event.values.clone();
                break;
        }

        if (mags != null && accels != null) {
            // Gravity rotational data
            float[] gravity = new float[9];
            // Magnetic rotational data
            //for magnetic rotational data
            float[] magnetic = new float[9];
            SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
            float[] outGravity = new float[9];
            SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
            SensorManager.getOrientation(outGravity, values);

            azimuth = values[0] * 57.2957795f; //z axis rotation
            pitch =values[1] * 57.2957795f;
            roll = values[2] * 57.2957795f;
            mags = null;
            accels = null;
        }
        if (System.currentTimeMillis() - start < 800) {
            return;
        }
        start = System.currentTimeMillis();

        buttonSpace.setPressed(false);

        currentGroupOfChars = switchBetweenCharGroupsAndSelect(currentGroupOfChars);
        switchBetweenCharsInAGroupAndSelectIfRotationOnX();
        if(switchBetweenCharsInAGroup) {
            iterateOnSlidingButtonsList();
        }
    }

    private void switchBetweenCharsInAGroupAndSelectIfRotationOnX(){
        if(currentGroupOfChars == null) {
            switchBetweenCharsInAGroup = false;
            return;
        }
        if(pitch  > 30 && !switchBetweenCharsInAGroup) { // if X rotation < -30, switch to the sliding buttons
            setVisibilityOfSlidingButtons(View.VISIBLE);
            initializeTextOfSlidingButtons(currentGroupOfChars);
            switchBetweenCharsInAGroup = true;
        }
    }

    private String switchBetweenCharGroupsAndSelect(String currentCharGroup) {

        if(currentGroupOfChars != null) {
            return currentCharGroup;
        }
        System.out.println("Switching. Bin = "+indexOnSlidingButtons);

        setVisibilityOfSlidingButtons(View.INVISIBLE);
        buttons.get(indexOnCharGroups).requestFocus();
        if(roll < -25){ //Z rotation > 20
            indexOnCharGroups = indexOnCharGroups - 1 >= 0 ? indexOnCharGroups - 1 : indexOnCharGroups;
            buttons.get(indexOnCharGroups).requestFocus();
            return null;
        }
        if(roll > 25){ //Z rotation < -20
            indexOnCharGroups = indexOnCharGroups + 1 < buttons.size() ? indexOnCharGroups + 1 : indexOnCharGroups;
            buttons.get(indexOnCharGroups).requestFocus();
            return null;
        } //Z rotation between -20 and 20
        if(pitch > 30){ // X rotation <-30
            if(indexOnCharGroups >= 5)
            {
                buttons.get(indexOnCharGroups).performClick();
                return null;
            } else {
                buttons.get(indexOnCharGroups).setPressed(true);
                indexOnSlidingButtons = 0;
                indexFirstButtonChar = 0;
                return keyboard.getBin(indexOnCharGroups);
            }
        }else{
            buttons.get(indexOnCharGroups).setPressed(false);
            buttons.get(indexOnCharGroups).requestFocus();
        }

        if(pitch<5){
            buttonSpace.performClick();
        }
        return currentCharGroup;
    }

    //Move between buttons in the expanded bin
    private void iterateOnSlidingButtonsList(){
        System.out.println("Iterating. Index = "+indexOnSlidingButtons);
        if(roll < -30) { //Z rotation > 20 move left
            if(indexOnSlidingButtons == 0){
                updateTextOfSlidingButtons(indexOnCharGroups, Direction.LEFT);
            }
            indexOnSlidingButtons = indexOnSlidingButtons - 1 >= 0 ? indexOnSlidingButtons - 1 : indexOnSlidingButtons;
            slidingButtons.get(indexOnSlidingButtons).requestFocus();
            return;
        }
        if(roll > 30){ //Z rotation < -20 move right
            if(indexOnSlidingButtons == slidingButtons.size()-1){
                updateTextOfSlidingButtons(indexOnCharGroups, Direction.RIGHT);
            }

            indexOnSlidingButtons = indexOnSlidingButtons + 1 < slidingButtons.size() ? indexOnSlidingButtons + 1 : indexOnSlidingButtons;
            slidingButtons.get(indexOnSlidingButtons).requestFocus();
            return;
        }
        if(pitch  > 50) { // if X rotation < -30, select current button's character
            //slidingButtons.get(indexOnSlidingButtons).performClick();
            updateMessage(slidingButtons.get(indexOnSlidingButtons).getText().charAt(0));
            slidingButtons.get(indexOnSlidingButtons).performClick();
        }else{
            slidingButtons.get(indexOnSlidingButtons).setPressed(false);
            slidingButtons.get(indexOnSlidingButtons).requestFocus();
        }
        if(pitch < 15){ // X rotation > 30
            currentGroupOfChars = null;
            switchBetweenCharsInAGroup = false;
            setVisibilityOfSlidingButtons(View.INVISIBLE);
            buttons.get(indexOnCharGroups).requestFocus();
        }
    }

    private void initializeTextOfSlidingButtons(String chars){
        for(int index = 0; index < slidingButtons.size(); index++){
            slidingButtons.get(index).setText(String.valueOf(chars.charAt(index)));
        }
        slidingButtons.get(0).requestFocus();
    }

    private void updateTextOfSlidingButtons(int bin, Direction direction){
        //move index left or right
        if(direction == Direction.RIGHT){
            indexFirstButtonChar+=1;
            if(indexFirstButtonChar>keyboard.getBin(bin).length()){
                indexFirstButtonChar = 0;
            }
        }else if(direction == Direction.LEFT){
            indexFirstButtonChar-=1;
            if(indexFirstButtonChar<0){
                indexFirstButtonChar = keyboard.getBin(bin).length()-1;
            }
        }

        for(int i=0; i<slidingButtons.size(); i++){
            int binPos = (indexFirstButtonChar+i)%keyboard.getBin(bin).length();
            slidingButtons.get(i).setText(String.valueOf(keyboard.getChar(bin, binPos)));
        }
    }

    private void setVisibilityOfSlidingButtons(int visibility) {
        findViewById(R.id.slidingButton0).setVisibility(visibility);
        findViewById(R.id.slidingButton1).setVisibility(visibility);
        findViewById(R.id.slidingButton2).setVisibility(visibility);
        findViewById(R.id.slidingButton3).setVisibility(visibility);
        findViewById(R.id.slidingButton4).setVisibility(visibility);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /*Buttons*/
    private void initialiseButtons(){
        buttons = new ArrayList<>();
        final Button button0 = findViewById(R.id.button0);
        final Button button1 = findViewById(R.id.button1);
        final Button button2 = findViewById(R.id.button2);
        final Button button3 = findViewById(R.id.button3);
        final Button button4 = findViewById(R.id.button4);
        final Button buttonDelete = findViewById(R.id.buttonDelete);
        final Button buttonCopy = findViewById(R.id.buttonCopy);
        final Button buttonSms = findViewById(R.id.buttonSms);
        buttonSpace = findViewById(R.id.buttonSpace);

        buttons.add(button0);
        buttons.add(button1);
        buttons.add(button2);
        buttons.add(button3);
        buttons.add(button4);
        buttons.add(buttonDelete);
        buttons.add(buttonCopy);
        buttons.add(buttonSms);

        for(int i=0; i<buttons.size()-1; i++){
            setButtonText(buttons.get(i), i);
            setButtonListener(buttons.get(i), i, 0, buttons);
            buttons.get(i).setFocusableInTouchMode(true);

        }
        buttonSms.setFocusableInTouchMode(true);


        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0; i<buttons.size(); i++){
                    buttons.get(i).setPressed(false);
                }
                buttonDelete.setPressed(true);

                if ((message != null) && (message.length() > 0)) {
                    message = message.substring(0, message.length() - 1);
                    ((TextView)findViewById(R.id.inputTextView)).setText(message);
                }
            }
        });

        buttonSpace.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0; i<buttons.size(); i++){
                    buttons.get(i).setPressed(false);
                }
                buttonSpace.setPressed(true);

                if ((message != null) && (message.length() > 0)) {
                    updateMessage(' ');
                }
            }
        });

        buttonCopy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0; i<buttons.size(); i++){
                    buttons.get(i).setPressed(false);
                }
                buttonCopy.setPressed(true);

                //code to copy texts into clipboard, the text part need to be changed to textbox value(copied text)
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MotionKeyboardText",message);
                clipboard.setPrimaryClip(clip);
            }
        });

        buttonSms.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click to send sms, clear the box if required after sending sms
                // sendSMS();
                for(int i=0; i<buttons.size(); i++){
                    buttons.get(i).setPressed(false);
                }
                buttonSms.setPressed(true);

                try {

                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage("0450639014", null, message, null, null);
                    buttonSms.setPressed(true);
                    Toast.makeText(getApplicationContext(), "SMS Sent!",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            //"SMS faild, please try again later!",
                            "e  "+e,
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });


        slidingButtons = new ArrayList<>();

        final Button slidingButton0 = findViewById(R.id.slidingButton0);
        final Button slidingButton1 = findViewById(R.id.slidingButton1);
        final Button slidingButton2 = findViewById(R.id.slidingButton2);
        final Button slidingButton3 = findViewById(R.id.slidingButton3);
        final Button slidingButton4 = findViewById(R.id.slidingButton4);

        slidingButtons.add(slidingButton0);
        slidingButtons.add(slidingButton1);
        slidingButtons.add(slidingButton2);
        slidingButtons.add(slidingButton3);
        slidingButtons.add(slidingButton4);

        for(int i = 0; i < slidingButtons.size(); i++){
            setButtonText(slidingButtons.get(i), i);
            setButtonListener(slidingButtons.get(i), i, 0, slidingButtons);
            slidingButtons.get(i).setFocusableInTouchMode(true);

        }

        setVisibilityOfSlidingButtons(View.INVISIBLE);

    }

    private void setButtonText(Button button, int bin){
        if(bin<keyboard.getBinTotal()){
            String buttonText = keyboard.binText(bin);
            button.setText(buttonText);
        }
    }

    private void setButtonListener(Button button, final int bin, int binPos, final ArrayList<Button> buttongroup){
        final int binNumber = bin;
        final int binPosition = binPos;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                buttongroup.get(bin).setPressed(true);
                //set buttons on the side to "unpressed"

                for(int i=0; i<buttongroup.size(); i++){
                    if(i!=bin){
                        buttongroup.get(i).setPressed(false);
                    }
                }
            }
        });
    }


}

