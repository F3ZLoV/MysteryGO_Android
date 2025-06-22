package com.example.gpsbasedmysterygame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;

public class ProfileActivity extends AppCompatActivity {
    private static final int REQ_PICK_IMAGE = 1;
    public static final String PREFS_NAME = "GPSMysteryPrefs";
    public static final String KEY_SCORE = "user_score";
    private ImageButton imgProfile;
    private TextView txtProfileScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 툴바 뒤로가기
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("내 프로필");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        imgProfile = findViewById(R.id.imgProfile);
        txtProfileScore = findViewById(R.id.txtProfileScore);

        // 프로필 이미지 누르면 갤러리에서 이미지 선택
        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_PICK_IMAGE);
        });

        updateScore();
        loadProfileImage();
    }

    // 점수는 화면 돌아올 때마다 갱신
    @Override
    protected void onResume() {
        super.onResume();
        updateScore();
    }

    private void updateScore() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int score = prefs.getInt(KEY_SCORE, 0);
        txtProfileScore.setText("현재 점수: " + score);
    }

    // 이미지 선택 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                // 내부 저장소에 이미지 저장 후 표시
                try (InputStream in = getContentResolver().openInputStream(imageUri)) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    // 저장
                    File file = new File(getFilesDir(), "profile.jpg");
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    }
                    imgProfile.setImageBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 액티비티 시작시 이미지 불러오기
    private void loadProfileImage() {
        File file = new File(getFilesDir(), "profile.jpg");
        if (file.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            imgProfile.setImageBitmap(bmp);
        }
    }

    // 툴바 뒤로가기(←) 버튼 핸들링
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
