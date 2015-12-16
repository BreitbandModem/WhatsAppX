package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class LockContactService extends IntentService {

	  public LockContactService() {
	      super("HelloIntentService");
	  }
	  
	  @Override
	  protected void onHandleIntent(Intent intent) {
		  	String contact = intent.getStringExtra("contact");
		  	if(contact.length() < 1)
		  		return;
			boolean lock = intent.getBooleanExtra("lock", true);
			SharedPreferences pref = getSharedPreferences("locked", Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = pref.edit();
			if(lock){
				edit.putLong(contact, 0);
			}else{
				edit.remove(contact);
				edit.remove("pattern."+contact);
			}		
			edit.commit();
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/locked.xml", true, false);
	  }

}
