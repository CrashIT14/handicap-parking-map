package se.creotec.ctkhandicapmap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng goteborg = new LatLng(57.714244, 11.975393);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // TODO: Refresh data
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        // Move camera to show Göteborg
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(goteborg, 12));

        new DownloadParkingDataAsync().execute();
    }

    public class DownloadParkingDataAsync extends AsyncTask<Void, Void, List<IHandicapParking>> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected List<IHandicapParking> doInBackground(Void... params) {
            try {
                return getParkingSpots();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return new ArrayList<IHandicapParking>();
            }
        }

        private List<IHandicapParking> readJson(InputStream is) throws IOException{
            List<IHandicapParking> resultList = new ArrayList<>();
            JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            reader.setLenient(true);

            reader.beginArray();
            while (reader.hasNext()) {
                resultList.add(readParkingSpot(reader));
            }
            reader.endArray();
            return resultList;
        }

        private IHandicapParking readParkingSpot(JsonReader reader) throws IOException{
            String id = null;
            String name = null;
            String maxParkingTime = null;
            double lat = -1;
            double lng = -1;
            long parkingCount = -1;

            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals(Constants.KEY_ID)) {
                    id = reader.nextString();
                } else if (key.equals(Constants.KEY_NAME)) {
                    name = reader.nextString();
                } else if (key.equals(Constants.KEY_MAX_TIME)) {
                    maxParkingTime = reader.nextString();
                } else if (key.equals(Constants.KEY_LAT)) {
                    lat = reader.nextDouble();
                } else if (key.equals(Constants.KEY_LONG)) {
                    lng = reader.nextDouble();
                } else if (key.equals(Constants.KEY_PARKING_SPACES)) {
                    parkingCount = reader.nextLong();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return new HandicapParking(id, name, lat, lng, parkingCount, maxParkingTime);
        }

        private List<IHandicapParking> getParkingSpots() throws IOException {

            URL url = new URL(Constants.HANDICAP_API_URL + Constants.GBG_API_KEY + "?format=JSON");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000); /* milliseconds */
            conn.setConnectTimeout(10000); /* milliseconds */
            conn.setRequestMethod("GET");
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();

            // HTTP response 200 = OK
            if (response == 200) {
                return readJson(conn.getInputStream());
            } else {
                throw new IOException();
            }
        }

        @Override
        protected void onPostExecute(List<IHandicapParking> iHandicapParkings) {
            for (IHandicapParking parking : iHandicapParkings) {
                mMap.addMarker(new MarkerOptions()
                        .title(parking.getName())
                        .snippet(parking.getMaxParkingTime() + " | " +parking.getTotalParkingCount())
                        .position(new LatLng(parking.getLatitude(), parking.getLongitude())));
            }
        }
    }
}
