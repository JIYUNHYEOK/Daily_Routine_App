package com.scsa.andr.project;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class GameActivity extends AppCompatActivity {

    // NFC 관련 멤버 변수
    private NfcAdapter nfcAdapter;
    private boolean nfcEnabled = false;

    TextView time;
    TextView count;
    Button start;

    ImageView[] img_array = new ImageView[9];
    int[] imageID = {R.id.imageView1, R.id.imageView2, R.id.imageView3, R.id.imageView4, R.id.imageView5, R.id.imageView6, R.id.imageView7, R.id.imageView8, R.id.imageView9};

    final String TAG_ON = "on";
    final String TAG_OFF = "off";
    int score = 0;

    SoundPool soundPool;   // 소리
    int killSound;    // 소리
    MediaPlayer mediaPlayer;   // 소리

    private boolean gameRunning = false;
    private TimeCheck timeCheckThread;
    private MoleThread[] moleThreads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // NFC 어댑터 가져오기
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // NFC가 지원되지 않는 기기이거나 비활성화 상태인 경우
            Toast.makeText(this, "NFC가 지원되지 않거나 비활성화되어 있습니다.", Toast.LENGTH_SHORT).show();
        } else {
            nfcEnabled = true;
        }

        time = findViewById(R.id.time);
        count = findViewById(R.id.game_count);
        start = findViewById(R.id.start);

        for (int i = 0; i < img_array.length; i++) {
            img_array[i] = findViewById(imageID[i]);
            img_array[i].setImageResource(R.drawable.mole_down);
            img_array[i].setTag(TAG_OFF);

            img_array[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameRunning && ((ImageView) v).getTag().toString().equals(TAG_ON)) {
                        Toast.makeText(getApplicationContext(), "Good!", Toast.LENGTH_SHORT).show();
                        count.setText(String.valueOf(score += 30));
                        ((ImageView) v).setImageResource(R.drawable.mole_down);
                        v.setTag(TAG_OFF);
                        playSoundEffect("mole_scream.mp3");
                    } else if (gameRunning && ((ImageView) v).getTag().toString().equals(TAG_OFF)) {
                        Toast.makeText(getApplicationContext(), "Bomb!", Toast.LENGTH_SHORT).show();
                        count.setText(String.valueOf(score -= 10));
                        ((ImageView) v).setImageResource(R.drawable.bomb);
                        v.setTag(TAG_OFF);
                        playSoundEffect("bomb.wav");
                    } else {
                        Toast.makeText(getApplicationContext(), "Miss!", Toast.LENGTH_SHORT).show();
                        if (score <= 0) {
                            score = 0;
                            count.setText(String.valueOf(score));
                        } else {
                            count.setText(String.valueOf(score -= 10));
                        }
                        playSoundEffect("miss.mp3");
                    }
                }
            });
        }

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        killSound = soundPool.load(this, R.raw.mole_scream, 1);
        mediaPlayer = MediaPlayer.create(this, R.raw.bgm);
        mediaPlayer.setLooping(true);
    }

    private void enableNfcForegroundDispatch() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE); // FLAG_IMMUTABLE 추가

        // NFC 태그를 발견했을 때 호출될 Intent를 설정합니다.
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }


    private void disableNfcForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcEnabled)
            enableNfcForegroundDispatch(); // Foreground Dispatch를 다시 활성화합니다.
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcEnabled)
            disableNfcForegroundDispatch(); // Foreground Dispatch를 비활성화합니다.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
        soundPool.release();
    }

    private void startGame() {
        if (gameRunning)
            return;

        gameRunning = true;
        score = 0;
        count.setText("0");

        timeCheckThread = new TimeCheck();
        timeCheckThread.start();

        moleThreads = new MoleThread[9];
        for (int i = 0; i < 9; i++) {
            moleThreads[i] = new MoleThread(i);
            moleThreads[i].start();
        }

        start.setEnabled(false);
        playBackgroundMusic();
    }

    private class TimeCheck extends Thread {
        private int seconds = 10;

        @Override
        public void run() {
            while (seconds >= 0) {
                handler.sendEmptyMessage(seconds);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                seconds--;
            }
            handler.sendEmptyMessage(-1);
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int seconds = msg.what;
            if (seconds >= 0)
                time.setText("Time: " + seconds);
            else {
                time.setText("Game Over");
                stopBackgroundMusic();
                gameRunning = false;
                start.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Game Over", Toast.LENGTH_SHORT).show();
                for (int i = 0; i < 9; i++) {
                    img_array[i].setTag(TAG_OFF);
                    img_array[i].setImageResource(R.drawable.mole_down);
                }
                // 결과를 ResultActivity로 전달
                Intent intent = new Intent(GameActivity.this, ResultActivity.class);
                intent.putExtra("score", score);
                startActivity(intent);
                return false;
            }
            return true;
        }
    });

    private class MoleThread extends Thread {
        private int index;
        private boolean running = true;

        public MoleThread(int index) {
            this.index = index;
        }

        public void stopRunning() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(getRandomTime());
                    if (!gameRunning || !running)
                        break;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (gameRunning) {
                                img_array[index].setImageResource(R.drawable.mole_up);
                                img_array[index].setTag(TAG_ON);
                            }
                        }
                    });
                    Thread.sleep(500);
                    if (!gameRunning || !running)
                        break;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (gameRunning) {
                                img_array[index].setImageResource(R.drawable.mole_down);
                                img_array[index].setTag(TAG_OFF);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private int getRandomTime() {
            Random random = new Random();
            int min = 500;
            int max = 3000;
            return random.nextInt(max - min + 1) + min;
        }
    }

    private void playSoundEffect(String fileName) {
        soundPool.play(killSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    private void playBackgroundMusic() {
        if (!mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

    private void stopBackgroundMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
        for (MoleThread moleThread : moleThreads) {
            moleThread.stopRunning();
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // NFC 태그를 감지하면 게임 시작
        startGame();
        Toast.makeText(this, "NFC 태그되어 게임 자동시작", Toast.LENGTH_SHORT).show();
    }
}
