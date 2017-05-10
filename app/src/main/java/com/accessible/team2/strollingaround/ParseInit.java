package com.accessible.team2.strollingaround;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by Eric on 3/25/2016.
 */

//connect the app to the database
public class ParseInit extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        //connnect to parse database
        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(new Parse.Configuration.Builder(this)
            .applicationId("get2gether")
            .server("http://159.203.253.5:6380/parse/")
            .build()
        );
    }//end onCreate
}//end ParseInit Class
