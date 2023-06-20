package com.scsa.andr.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TodoActivity extends AppCompatActivity {

    private static final String TAG = "TodoActivity_SCSA";

    private static final int REQUEST_PERMISSIONS = 100;

    private SMSReceiver receiver;

    private final String [] REQUIRED_PERMISSIONS = new String []{
            "android.permission.RECEIVE_SMS"
    };

    List<MemoDto> list = new ArrayList<>();
    MyAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        // 1. 권한이 있는지 확인.
        int permission = ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[0]);

        // 2. 권한이 없으면 런타임 퍼미션 창 띄우기. 있으면 정상진행.
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
        }

        list.add(new MemoDto("부서회의", "부서회의 입니다.", "2023-06-02", false));
        list.add(new MemoDto("개발미팅", "개발미팅 입니다.", "2023-06-05", false));
        list.add(new MemoDto("소개팅", "소개팅 입니다.", "2023-06-07", false));

        String sms = getIntent().getStringExtra("sms");

        if (sms != null) {
            Log.d(TAG, "onCreate: " + sms);
            list.add(new MemoDto(sms, sms, new Date().toString(), false));
        }

        ListView listView = findViewById(R.id.listView);
        registerForContextMenu(listView);

        adapter = new MyAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {

            Intent intent = new Intent(TodoActivity.this, MemoInfo.class);
            intent.putExtra("position", position);
            intent.putExtra("item", list.get(position));

            startActivityForResult(intent, 2);
        });

    }
    //start of options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivityForResult(new Intent(this, MemoEdit.class), 1);

        return super.onOptionsItemSelected(item);
    }
//end of options menu


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            if (resultCode == RESULT_OK && data != null) {
                MemoDto dto = (MemoDto) data.getSerializableExtra("result");
                Log.d(TAG, "onActivityResult: " + data.getSerializableExtra("result"));
                list.add(dto);
                adapter.notifyDataSetChanged();
            }
        }else if (requestCode == 2){
            if (resultCode == RESULT_OK && data != null) {
                MemoDto dto = (MemoDto) data.getSerializableExtra("result");
                Log.d(TAG, "onActivityResult: " + data.getSerializableExtra("result"));
                int position = data.getIntExtra("position", -1);
                if (position != -1) {
                    list.set(position, dto);
                    adapter.notifyDataSetChanged();
                }
            } else if (resultCode == 100 && data != null) {
                int position = data.getIntExtra("position", -1);
                if (position != -1) {
                    // Delete the memo at the specified position
                    list.remove(position);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }


    //start of context menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0,0,0,"삭제");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
        AdapterView.AdapterContextMenuInfo aptInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Toast.makeText(this, "position:"+aptInfo.position, Toast.LENGTH_SHORT).show();

        list.remove(aptInfo.position);
        adapter.notifyDataSetChanged();

        return super.onContextItemSelected(item);
    }
//end of context menu

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if(view == null){
                LayoutInflater inflater = LayoutInflater.from(TodoActivity.this);
                view = inflater.inflate(R.layout.item, parent, false);
            }

            MemoDto dto = list.get(position);
            CheckBox checkBox = view.findViewById(R.id.checkbox);

            TextView textView = view.findViewById(R.id.title);
            TextView date = view.findViewById(R.id.date);
            textView.setText(dto.getTitle());
            date.setText(dto.getDate());
            checkBox.setChecked(dto.isCompleted());

            textView.setOnClickListener(v -> {
                Intent intent = new Intent(TodoActivity.this, MemoInfo.class);
                intent.putExtra("position", position);
                intent.putExtra("item", list.get(position));
                startActivityForResult(intent, 2);
            });

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                dto.setCompleted(isChecked); // Update the MemoDto's isCompleted field
                adapter.notifyDataSetChanged(); // Notify adapter to update the list view
            });

            return view;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    boolean hasPermission = true;

    // requestPermissions의 call back method
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                showDialog();
            }
        }

    }

    private void showDialog(){
        AlertDialog dialog = new AlertDialog.Builder(TodoActivity.this)
                .setTitle("권한확인")
                .setMessage("서비스를 정상적으로 이용하려면, 권한이 필요합니다. 설정화면으로 이동합니다.")
                .setPositiveButton("예", (dialogInterface, i) -> {
                    //권한설정화면으로 이동.
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("아니오", (dialogInterface, which) -> {
                    Toast.makeText(TodoActivity.this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                })
                .create();
        dialog.show();
    }


    // 2. BroadcastReceiver를 registerReceiver()를 이용하여 등록 후 사용하기
    private void regist() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");

        receiver = new SMSReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        regist();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TodoActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}