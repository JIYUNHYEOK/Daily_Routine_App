package com.scsa.andr.project.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

public class CheckPermission {

	private Context context;

	public CheckPermission(Context context) {
		this.context = context;
	}

	public boolean runtimeCheckPermission(Context context, String... permissions) {
		if (context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean hasPermission(String [] runtimePermissions, int [] grantResults){
		if (runtimePermissions.length == grantResults.length) {
			for (int grantResult : grantResults) {
				if (grantResult == PackageManager.PERMISSION_DENIED) {
					return false;
				}
			}
		}else{
			return false;
		}

		return true;
	}

	public void requestPermission() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		alertDialog.setTitle("권한이 필요합니다.");
		alertDialog.setMessage("설정으로 이동합니다.");
		alertDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) { // 안드로이드 버전에 따라 다를 수 있음.
				Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + context.getPackageName()));
				context.startActivity(intent);
				dialogInterface.cancel();
			}
		});
		alertDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.cancel();
			}
		});
		alertDialog.show();
	}
}
