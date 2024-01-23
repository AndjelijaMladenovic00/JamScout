package com.example.jamscout;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

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
    private ImageButton reportButton;
    private List<String> all_streets = null;

    @SuppressLint("MissingInflatedId")
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

        button = (ImageButton) findViewById(R.id.modeButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driving = !driving;

                if (driving) {
                    button.setImageResource(R.drawable.car);
                } else {
                    button.setImageResource(R.drawable.walking);
                }

                map.clear();

                if (currentLocation != null && (currentLocation.getLatitude() != nis.latitude || currentLocation.getLongitude() != nis.longitude)) {
                    LatLng location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    map.addMarker(new MarkerOptions().position(location).title("Vi ste ovde!"));
                }

                if (gotoMarker != null) {
                    gotoMarker.remove();
                }

                if (gotoLocation != null) {
                    gotoMarker = map.addMarker(new MarkerOptions().position(gotoLocation).title("Vaše odredište!"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(gotoLocation, 15));

                    if (!has_current_location) {
                        Toast.makeText(MapsActivity.this, "Omogućite lokaciju da bi vam sve funkcionalnosti bile dostupne.", Toast.LENGTH_LONG).show();
                    } else {
                        String url = getDirectionsURL(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), gotoLocation);
                        DownloadTask task = new DownloadTask(MapsActivity.this);
                        task.execute(url);
                    }
                }

            }
        });

        reportButton = (ImageButton) findViewById(R.id.reportButton);

        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

                builder.setTitle("Prijavljujete gužvu?");
                builder.setMessage("Potvrdom ovog dijaloga prijavićete gužvu na svojoj trenutnoj lokaciji. Da li želite da nastavite?");
                builder.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
                            Toast.makeText(MapsActivity.this, "Ne možete prijaviti gužvu ako Vam nije uključena lokacija!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Task<Location> task = flpc.getLastLocation();
                        task.addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                                    try {
                                        List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 3);
                                        String current_street_name = null;

                                        for (int i = 0; i < address.size(); i++) {

                                            List<String> split_address = Arrays.asList(address.get(i).getAddressLine(0).split(","));

                                            if (split_address.size() == 3) {
                                                String street_name = split_address.get(0);
                                                List<String> words = Arrays.asList(street_name.split(" "));
                                                List<String> remaining_words = new ArrayList<String>();

                                                Pattern pattern = Pattern.compile(".*\\d.*");

                                                for (String str : words) {
                                                    if (!pattern.matcher(str).matches()) {
                                                        remaining_words.add(str);
                                                    }
                                                }

                                                street_name = String.join("", remaining_words);

                                                if(current_street_name == null)
                                                    current_street_name = street_name;

                                                if(all_streets == null) {
                                                    break;
                                                }
                                                else if(all_streets.contains(street_name)){
                                                    current_street_name = street_name;
                                                    break;
                                                }
                                            }
                                        }

                                        if(current_street_name == null)
                                            Toast.makeText(MapsActivity.this, "Došlo je do greške pri prijavi gužve!", Toast.LENGTH_LONG).show();

                                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                                        String report = current_street_name+ "," + timeStamp + "\n";

                                        try {
                                            FileOutputStream fos = MapsActivity.this.openFileOutput("reports.csv", Context.MODE_APPEND);
                                            fos.write(report.getBytes());
                                            fos.close();

                                            Toast.makeText(MapsActivity.this, "Uspešno ste prijavili gužvu na svojoj trenutnoj lokaciji!", Toast.LENGTH_LONG).show();

                                        }catch(IOException e){
                                            Toast.makeText(MapsActivity.this, "Došlo je do greške pri prijavi gužve!", Toast.LENGTH_LONG).show();
                                        }
                                    } catch (IOException e) {
                                        Toast.makeText(MapsActivity.this, "Došlo je do greške pri prijavi gužve!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                    }
                });

                builder.setNegativeButton("Ne", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

                dialog = builder.create();
                dialog.show();
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

                if (gotoMarker != null) {
                    map.clear();
                    gotoMarker.remove();
                    gotoLocation = null;

                    if (currentLocation != null && (currentLocation.getLatitude() != nis.latitude || currentLocation.getLongitude() != nis.longitude)) {
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

                    gotoMarker = map.addMarker(new MarkerOptions().position(gotoLocation).title("Vaše odredište!"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(gotoLocation, 15));

                    if (!has_current_location) {
                        Toast.makeText(MapsActivity.this, "Omogucite lokaciju da bi vam sve funkcionalnosti bile dostupne.", Toast.LENGTH_LONG).show();
                    } else {
                        String url = getDirectionsURL(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), gotoLocation);
                        DownloadTask task = new DownloadTask(MapsActivity.this);
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

        if (Math.abs(location.latitude - nis.latitude) > 0.15 || Math.abs(location.longitude - nis.longitude) > 0.15) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(nis, 14));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }

        if (!location.equals(nis)) {
            map.addMarker(new MarkerOptions().position(location).title("Vi ste ovde!"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MapsActivity.this);

                Toast.makeText(this, "Niste omogucili pristup lokaciji, funkcionalnosti ce biti ogranicene.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getDirectionsURL(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        String mode;
        if (driving)
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
        private MapsActivity activity;

        public DownloadTask(MapsActivity activity) {
            this.activity = activity;
        }

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
            ParserTask parserTask = new ParserTask(activity);
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        private MapsActivity activity;

        public ParserTask(MapsActivity activity) {
            this.activity = activity;
        }

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
            ArrayList<LatLng> points = new ArrayList<LatLng>();
            List<List<String>> data = new ArrayList<List<String>>();
            List<String> previous_street_names = null;

            InputStream is = getResources().openRawResource(R.raw.tracking);

            try (CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))) {
                String[] values = null;
                boolean readFirst = false;
                while ((values = reader.readNext()) != null) {
                    if (!readFirst)
                        readFirst = true;
                    else data.add(Arrays.asList(values));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<String> content = new ArrayList<String>();

            try {
                FileInputStream fis = openFileInput("reports.csv");
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);

                br.readLine();

                String line;
                while ((line = br.readLine()) != null) {
                    if(!line.equals("")) content.add(line);
                }

                br.close();
                isr.close();
                fis.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < result.size(); i++) {

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);

                    Geocoder geocoder = new Geocoder(activity, Locale.getDefault());

                    try {
                        List<Address> address = geocoder.getFromLocation(position.latitude, position.longitude, 6);

                        all_streets = new ArrayList<String>();
                        String current_street = null;

                        for (i = 0; i < address.size(); i++) {

                            List<String> split_address = Arrays.asList(address.get(i).getAddressLine(0).split(","));

                            if (split_address.size() == 3) {
                                String street_name = split_address.get(0);
                                List<String> words = Arrays.asList(street_name.split(" "));
                                List<String> remaining_words = new ArrayList<String>();

                                Pattern pattern = Pattern.compile(".*\\d.*");

                                for (String str : words) {
                                    if (!pattern.matcher(str).matches()) {
                                        remaining_words.add(str);
                                    }
                                }

                                street_name = String.join("", remaining_words);

                                all_streets.add(street_name);

                                if (current_street == null && previous_street_names != null && previous_street_names.contains(street_name)) {
                                    current_street = street_name;
                                }
                            }
                        }
                        if (current_street == null) {
                            current_street = all_streets.get(0);
                        }

                        Log.i(TAG, "Trenutna ulica " + current_street );

                        if (j > 0) {
                            PolylineOptions options = new PolylineOptions();
                            options.add(points.get(j - 1));
                            options.add(points.get(j));
                            options.width(12);
                            options.geodesic(true);

                            int index = 0;
                            boolean found = false;
                            int number_of_reports = 0;

                            while (!found && index < data.size()) {
                                if (current_street.toLowerCase().contains(data.get(index).get(0).toLowerCase()) || current_street.toLowerCase().contains(data.get(index).get(1).toLowerCase())) {
                                    found = true;
                                    break;
                                } else {
                                    index++;
                                }
                            }

                            for(int n=0; n<content.size(); n++) {
                                String[] elements = content.get(n).split(",");
                                if(elements[0].toLowerCase().equals(current_street.toLowerCase())){
                                    String[] date_elements = elements[1].split("_");
                                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                                    String[] current_date_elements = timeStamp.split("_");

                                    if(!date_elements[0].equals(current_date_elements[0])) break;
                                    int timestamp_minutes = Integer.parseInt(date_elements[1].substring(0, 2)) * 60 + Integer.parseInt(date_elements[1].substring(2, 4));
                                    int current_minutes = Integer.parseInt(current_date_elements[1].substring(0, 2)) * 60 + Integer.parseInt(current_date_elements[1].substring(2, 4));

                                    if(current_minutes - timestamp_minutes <= 5) number_of_reports++;
                                }
                            }

                            if (!found || data.get(index).size() != 5) {
                                options.color(Color.BLACK);
                            } else {
                                double coefficient = Integer.parseInt(data.get(index).get(2)) / Integer.parseInt(data.get(index).get(4)) + number_of_reports * 0.5;
                                Log.i(TAG, "KOEFICIJENT " + current_street + " " + String.valueOf(coefficient));
                                if (driving) {
                                    coefficient += Integer.parseInt(data.get(index).get(3)) * 0.2;
                                } else
                                    coefficient += Integer.parseInt(data.get(index).get(3)) * 0.5;

                                if (coefficient <= 2) {
                                    options.color(Color.GREEN);
                                } else if (coefficient <= 4.5) {
                                    options.color(Color.YELLOW);
                                } else options.color(Color.RED);
                            }

                            map.addPolyline(options);
                        }

                        previous_street_names = all_streets;

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}


