package se.creotec.ctkhandicapmap.controller;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import se.creotec.ctkhandicapmap.Constants;
import se.creotec.ctkhandicapmap.R;
import se.creotec.ctkhandicapmap.model.HandicapParking;
import se.creotec.ctkhandicapmap.model.IHandicapParking;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng ctkOffice = new LatLng(57.690292, 11.972511);

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

    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        // Move camera to show CTK's office
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ctkOffice, 14));

        if (isConnected()) {
            new DownloadParkingDataAsync().execute();
        }
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

        private List<IHandicapParking> parseJSON(InputStream is) throws IOException{
            List<IHandicapParking> resultList = new ArrayList<>();
            JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            reader.setLenient(true);

            reader.beginArray();
            while (reader.hasNext()) {
                resultList.add(parseParkingSpot(reader));
            }
            reader.endArray();
            return resultList;
        }

        private IHandicapParking parseParkingSpot(JsonReader reader) throws IOException{
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
            String apiURL = Constants.HANDICAP_API_URL.replace(Constants.API_KEY_PLACEHOLDER, Constants.GBG_API_KEY);
            URL url = new URL(apiURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(Constants.CONNECTION_TIMEOUT); /* milliseconds */
            conn.setConnectTimeout(Constants.CONNECTION_TIMEOUT); /* milliseconds */
            conn.setRequestMethod("GET");
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            // HTTP response 200 = OK
            if (response == 200) {
                return parseJSON(conn.getInputStream());
            } else {
                throw new IOException();
            }
        }

        @Override
        protected void onPostExecute(List<IHandicapParking> iHandicapParkings) {
            mMap.clear();
            for (IHandicapParking parking : iHandicapParkings) {
                mMap.addMarker(new MarkerOptions()
                        .title(parking.getName())
                        .snippet(getString(R.string.parking_count) + " " + parking.getTotalParkingCount() + ", "
                                + getString(R.string.max_parking) + " " + parking.getMaxParkingTime())
                        .position(new LatLng(parking.getLatitude(), parking.getLongitude())));
            }
        }
    }
}
