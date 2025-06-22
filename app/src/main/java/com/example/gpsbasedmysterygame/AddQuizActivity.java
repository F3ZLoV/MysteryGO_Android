package com.example.gpsbasedmysterygame;

import android.Manifest; // 권한 추가
import android.content.Intent;
import android.content.pm.PackageManager; // 권한 추가
import android.location.Location; // 위치 추가
import android.os.Bundle;
import android.os.Looper;
import android.util.Log; // 로그 추가
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // NonNull 추가
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat; // 권한 추가
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient; // 위치 추가
import com.google.android.gms.location.LocationCallback; // 위치 추가
import com.google.android.gms.location.LocationRequest; // 위치 추가
import com.google.android.gms.location.LocationResult; // 위치 추가
import com.google.android.gms.location.LocationServices; // 위치 추가
import com.google.android.gms.location.Priority; // 위치 추가
import com.google.firebase.database.DatabaseReference; // Firebase 추가
import com.google.firebase.database.FirebaseDatabase; // Firebase 추가

import java.util.HashMap; // 데이터 구조 추가
import java.util.Map; // 데이터 구조 추가

public class AddQuizActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AddQuizActivity"; // 로그 태그

    Button btnSubmit;
    EditText edtQuestion;
    EditText edtAnswer;
    EditText edtExplain;

    // Firebase
    private DatabaseReference mDatabase;

    // 위치 서비스
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location currentLocation; // 현재 위치를 저장할 변수

    // 권한 요청 런처
    private ActivityResultLauncher<String[]> locationPermissionRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_quiz);

        btnSubmit = findViewById(R.id.btnSubmit);
        edtQuestion =  findViewById(R.id.edtQuestionContent);
        edtAnswer =  findViewById(R.id.edtAnswer);
        edtExplain = findViewById(R.id.edtExplain);

        btnSubmit.setOnClickListener(this);

        // Firebase Database 인스턴스 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 위치 서비스 클라이언트 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 위치 권한 요청 런처 초기화
        locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        // 정확한 위치 권한 허용됨
                        Log.d(TAG, "ACCESS_FINE_LOCATION permission granted.");
                        startLocationUpdates(); // 권한 허용 시 위치 업데이트 시작
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // 대략적인 위치 권한만 허용됨
                        Log.d(TAG, "ACCESS_COARSE_LOCATION permission granted.");
                        startLocationUpdates(); // 권한 허용 시 위치 업데이트 시작
                    } else {
                        // 권한 거부됨
                        Log.d(TAG, "Location permission denied.");
                        Toast.makeText(this, "퀴즈를 등록하려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                    }
                });

        // 위치 요청 설정
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10초 간격
                .setMinUpdateIntervalMillis(5000) // 최소 5초 간격
                .build();

        // 위치 콜백 정의
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location; // 최신 위치 정보 업데이트
                        Log.d(TAG, "Location Updated: " + location.getLatitude() + ", " + location.getLongitude());
                        // 필요하다면 위치 업데이트 중단 (한 번만 가져오려는 경우)
                        // stopLocationUpdates();
                    }
                }
            }
        };

        // 앱 시작 시 권한 확인 및 위치 업데이트 시작
        checkAndRequestLocationPermissions();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkAndRequestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            // 권한이 이미 있으면 위치 업데이트 시작
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 이중 확인: 권한이 없으면 시작하지 않음
            Toast.makeText(this, "위치 권한이 없어 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Requesting location updates...");
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "Stopping location updates.");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSubmit) {
            String question = edtQuestion.getText().toString().trim();
            String answer = edtAnswer.getText().toString().trim();
            String explain = edtExplain.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(this, "퀴즈를 내지 않으셨어요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (answer.isEmpty()) {
                Toast.makeText(this, "정답을 입력하지 않으셨어요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (explain.isEmpty()) {
                Toast.makeText(this, "설명을 적어주지 않으셨어요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 현재 위치 정보 확인
            if (currentLocation == null) {
                Toast.makeText(this, "현재 위치를 가져오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestLocationPermissions(); // 권한이 없다면 다시 요청
                }
                return;
            }

            // 모든 조건 통과 및 위치 정보 있음 -> DB에 업로드
            uploadQuizToDatabase(question, answer, explain, currentLocation);
        }
    }

    private void uploadQuizToDatabase(String question, String answer, String explanation, Location location) {
        // DB의 'quizzes' 경로 아래에 새로운 퀴즈 ID 생성
        String quizId = mDatabase.child("quizzes").push().getKey();

        if (quizId == null) {
            Log.e(TAG, "Failed to create new quiz ID.");
            Toast.makeText(this, "퀴즈 ID 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 저장할 데이터 Map 생성
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("question", question);
        quizData.put("answer", answer);
        quizData.put("explanation", explanation);
        quizData.put("latitude", location.getLatitude());
        quizData.put("longitude", location.getLongitude());
        quizData.put("timestamp", System.currentTimeMillis()); // 현재 시간 (선택 사항)
        // 만약 Mystery 클래스와 같은 데이터 모델을 사용한다면, 해당 객체를 직접 넘길 수도 있습니다.
        // Mystery newMystery = new Mystery(quizId, question, explanation, answer, location.getLatitude(), location.getLongitude(), false, System.currentTimeMillis());
        // mDatabase.child("quizzes").child(quizId).setValue(newMystery)

        Log.d(TAG, "Uploading quiz data: " + quizData.toString());

        // Firebase Realtime Database에 데이터 쓰기
        mDatabase.child("quizzes").child(quizId).setValue(quizData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Quiz uploaded successfully!");
                    Toast.makeText(AddQuizActivity.this, "퀴즈가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show();
                    // 성공 후 액티비티 종료 또는 다른 화면으로 이동 등
                    finish(); // 예: 현재 액티비티 종료
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload quiz.", e);
                    Toast.makeText(AddQuizActivity.this, "퀴즈 등록에 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 액티비티가 다시 활성화될 때 위치 업데이트 재개 (필요한 경우)
        // 현재는 onCreate에서 권한 확인 후 바로 시작하므로, 여기서는 생략 가능.
        // 만약 onPause에서 stopLocationUpdates를 하고, 항상 최신 위치를 원한다면 여기서 다시 시작.
        // if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        //     ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        //     startLocationUpdates();
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 액티비티가 비활성화될 때 배터리 절약을 위해 위치 업데이트 중단
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 확실히 위치 업데이트 중단
        stopLocationUpdates();
    }
}