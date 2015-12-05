package de.bidlingmeyer.xposed.WhatsAppX;

import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

public class PinActivity extends Activity{
	
	private String contact;
	boolean set = false, fromConversations;
	private static final int REQ_CREATE_PATTERN = 1;
	private static final int REQ_ENTER_PATTERN = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pin_layout);
		this.setFinishOnTouchOutside(false);
		Intent i = getIntent();
		contact = i.getStringExtra("contact");
		fromConversations = i.getBooleanExtra("fromConversations", true);
		set = i.getBooleanExtra("set", false);
		
		SharedPreferences pref = getSharedPreferences("locked", Context.MODE_PRIVATE);
		String save = pref.getString("pattern."+contact, "");
		if(save.length() < 1){
			SharedPreferences.Editor edit = pref.edit();
			edit.remove("contact");
			edit.remove("pin."+contact);
			edit.commit();
			set = true;
		}
		Settings.Display.setMinWiredDots(this, 1);
		SharedPreferences pref2 = getSharedPreferences("preferences", Context.MODE_PRIVATE);
		Settings.Display.setStealthMode(this, pref2.getBoolean("stealth", false));
		if(set){
			this.setFinishOnTouchOutside(true);
			Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, this, LockPatternActivity.class);
			startActivityForResult(intent, REQ_CREATE_PATTERN);
		}else{
			char[] savedPattern = new char[save.length()];
			for(int k=0; k<save.length(); k++){
				savedPattern[k] = save.charAt(k);
			}
			Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null, this, LockPatternActivity.class);
			intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
			startActivityForResult(intent, REQ_ENTER_PATTERN);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	    case REQ_CREATE_PATTERN: {
	        if (resultCode == RESULT_OK) {
	            char[] pattern = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN);
	            
	            String save = "";
	            for(char c : pattern){
	            	save += c;
	            }
	            
	            SharedPreferences pref = getSharedPreferences("locked", Context.MODE_PRIVATE);
				SharedPreferences.Editor edit = pref.edit();
				edit.putString("pattern."+contact, save);
				edit.putLong(contact, 0);
				edit.commit();
				Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/locked.xml", true);
				Toast.makeText(PinActivity.this, "lock activated", Toast.LENGTH_SHORT).show();
				
				finish();
	        }else{
	        	finish();
	        }
	        break;
	    }// REQ_CREATE_PATTERN
	    case REQ_ENTER_PATTERN: {
	        /*
	         * NOTE that there are 4 possible result codes!!!
	         */
	        switch (resultCode) {
	        case RESULT_OK:
	            // The user passed
	        	SharedPreferences pref = getSharedPreferences("locked", Context.MODE_PRIVATE);
				SharedPreferences.Editor edit = pref.edit();
				edit.putLong(contact, System.currentTimeMillis());
				edit.commit();
				Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/locked.xml", true);
				finish();
	            break;
	        case RESULT_CANCELED:
	            // The user cancelled the task
	            break;
	        case LockPatternActivity.RESULT_FAILED:
	            // The user failed to enter the pattern
	            break;
	        case LockPatternActivity.RESULT_FORGOT_PATTERN:
	            // The user forgot the pattern and invoked your recovery Activity.
	            break;
	        }

	        /*
	         * In any case, there's always a key EXTRA_RETRY_COUNT, which holds
	         * the number of tries that the user did.
	         */
	        //int retryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 0);

	        break;
	    }// REQ_ENTER_PATTERN
	    }
	}
	
	@Override
    public void onBackPressed() {
		if(fromConversations){
			Intent intent = new Intent("android.intent.action.MAIN");
			 intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Main"));
			 intent.addCategory("android.intent.category.LAUNCHER");
			 startActivity(intent);
		}else{
			Helper.shell("am force-stop com.whatsapp", true);
			super.onBackPressed();
		}
    }
}
