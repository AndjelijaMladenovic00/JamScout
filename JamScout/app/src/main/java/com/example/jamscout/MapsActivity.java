package com.example.jamscout;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int FINE_PERMISSION_CODE = 1;
    final LatLng nis = new LatLng(43.3139854, 21.8900026);
    private GoogleMap map;
    private boolean has_current_location = false;
    private Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
    private LatLng gotoLocation;
    private Marker gotoMarker;
    private FusedLocationProviderClient flpc;
    private boolean driving = true;

    private ImageButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        currentLocation.setLatitude(43.3139854);
        currentLocation.setLongitude(21.8900026);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAvkuVq1jdKvLKFrSLBuT9rbjzlJ9mUU4k", Locale.US);
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));


        flpc = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        button = (ImageButton)findViewById(R.id.modeButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driving = !driving;

                if(driving){
                    button.setImageResource(R.drawable.car);
                }
                else{
                    button.setImageResource(R.drawable.walking);
                }

                map.clear();

                if(currentLocation != null && (currentLocation.getLatitude()!= nis.latitude || currentLocation.getLongitude() != nis.longitude)){
                    LatLng location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    map.addMarker(new MarkerOptions().position(location).title("Vi ste ovde!"));
                }

                if(gotoMarker!=null){
                    gotoMarker.remove();
                }

                if(gotoLocation != null) {
                    gotoMarker = map.addMarker(new MarkerOptions().position(gotoLocation).title("Vase odrediste!"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(gotoLocation, 15));

                    if (!has_current_location) {
                        Toast.makeText(MapsActivity.this,"Omogucite lokaciju da bi vam sve funkcionalnosti bile dostupne.",Toast.LENGTH_LONG).show();
                    }
                    else{
                        String url = getDirectionsURL(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), gotoLocation);
                        DownloadTask task = new DownloadTask();
                        task.execute(url);
                    }
                }

            }
        });


            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                String searched_location = place.getName();
                List<Address> addressList;

                if(gotoMarker!=null){
                    map.clear();
                    gotoMarker.remove();
                    gotoLocation = null;

                    if(currentLocation != null && (currentLocation.getLatitude()!= nis.latitude || currentLocation.getLongitude() != nis.longitude)){
                        LatLng location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        map.addMarker(new MarkerOptions().position(location).title("Vi ste ovde!"));
                    }
                }

                if (searched_location != null) {
                    Geocoder geocoder = new Geocoder(MapsActivity.this);

                    try {
                        addressList = geocoder.getFromLocationName(searched_location, 1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Address address = addressList.get(0);
                    gotoLocation = new LatLng(address.getLatitude(), address.getLongitude());

                    gotoMarker = map.addMarker(new MarkerOptions().position(gotoLocation).title("Vase odrediste!"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(gotoLocation, 15));

                    if (!has_current_location) {
                        Toast.makeText(MapsActivity.this,"Omogucite lokaciju da bi vam sve funkcionalnosti bile dostupne.",Toast.LENGTH_LONG).show();
                    }
                    else{
                        String url = getDirectionsURL(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), gotoLocation);
                        DownloadTask task = new DownloadTask();
                        task.execute(url);
                    }
                }
            }

        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = flpc.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    has_current_location = true;
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MapsActivity.this);
                }
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MapsActivity.this);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        LatLng location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        if(Math.abs(location.latitude - nis.latitude) > 0.15 || Math.abs(location.longitude - nis.longitude) > 0.15) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(nis, 14));
        }
        else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }

        if(!location.equals(nis)){
            map.addMarker(new MarkerOptions().position(location).title("Vi ste ovde!"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MapsActivity.this);

                Toast.makeText(this, "Niste omogucili pristup lokaciji, funkcionalnosti ce biti ogranicene.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getDirectionsURL(LatLng origin, LatLng dest){
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        String mode;
        if(driving)
            mode = "mode=driving";
        else mode = "mode=walking";

        String parameters = str_origin + "&" + str_dest + "&" + mode;
        String output = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + "AIzaSyAvkuVq1jdKvLKFrSLBuT9rbjzlJ9mUU4k";

        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DataParser parser = new DataParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = new ArrayList();
            PolylineOptions lineOptions = new PolylineOptions();

            for (int i = 0; i < result.size(); i++) {

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

            if (points.size() != 0)
                map.addPolyline(lineOptions);
        }
    }
}

