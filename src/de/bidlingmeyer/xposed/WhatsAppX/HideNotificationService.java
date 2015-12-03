package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class HideNotificationService extends IntentService{

	public HideNotificationService() {
		super("HideNotificationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences pref = getSharedPreferences("hideNotification", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		String contact = intent.getStringExtra("contact");
		
		boolean hidden = pref.getBoolean(contact, false);
		
		if(hidden){
			edit.remove(contact);
		}else{
			edit.putBoolean(contact, true);
		}
		
		edit.commit();
		Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/hideNotification.xml", true);
	}

}
