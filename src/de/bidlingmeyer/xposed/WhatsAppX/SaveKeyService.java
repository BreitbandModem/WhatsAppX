package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SaveKeyService extends IntentService{

	public SaveKeyService() {
		super("HideNotificationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences pref = getSharedPreferences("encryptionKeys", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		
		edit.putString(intent.getStringExtra("jid"), intent.getStringExtra("key"));
		edit.commit();
		Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/encryptionKeys.xml", true);
	}

}
