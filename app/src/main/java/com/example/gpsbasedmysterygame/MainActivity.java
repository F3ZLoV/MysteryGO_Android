package com.example.gpsbasedmysterygame;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient locationClient;
    private static final int PERMISSION_REQ_CODE = 100;
    private List<Marker> mysteryMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // 지도 커스텀 스타일 적용 (res/raw/map_style.json)
        // https://mapstyle.withgoogle.com/
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) { }

        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                moveToLocation(location);
                addRandomMysteryMarkers(location);
            }
        });
    }

    private void moveToLocation(Location location) {
        LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 18f));
    }

    private void addRandomMysteryMarkers(Location myLocation) {
        // 기존 마커 삭제
        for (Marker m : mysteryMarkers) m.remove();
        mysteryMarkers.clear();
        // 5개 랜덤 생성 예시
        for (int i = 0; i < 5; i++) {
            LatLng randomLatLng = getRandomNearbyLocation(myLocation, 30); // 30m 반경
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(randomLatLng)
                    .title("미스터리 퀴즈!"));
            mysteryMarkers.add(marker);
        }
        mMap.setOnMarkerClickListener(marker -> {
            // 퀴즈 UI 띄우기 등 처리
            // 예시
            new android.app.AlertDialog.Builder(this)
                    .setTitle("퀴즈 풀기")
                    .setMessage("이곳의 퀴즈를 풀어보세요!")
                    .setPositiveButton("확인", null)
                    .show();
            return true;
        });
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

}
