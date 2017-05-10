package com.accessible.team2.strollingaround;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.location.LocationListener;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.barcode.Barcode;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener {
    private Context mContext;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private DrawerLayout drawerLayout;
    private LocationManager locManager;
    private ListView listView;
    private RelativeLayout mDrawer;
    private long UPDATE_INTERVAL = 60000;  /* 60 secs */
    private long FASTEST_INTERVAL = 5000; /* 5 secs */
    private LatLng currentLocale;
    private double latitude;
    private double longitude;
    public ArrayList<LatLng> coordinateList = new ArrayList<>();
    public static int MAP_ZOOM = 19;
    private int listSize = 0;
    private LatLng newLocale;

    /*Markers and Drawer attributes*/
    private ArrayList<Markers> alMarkers;
    private final int DRAWERSIZE = 500;
    private DrawerAdapter adapter;
    private static final String[] title = {"RAMP", "DOOR", "PARKING", "ELEVATOR", "RESTROOM", "ROUTES"}; //Marker Types
    private static final int[] img = {R.mipmap.disability, R.mipmap.entrance, R.mipmap.car, R.mipmap.elevator, R.mipmap.toilets, R.mipmap.direction_down}; //Marker Image

    /*Audio attributes*/
    private MediaPlayer mp2;
    private Boolean loaded = false; //ensures mp2 is only played when Activity first created

    //Database Attributes
    private List<ParseObject> parseObjects;
    private boolean commentPin = true; //check if need to save comment pin
    private boolean saveParseObject = true; //helps flag for user not saving pin
    private ParseGeoPoint userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drawer);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Creating Markers for adapter
        alMarkers = new ArrayList<>();
        for (int i = 0; i < title.length; i++) {
            Markers markers = new Markers();
            markers.title = title[i];
            markers.imgId = img[i];
            alMarkers.add(markers);
            Log.e("TESTMENU", "" + alMarkers.get(i).title + " " + alMarkers.get(i).imgId);
        }

        //audio
        if (!loaded) { //Play the Menu Instruction at start only
            mp2 = MediaPlayer.create(this, R.raw.display_menu);
            mp2.start();
            loaded = true;
            Toast.makeText(MapsActivity.this, "SWIPE SCREEN FROM LEFT TO DISPLAY MENU\n<--",
                    Toast.LENGTH_LONG).show();
        }



        //Changing width of the drawer
        mDrawer = (RelativeLayout) findViewById(R.id.relative_drawer);
        DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawer.getLayoutParams();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.5);
        mDrawer.setLayoutParams(params);
        //drawer listener
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                //testing log for drawer slide
                Log.e("test", "on drawer slide");
            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }

        });

        /*Update the map every 5 seconds*/
        final Handler h = new Handler();
        final int delay = 5000; //milliseconds
        h.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mMap.clear(); //first clear map to avoid stacking
                        updateQueryParse(); //add markers to map
                        h.postDelayed(this, delay);
                    }//end run
                }, delay); //end handeler update

        listView = (ListView) drawerLayout.findViewById(R.id.drawer_list);
        listView.setOnItemClickListener(this);
        adapter = new DrawerAdapter(MapsActivity.this, alMarkers);
        listView.setAdapter(adapter);

    }//end onCreate

    //update map with markers from database
    private void parseUpdateMarkers(double latitude, double longitude){
        //load geo points into local store
        ParseObject locationObject = new ParseObject("userLocation");
        userLocation = new ParseGeoPoint(latitude, longitude);
        locationObject.put("userLoc", userLocation);
        updateQueryParse();             //update map location
    }//end parseUpdateMarkers

    private void updateQueryParse() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("mapPoint");
        //search for marker locations in database
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> locationList, ParseException e) {
                if (e == null) {
                    //add markers to map
                    for (int i = 0; i < locationList.size(); i++){
                        ParseObject marker = locationList.get(i);
                        ParseGeoPoint parseGeoPoint = (ParseGeoPoint) marker.get("location");
                        double lat = parseGeoPoint.getLatitude();
                        double lon = parseGeoPoint.getLongitude();
                        String markerType = marker.getString("markerType");
                        String comment = marker.getString("comment");
                        Log.e("markergetlat", "lat " + lat + " long " + lon);
                        LatLng latLng = new LatLng(lat, lon);
                        Log.e("icon", "" + Arrays.asList(title).indexOf(markerType));
                        int imgPos = Arrays.asList(title).indexOf(markerType);
                        if (imgPos > 0 && imgPos < img.length)
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng).title(markerType)
                                    .draggable(true)
                                    .icon(BitmapDescriptorFactory.fromResource(img[imgPos]))
                                    .snippet(comment));
                    }
                }
                else
                    Log.e("queryNear", "failed");
            }//end void done
        });//end find in background
    }//end updateQueryParse


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
            LatLng temp = new LatLng(location.getLatitude(), location.getLongitude());
            coordinateList.add(temp);
            listSize++;
        }

        public void onProviderDisabled(String provider) {
            updateWithNewLocation(null);
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };//end locationListener

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true); //false to disable
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10L,
                0.0f, locationListener);
        Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            //calling parse data store
            parseUpdateMarkers(latitude, longitude);
            currentLocale = new LatLng(latitude, longitude);
            Log.e("TEST", "currentLocale.longitude : " + currentLocale.longitude + " // currentLocale.latitude : " + currentLocale.latitude);
            //move camera to current location
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocale));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocale, MAP_ZOOM));
        } else {
            updateWithNewLocation(location);
        }
    }//end mapOnReady


    private String getMapsApiDirectionsUrl() {
        String waypoints = "waypoints=optimize:true|"
                + currentLocale.latitude + "," + currentLocale.longitude
                + "|" + "|" + newLocale.latitude + ","
                + newLocale.longitude;

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        return url;
    }//end getMapsApiDirectionsUrl

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }//end doInBackground

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }//end onPostExecute
    }//end ReadTask class

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }//end doInBackground

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(2);
                polyLineOptions.color(Color.BLUE);
            }
            mMap.addPolyline(polyLineOptions);
        }//end onPostExecute
    }//end Parser Class

    protected void connectClient() {
        // Connect the client.
        if (isGooglePlayServicesAvailable() && mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }//end connectClient

    /*
     * Called when the Activity becomes visible.
    */
    @Override
    protected void onResume() {
        super.onResume();
        connectClient();
    }//end onResume

    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            Toast.makeText(this, "GPS location was found!", Toast.LENGTH_SHORT).show();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate);
            startLocationUpdates();
        } else {//gps not found
            Toast.makeText(this, "Current location was null, enable GPS on emulator!", Toast.LENGTH_SHORT).show();
        }
    }//end onConnected

    private void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }//end if
    }//end locationUpdates


    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }//end connectionSuspend

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //debugging log info
        Log.d("Connection Failed", "Connection somehow failed");
    }//end connectionFailed

    private boolean isGooglePlayServicesAvailable() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            return true;
        } else {
            //google play services not available
            return false;
        }
    }//end isGooglePlayServicesAvailable

    private void updateWithNewLocation(Location location) {

        String latLongString = "";
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            latLongString = "Lat:" + lat + "\nLong:" + lng;
        } else {
            //do nothing
        }
        //log updated location
        Log.e("updateNewLocation", latLongString);
    }//end updateWithNewLocation


    //onclick listener for drawer
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //  Permissions Check
            return;
        }
        //Get current location and call the appropriate Dialog to Pop Up
        Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            String markerComment;  //holds comment if given
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            currentLocale = new LatLng(latitude, longitude);
            //prep object to save to database
            //objects saved to database are named mapPointy
            final ParseObject parseGeoObject = new ParseObject("mapPoint");
            final ParseGeoPoint parseGeoPoint = new ParseGeoPoint(latitude, longitude);
            //name Geopoints attached to mapPoint, "location"
            parseGeoObject.put("location", parseGeoPoint);
            //end prepping
            Log.e("LATIF", "currentLocale.longitude : " + currentLocale.longitude
                    + " // currentLocale.latitude : " + currentLocale.latitude);

            if (position < 5) { //If a Marker is selected addCommentPopUp() dialog is called
                markerComment = addCommentPopUp(currentLocale, position);
                //add marker type to object
                parseGeoObject.put("markerType", title[position]);
                //add comment to database if given
                if(commentPin)
                    parseGeoObject.put("comment", markerComment);
                drawerLayout.closeDrawer(mDrawer);
            }
            else {   //if Routes is selected newRoutePopUp() is called instead
                newRoutePopUp(currentLocale);
                drawerLayout.closeDrawer(mDrawer);
            }
            //save object to database if flaged to do so
            if(saveParseObject) //saveParseObject is false if user decides to cancel the marker
                parseGeoObject.saveInBackground();
        }
        else {//location is null
            updateWithNewLocation(location);
            drawerLayout.closeDrawer(mDrawer);
        }

    }//end onItemClick


    //****ADD COMMENT POP UP*************************************
// This POP-UP Dialog allows user to enter comment about new pin to be added********************
    public String addCommentPopUp(LatLng currentLocale, int position) {
        saveParseObject = true; //default to save the marker
        commentPin = false; //default no comment chosen
        final String[] userComment = new String[1];
        final int choice = position;
        final LatLng mark = currentLocale;
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("COMMENT ABOUT " + title[position]+"?");
        helpBuilder.setMessage("WRITE A SHORT COMMENT BELOW");
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setText("");
        helpBuilder.setView(input);
        helpBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Get Comment from User and add it as New Marker's snippet
                        commentPin = true;
                        userComment[0] = input.getText().toString();
                        if (userComment[0] == null) {
                            userComment[0] = " ";  //in case user didn't enter any input text
                        }
                        //add marker to map
                        mMap.addMarker(new MarkerOptions()
                                .position(mark)
                                .title(title[choice])
                                .snippet(userComment[0])
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.fromResource(img[choice])));

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(mark));
                        Toast.makeText(MapsActivity.this, "" + title[choice] + " PIN DROPPED ",
                                Toast.LENGTH_LONG).show();
                    }
                });
        helpBuilder.setNeutralButton("NO COMMENT",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Add new Marker without a Comment
                        mMap.addMarker(new MarkerOptions()
                                .position(mark)
                                .title(title[choice])
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.fromResource(img[choice])));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(mark));
                        Toast.makeText(MapsActivity.this, "" + title[choice] + " PIN DROPPED ",
                                Toast.LENGTH_LONG).show();
                        commentPin = false; //no comment choice chosen
                    }
                });
        helpBuilder.setNegativeButton("CANCEL",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing, Cancel adding a Marker
                        commentPin = false;
                        saveParseObject = false;
                    }
                });
        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
        return userComment[0];
    }//end addCommentPopup
    //END POP UP*******

    /*********NEW ROUTE Pop-Up ******************************************
     * This Dialog prompts the User to Enter a destination to look up a Route from
     *their current location
     * Currently Routes are done via a call to the Google Maps App, though later it shall be
     * handled within this app itself
     */
    public void newRoutePopUp(final LatLng currentLocation) {
        final String[] destination = new String[1];
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("NEW ROUTE");
        helpBuilder.setMessage("ENTER YOUR DESTINATION");
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setText("");
        helpBuilder.setView(input);
        helpBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Pass userComment
                        destination[0] = input.getText().toString();    //GET DESTINATION FROM USER
                        Toast.makeText(MapsActivity.this, "" + " NEW ROUTE REQUESTED ",
                                Toast.LENGTH_LONG).show();
                        //Google Maps is called and current location and Destination are passed to it
                        //Note that &dirflg=w is invoked to specify a Walking Route mode to Maps
                        String url = "http://maps.google.com/maps?saddr="+currentLocation.latitude
                                +","+currentLocation.longitude
                                +"&daddr="+destination[0]+"&dirflg=w";
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                        startActivity(intent);
                    }
                });
        helpBuilder.setNegativeButton("CANCEL", //cancels the new Route request
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing
                    }
                });
        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
    }//end newroutePopUp

    //END POP UP

    public void drawPoints() {
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (int i = 0; i < listSize; i++) {
            LatLng point = coordinateList.get(i);
            options.add(point);
        }
        mMap.addPolyline(options);
    }//end draw points

    @Override
    protected void onPause() {
        //Stops the Swipe Instruction message if Activity is paused
        super.onPause();
        finish();
        mp2.stop();
    }//end onPause
    @Override
    protected void onDestroy() {
        //if Activity is Destroyed while still playing the Swipe Instruction
        //it is terminated
        super.onDestroy();
        if (mp2 != null) {
            if (mp2.isPlaying()) mp2.stop();
            mp2.release();
            mp2 = null;
        }
    }//end onDestroy

}//end Maps Activity