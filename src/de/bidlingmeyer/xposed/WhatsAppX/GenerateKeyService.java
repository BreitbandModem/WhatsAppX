package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class GenerateKeyService extends IntentService{

	public GenerateKeyService() {
		super("HideNotificationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences pref = getSharedPreferences("preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		
		edit.putString("myPublicKey", "testKey");
		edit.commit();
		Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/preferences.xml", true);
	}

}
