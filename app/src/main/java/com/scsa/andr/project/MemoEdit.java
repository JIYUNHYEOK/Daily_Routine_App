package com.scsa.andr.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.scsa.andr.project.databinding.ActivityMemoEditBinding;
import com.scsa.andr.project.util.CheckPermission;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

@SuppressLint("MissingPermission")
public class MemoEdit extends AppCompatActivity {

    private static final String TAG = "MemoEdit_SCSA";

    private EditText dateEditText;
    private SimpleDateFormat dateFormatter;
    private Calendar selectedDate;

    private BeaconManager beaconManager;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private CheckPermission checkPermission;

    private static final int PERMISSION_REQUEST_CODE = 18;
    private String[] runtimePermissions;

    private static final String BEACON_UUID = "fda50693-a4e2-4fb1-afcf-c6eb07647825";
    private static final String BEACON_MAJOR = "10004";
    private static final String BEACON_MINOR = "54480";

    // Beacon의 Region 설정
    private Region region = new Region("estimote",
            Arrays.asList(Identifier.parse(BEACON_UUID), Identifier.parse(BEACON_MAJOR), Identifier.parse(BEACON_MINOR)),
            "F0:F8:F2:04:4D:51"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMemoEditBinding binding = ActivityMemoEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dateEditText = binding.date;
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Initialize selectedDate to the current date
        selectedDate = Calendar.getInstance();

        // Show the current date in the dateEditText
        updateDateEditText();

        // Set OnClickListener for the dateEditText to open the DatePickerDialog
//        dateEditText.setOnClickListener(view -> {
//            Log.d(TAG, "showDatePickerDialog: ");
//            showDatePickerDialog();
//        });

        // 31 이상
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            runtimePermissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            runtimePermissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        // BeaconManager 지정
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // Beacon Parser 설정
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        checkPermission = new CheckPermission(this);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "블루투스 기능을 확인해 주세요.", Toast.LENGTH_SHORT).show();

            Intent bleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bleIntent, 1);
        }

        if (!checkPermission.runtimeCheckPermission(this, runtimePermissions)) {
            ActivityCompat.requestPermissions(this, runtimePermissions, PERMISSION_REQUEST_CODE);
        } else { // 이미 전체 권한이 있는 경우
            initView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermission.hasPermission(runtimePermissions, grantResults)) {
                initView();
            } else {
                checkPermission.requestPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 5초 후에 Beacon 감지 시작
        handler.postDelayed(() -> {
            // detecting되는 해당 region의 beacon 정보를 받는 클래스 지정.
            beaconManager.addRangeNotifier(rangeNotifier);
            beaconManager.startRangingBeacons(region);
        }, 5_000);
    }

    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.removeRangeNotifier(rangeNotifier);
    }

    public void showDatePickerDialog(View v) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    selectedDate.set(year, monthOfYear, dayOfMonth);
                    updateDateEditText();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void updateDateEditText() {
        dateEditText.setText(dateFormatter.format(selectedDate.getTime()));
    }

    private void initView() {
        ActivityMemoEditBinding binding = ActivityMemoEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Save button action
        binding.button.setOnClickListener(view -> saveMemo());

        // Delete button action
        binding.deleteButton.setOnClickListener(view -> deleteMemo());
    }

    private static final double BEACON_DISTANCE = 0.5;

    // 매초마다 해당 리전의 beacon 정보들을 collection으로 제공받아 처리한다.
    RangeNotifier rangeNotifier = (beacons, region) -> {
        if (beacons.size() > 0) {
            Iterator iterator = beacons.iterator();
            while (iterator.hasNext()) {
                Beacon beacon = (Beacon) iterator.next();
                String msg = "distance: " + beacon.getDistance();
                // 사정거리 내에 있을 경우 이벤트 표시 다이얼로그 팝업
                if (beacon.getDistance() <= BEACON_DISTANCE) {
                    Log.d(TAG, "didRangeBeaconsInRegion: distance 이내.");
                    Toast.makeText(this, "Beacon 500m 이내 접근, 자동저장", Toast.LENGTH_SHORT).show();
                    saveMemo();


                } else {
                    Log.d(TAG, "didRangeBeaconsInRegion: distance 이외.");
                }
                Log.d(TAG, "distance: " + beacon.getDistance() + " id:" + beacon.getId1() + "/" + beacon.getId2() + "/" + beacon.getId3());
            }
        }

        if (beacons.size() > 0) {
            beaconManager.stopRangingBeacons(region);
        }
    };

    private void saveMemo() {
        String title = ((EditText) findViewById(R.id.title)).getText().toString();
        String content = ((EditText) findViewById(R.id.content)).getText().toString();
        String date = dateEditText.getText().toString();

        MemoDto dto = new MemoDto(title, content, date, false);

        Intent intent = new Intent();
        intent.putExtra("result", dto);

        setResult(RESULT_OK, intent);
        finish();
        Toast.makeText(this, "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void deleteMemo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this memo?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Delete the memo
                        deleteMemoAndReturnResult();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMemoAndReturnResult() {
        // Create an intent to return the position to the calling activity
        Intent intent = new Intent();
        intent.putExtra("position", -1); // Set the position of the newly created memo to -1
        setResult(100, intent);
        finish();
        Toast.makeText(this, "메모가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
