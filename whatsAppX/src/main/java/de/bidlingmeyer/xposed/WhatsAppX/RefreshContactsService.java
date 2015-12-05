package de.bidlingmeyer.xposed.WhatsAppX;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class RefreshContactsService extends IntentService{

	public RefreshContactsService() {
	      super("RefreshContactsService");
	  }
	  
	  @Override
	  protected void onHandleIntent(Intent intent) {
		  	ArrayList<String> list = Helper.refreshContactList();
		  	//Log.d("WhatsAppXdebug", "list: "+list);
		  	//Log.d("WhatsAppXdebug", "listSize: "+list.size());
			SharedPreferences contactsJid = getSharedPreferences("contactsJid", Context.MODE_PRIVATE);
			SharedPreferences contactsName = getSharedPreferences("contactsName", Context.MODE_PRIVATE);
			SharedPreferences.Editor editName = contactsName.edit();
			SharedPreferences.Editor editJid = contactsJid.edit();
			//Log.d("WhatsAppXdebug", "prefs: "+s+"  edit: "+edit);
			for(String str : list){
				//Log.d("WhatsAppXdebug", "str: "+str);
				String[] split = str.split("[\\x7C]"); // "\n"
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
					editName.putString(name, jid);
				}
				if(jid != null & jid.length() > 1){
					editJid.putString(jid, name);
				}
			}
			editJid.commit();
			editName.commit();
			//Log.d("WhatsAppXdebug", "commited");
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/contactsJid.xml", true);
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/contactName.xml", true);
			//Log.d("WhatsAppXdebug", "over");
			//Toast.makeText(this, "Contacts list updated", Toast.LENGTH_SHORT).show();
	  }	
}
