package com.example.gpsbasedmysterygame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.util.List;

public class QuizActivity extends AppCompatActivity {
    private TextView txtQuestion;
    private EditText edtAnswer;
    private Button btnSubmit;
    private Mystery currentQuiz;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "GPSMysteryPrefs";
    private static final String KEY_SCORE = "user_score";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        androidx.appcompat.widget.Toolbar toolbar =
                findViewById(R.id.toolbar_quiz);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        txtQuestion = findViewById(R.id.txtQuestion);
        edtAnswer   = findViewById(R.id.edtAnswer);
        btnSubmit   = findViewById(R.id.btnSubmit);
        prefs       = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int quizId = getIntent().getIntExtra("quiz_id", -1);
        currentQuiz = findQuizById(quizId);
        if (currentQuiz == null) finish();  // 없으면 종료

        txtQuestion.setText(currentQuiz.getTitle() + "\n\n" + currentQuiz.getDescription());

        btnSubmit.setOnClickListener(v -> {
            String userAns = edtAnswer.getText().toString().trim();
            if (userAns.equalsIgnoreCase(currentQuiz.getAnswer())) {
                // 정답! 점수 +10
                int oldScore = prefs.getInt(KEY_SCORE, 0);
                prefs.edit().putInt(KEY_SCORE, oldScore + 10).apply();
                Toast.makeText(this, "정답! 점수 +10 (총 " + (oldScore+10) + " 점)", Toast.LENGTH_LONG).show();
                finish();  // 이전 화면으로 복귀
            } else {
                // 오답
                Toast.makeText(this, "오답입니다! 다시 시도해 보세요.", Toast.LENGTH_SHORT).show();
                edtAnswer.setText("");
            }
        });
    }

    // assets/mysteries.json 에서 퀴즈 리스트 불러와 ID 매칭
    private Mystery findQuizById(int id) {
        try {
            InputStreamReader reader = new InputStreamReader(getAssets().open("mysteries.json"));
            List<Mystery> list = new Gson().fromJson(reader, new TypeToken<List<Mystery>>(){}.getType());
            for (Mystery m : list) {
                if (m.getId() == id) return m;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
