package com.example.gpsbasedmysterygame;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.List;

public class MysteryAdapter extends ArrayAdapter<Mystery> {
    public MysteryAdapter(Context context, List<Mystery> list) {
        super(context, 0, list);
    }

    @Override
    public View getView(int pos, View v, ViewGroup parent) {
        Mystery item = getItem(pos);
        if (v == null)
            v = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);

        TextView t1 = v.findViewById(android.R.id.text1);
        TextView t2 = v.findViewById(android.R.id.text2);

        t1.setText(item.title + (item.solved ? " (해결됨)" : ""));
        t2.setText(item.description);

        v.setOnClickListener(view -> {
            if (item.solved) {
                Toast.makeText(getContext(), "이미 해결한 미스터리입니다!", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("정답 입력");
            EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("확인", (dialog, which) -> {
                String userAns = input.getText().toString().trim();
                if (userAns.equalsIgnoreCase(item.answer)) {
                    item.solved = true;
                    notifyDataSetChanged();
                    Toast.makeText(getContext(), "정답! 포인트 +10", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "오답입니다!", Toast.LENGTH_SHORT).show();
                    // **오답일 때만** 마커 재배치
                    if (getContext() instanceof MainActivity) {
                        ((MainActivity) getContext()).refreshMarkersOnFailure();
                    }
                }
            });
            builder.setNegativeButton("취소", null);
            builder.show();
        });
        return v;
    }
}
