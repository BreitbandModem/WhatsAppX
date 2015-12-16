package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class HidePreviewService extends IntentService {

	public HidePreviewService() {
		super("name");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences pref = getSharedPreferences("preview", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		String contact = intent.getStringExtra("contact");
		boolean isGroup = intent.getBooleanExtra("isGroup", false);
		
		if(pref.getInt(contact, 0) >= 1){
			edit.remove(contact);
		}else if(isGroup){
			edit.putInt(contact, 2);
		}else{
			edit.putInt(contact, 1);
		}
		edit.commit();
		Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/preview.xml", true, false);
	}

}
