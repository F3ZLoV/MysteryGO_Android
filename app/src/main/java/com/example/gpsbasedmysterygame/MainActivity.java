package com.example.gpsbasedmysterygame;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.util.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final long LOCATION_UPDATE_INTERVAL = 30_000;
    private static final long LOCATION_FASTEST_INTERVAL = 30_000;
    private GoogleMap mMap;
    private FusedLocationProviderClient locationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int PERMISSION_REQ_CODE = 100;
    private List<Marker> mysteryMarkers = new ArrayList<>();
    private boolean markersInitialized = false;
    private List<Mystery> activeMysteries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // ① 10초마다 위치 요청할 LocationRequest
        locationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_FASTEST_INTERVAL)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        // ② 위치 결과를 받을 LocationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null && mMap != null) {
                    moveToLocation(loc);
                    addRandomMysteryMarkers(loc);
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // ① 마커 클릭 → InfoWindow 띄우기
        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        // ② InfoWindow 클릭 → QuizActivity 로 이동
        mMap.setOnInfoWindowClickListener(marker -> {
            Mystery quiz = (Mystery) marker.getTag();
            if (quiz != null) {
                Intent intent = new Intent(this, QuizActivity.class);
                intent.putExtra("quiz_id", quiz.getId());
                startActivity(intent);
            }
        });

        // 스타일 적용
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception ignored) {}

        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        locationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void moveToLocation(Location location) {
        LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 18f));
    }

    private void addRandomMysteryMarkers(Location myLocation) {
        // 1) 이전 마커 모두 삭제
        for (Marker m : mysteryMarkers) m.remove();
        mysteryMarkers.clear();

        // 2) JSON 불러와 거리 필터링 후 섞기
        List<Mystery> mysteries = loadMysteriesFromAsset(this);
        double lat0 = myLocation.getLatitude(), lng0 = myLocation.getLongitude();
        List<Mystery> nearby = new ArrayList<>();
        for (Mystery m : mysteries) {
            if (distance(lat0, lng0, m.latitude, m.longitude) <= 200) {
                nearby.add(m);
            }
        }
        Collections.shuffle(nearby);
        int showCount = Math.min(5, nearby.size());

        // 3) 마커 추가 → 반드시 setTag(quiz)
        for (int i = 0; i < showCount; i++) {
            Mystery quiz = nearby.get(i);
            Marker mk = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(quiz.latitude, quiz.longitude))
                    .title(quiz.title)
                    .snippet(quiz.description));
            mk.setTag(quiz);              // ← 여기 꼭!
            mysteryMarkers.add(mk);
        }
    }


    private LatLng getRandomNearbyLocation(Location origin, double radiusMeters) {
        Random r = new Random();
        double radiusDegrees = radiusMeters / 111000f;
        double u = r.nextDouble();
        double v = r.nextDouble();
        double w = radiusDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double latOffset = w * Math.cos(t);
        double lngOffset = w * Math.sin(t) / Math.cos(Math.toRadians(origin.getLatitude()));
        double lat = origin.getLatitude() + latOffset;
        double lng = origin.getLongitude() + lngOffset;
        return new LatLng(lat, lng);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // 반드시 호출!
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }

    // mysteries.json 불러오는 함수
    private List<Mystery> loadMysteriesFromAsset(Context context) {
        try {
            InputStreamReader reader = new InputStreamReader(context.getAssets().open("mysteries.json"));
            return new Gson().fromJson(reader, new TypeToken<List<Mystery>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // 두 위치(위도/경도) 사이 거리 계산(m)
    private static double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void initializeMarkers(Location myLocation) {
        // 기존 마커 제거
        for (Marker mk : mysteryMarkers) mk.remove();
        mysteryMarkers.clear();

        // 반경 필터 + 랜덤 추출
        List<Mystery> all = loadMysteriesFromAsset(this);
        List<Mystery> nearby = new ArrayList<>();
        double lat0 = myLocation.getLatitude(), lng0 = myLocation.getLongitude();
        for (Mystery m : all) {
            if (distance(lat0, lng0, m.latitude, m.longitude) <= 100) {
                nearby.add(m);
            }
        }
        Collections.shuffle(nearby);
        activeMysteries = nearby.subList(0, Math.min(5, nearby.size()));

        // (★) 마커 추가 → 반드시 setTag(quiz) 해 주기
        for (Mystery quiz : activeMysteries) {
            Marker mk = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(quiz.latitude, quiz.longitude))
                    .title(quiz.title)
                    .snippet(quiz.description));
            mk.setTag(quiz);                           // ← 여기
            mysteryMarkers.add(mk);
        }
    }

    public void refreshMarkersOnFailure() {
        // 기존 마커 지우기
        for (Marker mk : mysteryMarkers) mk.remove();
        mysteryMarkers.clear();
        // activeMysteries 다시 랜덤 섞어서 찍기
        Collections.shuffle(activeMysteries);
        for (Mystery quiz : activeMysteries) {
            Marker mk = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(quiz.latitude, quiz.longitude))
                    .title(quiz.title)
                    .snippet(quiz.description));
            mysteryMarkers.add(mk);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        locationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationClient.removeLocationUpdates(locationCallback);
    }

}
