package de.bidlingmeyer.xposed.WhatsAppX;

import java.util.Map;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class NotificationService extends IntentService {
	
	NotificationManager myNotificationManager;

	public NotificationService() {
		super("sdg");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String contact = intent.getStringExtra("contact");
		String jid = intent.getStringExtra("jid");
		
		SharedPreferences pref = getSharedPreferences("notification", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		
		myNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(jid == null){//restore all notifications
	   		 Map<String, ?> allEntries = pref.getAll();
	   		 for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
	   			 //String cont = entry.getKey();
	   		     //SharedPreferences pref2 = getSharedPreferences("contacts", Context.MODE_PRIVATE);
	   			 String jid2 = entry.getKey();
	   			 not(contact, jid2, getId(jid2), intent);
	   		 }
		}else{
			int id = getId(jid);
			if(id == pref.getInt(jid, 0)){//cancel notification
				myNotificationManager.cancel(id);
				edit.remove(jid);
			}else{//create notification
				not(contact, jid, id, intent);
				edit.putInt(jid, id);
			}
			edit.commit();
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/notification.xml", true);
		}
	}
	
	public void not(String contact, String jid, int id, Intent intent){
		if(contact == null){
			SharedPreferences pref2 = getSharedPreferences("contactsJid", Context.MODE_PRIVATE);
			contact = pref2.getString(jid, "");
		}
		if(jid.contains("@g.us"))
    		jid = "";
		Intent resultIntent;
		if(jid.length() > 0){
			Uri uri = Uri.parse("smsto:"+jid);
	        resultIntent = new Intent(Intent.ACTION_SENDTO, uri);
	        resultIntent.setPackage("com.whatsapp");
			resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
		}else{
			resultIntent = new Intent("android.intent.action.MAIN");
			resultIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Main"));
			resultIntent.addCategory("android.intent.category.LAUNCHER");
		}
    	
        NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(contact);
        mBuilder.setContentText("Write back soon!");
        mBuilder.setTicker("reminder set");
        mBuilder.setSmallIcon(R.drawable.ic_reminder_notification);
        mBuilder.setOngoing(true);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
	     // Adds the back stack for the Intent (but not the Intent itself)
	     //stackBuilder.addParentStack(ResultActivity.class);
	     // Adds the Intent that starts the Activity to the top of the stack
	     stackBuilder.addNextIntent(resultIntent);
	     PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
	     mBuilder.setContentIntent(resultPendingIntent);
        
        myNotificationManager.notify(id, mBuilder.build());
	}
	public int getId(String jid){
		int id;
		jid = jid.split("@")[0];
		if(jid.contains("-")){
			id = Integer.parseInt(jid.split("-")[1].substring(5));
		}else{
			id = Integer.parseInt(jid.substring(5));
		}
		return id;
	}
}
