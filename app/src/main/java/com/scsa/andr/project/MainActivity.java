package com.scsa.andr.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonTodo = findViewById(R.id.buttonTodo);
        Button buttonNews = findViewById(R.id.buttonNews);
        Button buttonGame = findViewById(R.id.buttonGame);

        buttonTodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO List 페이지로 이동하는 코드를 추가합니다.
                Intent todoIntent = new Intent(MainActivity.this, TodoActivity.class);
                startActivity(todoIntent);
            }
        });

        buttonNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 뉴스보기 페이지로 이동하는 코드를 추가합니다.
                Intent newsIntent = new Intent(MainActivity.this, NewsActivity.class);
                startActivity(newsIntent);
            }
        });

        buttonGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 쥐잡기 게임 페이지로 이동하는 코드를 추가합니다.
                Intent gameIntent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(gameIntent);
            }
        });
    }
}
