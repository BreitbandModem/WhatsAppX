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
		
		SharedPreferences pref = getSharedPreferences("notification", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		
		myNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(contact == null){//restore all notifications
	   		 Map<String, ?> allEntries = pref.getAll();
	   		 for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
	   			 String cont = entry.getKey();
	   			 not(cont, getId(cont), intent);
	   		 }
		}else{
			int id = getId(contact);
			if(id == pref.getInt(contact, 0)){//cancel notification
				myNotificationManager.cancel(id);
				edit.remove(contact);
			}else{//create notification
				not(contact, id, intent);
				edit.putInt(contact, id);
			}
			edit.commit();
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/notification.xml", true);
		}
	}
	
	public void not(String cont, int id, Intent intent){
		
		SharedPreferences pref2 = getSharedPreferences("contacts", Context.MODE_PRIVATE);
		String jid = pref2.getString(cont, "");
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
        mBuilder.setContentTitle(cont);
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
	
	public int getId(String cont){
		int id = 0;
		for(int i=0; i<cont.length(); i++){
			char c = cont.charAt(i);
			id += c;
		}
		return id;
	}
}
