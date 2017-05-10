package com.accessible.team2.strollingaround;
/**
 * Created by Mikael A Mendoza on 2/17/2016
 */
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Toast;

public class SplashScreen extends Activity {
    private static int SPLASH_LENGTH = 3000;
    MediaPlayer mp1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        Thread timerThread = new Thread(){
            public void run(){
                try{
                    sleep(SPLASH_LENGTH);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }finally{
                    Intent intent = new Intent(SplashScreen.this,MapsActivity.class);
                    startActivity(intent);
                }
            }
        };
        timerThread.start();
        mp1 = MediaPlayer.create(this, R.raw.welcome);
        mp1.start ();
        Toast.makeText(SplashScreen.this, "WELCOME",
                Toast.LENGTH_LONG).show();
    }//end onCreate

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
        mp1.stop();
    }//end onPause

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mp1 != null) {
            if (mp1.isPlaying ()) mp1.stop ();
            mp1.release ();
            mp1 = null;
        }
    }//end onDestroy

}//end SplashScreen Class