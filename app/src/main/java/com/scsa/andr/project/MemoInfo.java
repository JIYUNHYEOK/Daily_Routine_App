package com.scsa.andr.project;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.scsa.andr.project.databinding.ActivityMemoInfoBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MemoInfo extends AppCompatActivity {
    private static final String TAG = "MemoInfo_SCSA";

    private EditText dateEditText;
    private SimpleDateFormat dateFormatter;
    private Calendar selectedDate;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        position = intent.getIntExtra("position", -1);
        MemoDto dto = (MemoDto) intent.getSerializableExtra("item");

        ActivityMemoInfoBinding binding = ActivityMemoInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.title.setText(dto.getTitle());
        binding.content.setText(dto.getContents());

        dateEditText = binding.date;
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Parse the date from the MemoDto and set it in selectedDate
        selectedDate = Calendar.getInstance();
        try {
            selectedDate.setTime(dateFormatter.parse(dto.getDate()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the formatted date in the EditText
        updateDateEditText();

        // Set OnClickListener for the date EditText to show the DatePickerDialog
        dateEditText.setOnClickListener(view -> showDatePickerDialog());

        binding.button.setOnClickListener(view -> {
            MemoDto dto_modify = new MemoDto(
                    binding.title.getText().toString(),
                    binding.content.getText().toString(),
                    dateFormatter.format(selectedDate.getTime()), false);

            Intent intent_modify = new Intent();
            intent_modify.putExtra("result", dto_modify);
            intent_modify.putExtra("position", position);

            setResult(RESULT_OK, intent_modify);
            finish();
        });

        // 삭제 버튼 클릭 이벤트
        binding.deleteButton.setOnClickListener(view -> deleteMemo());
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    selectedDate.set(year, monthOfYear, dayOfMonth);
                    updateDateEditText();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.setOnDismissListener(dialog -> updateDateEditText());  // 추가된 부분

        datePickerDialog.show();
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
        // Return the position to the calling activity
        Intent intent = new Intent();
        intent.putExtra("position", position);
        setResult(100, intent);
        finish();
    }

    private void updateDateEditText() {
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // 추가된 부분
        dateEditText.setText(dateFormatter.format(selectedDate.getTime()));
    }

}
