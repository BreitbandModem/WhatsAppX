package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class InstallSqliteService extends IntentService{

	public InstallSqliteService() {
		super("HideNotificationService");
	}

	@Override
	protected void onHandleIntent(Intent i) {
		SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = prefs.edit();

		if(prefs.getBoolean("sqlite3", false)) {
			return;
		}

		//install sqlite
		int androidVersion = android.os.Build.VERSION.SDK_INT;
		String abi = Build.CPU_ABI.toLowerCase();
		if (abi.contains("arm")) {
			if (androidVersion >= 21) {// sqlite arm pie
				copyFile("sqlite3.armeabi.pie");
			} else {// sqlite arm
				copyFile("sqlite3.armeabi");
			}
		} else if (abi.contains("x86")) {
			if (androidVersion >= 21) {// sqlite x86 pie
				copyFile("sqlite3.x86.pie");
			} else {// sqlite x86
				copyFile("sqlite3.x86");
			}
		} else if (abi.contains("mips")) {
			if (androidVersion >= 21) {// sqlite mips pie
				copyFile("sqlite3.mips.pie");
			} else {// sqlite mips
				copyFile("sqlite3.mips");
			}
		}

		Helper.sqlite3prefix = "data/data/de.bidlingmeyer.xposed.WhatsAppX/sqlite/sqlite3";
		if(Helper.shell("/data/data/com.whatsapp/databases/msgstore.db 'Select MAX(_id) FROM messages WHERE key_from_me=0';", true, true).trim().length() < 1){
			Helper.sqlite3prefix = "sqlite3";
			if(Helper.shell("/data/data/com.whatsapp/databases/msgstore.db 'Select MAX(_id) FROM messages WHERE key_from_me=0';", true, true).trim().length() < 1) {
				edit.putBoolean("sqlite3", false);
				//Toast.makeText(getBaseContext(), "data/data/de.bidlingmeyer.xposed.WhatsAppX/sqlite/sqlite3 is not installed! This app will not work!", Toast.LENGTH_LONG).show();
				NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this);
				mBuilder.setContentTitle("WhatsAppX");
				mBuilder.setContentText("Failed to install or find sqlite3 on your device!\nWhatsAppX won't work properly :(");
				mBuilder.setTicker("WhatsAppX error");
				mBuilder.setSmallIcon(android.R.drawable.ic_dialog_alert);
				mBuilder.setOngoing(false);

				NotificationManager myNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				myNotificationManager.notify(1111, mBuilder.build());
			}else{
				edit.putBoolean("sqlite3", true);
			}
		}else{
			edit.putBoolean("sqlite3", true);
		}

		edit.commit();
		Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/preferences.xml", true, false);
	}

	private boolean copyFile(String sourceFileName)
	{
		AssetManager assetManager = getAssets();

		File destFile = new File("/data/data/de.bidlingmeyer.xposed.WhatsAppX/sqlite/sqlite3");

		File destParentDir = destFile.getParentFile();
		destParentDir.mkdir();

		InputStream in = null;
		OutputStream out = null;
		try
		{
			in = assetManager.open(sourceFileName);
			out = new FileOutputStream(destFile);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;

			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

}
