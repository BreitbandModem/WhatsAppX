package de.bidlingmeyer.xposed.WhatsAppX;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class RefreshContactsService extends IntentService{

	public RefreshContactsService() {
	      super("RefreshContactsService");
	  }
	  
	  @Override
	  protected void onHandleIntent(Intent intent) {
		  	ArrayList<String> list = Helper.refreshContactList();
		  	//Log.d("WhatsAppXdebug", "list: "+list);
		  	//Log.d("WhatsAppXdebug", "listSize: "+list.size());
			SharedPreferences s = getSharedPreferences("contacts", Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = s.edit();
			//Log.d("WhatsAppXdebug", "prefs: "+s+"  edit: "+edit);
			for(String str : list){
				//Log.d("WhatsAppXdebug", "str: "+str);
				String[] split = str.split("[\\x7C]");
				//Log.d("WhatsAppXdebug", "split: "+split);
				//Log.d("WhatsAppXdebug", "splitSize: "+split.length);
				if(split.length != 2)
					continue;
				String name = split[0].trim();
				//Log.d("WhatsAppXdebug", "name: "+name);
				String jid = split[1].trim();
				//Log.d("WhatsAppXdebug", "jid: "+jid);
				/*Set<String> set = new HashSet<String>();
				set.add(jid);
				set.add(id);*/
				if(name != null & name.length() > 1){
					//Log.d("WhatsAppXdebug", "put");
					edit.putString(name, jid);
				}
			}
			edit.commit();
			//Log.d("WhatsAppXdebug", "commited");
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/contacts.xml", true);
			//Log.d("WhatsAppXdebug", "over");
			//Toast.makeText(this, "Contacts list updated", Toast.LENGTH_SHORT).show();
	  }	
}
