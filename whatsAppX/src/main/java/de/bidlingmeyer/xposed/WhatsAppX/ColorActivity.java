package de.bidlingmeyer.xposed.WhatsAppX;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

public class ColorActivity extends Activity {
	
	String jid;
	SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		jid = getIntent().getStringExtra("jid");
		if(jid.length() < 1)
			finish();
		jid = jid.split("@")[0];
		
		prefs = getSharedPreferences("contactColor", Context.MODE_PRIVATE);
		
		AmbilWarnaDialog dialog = new AmbilWarnaDialog(ColorActivity.this, prefs.getInt(jid, Color.WHITE), true, false, true, new OnAmbilWarnaListener() {
	        @Override
	        public void onOk(AmbilWarnaDialog dialog, int color) {
	            // color is the color selected by the user.
	        	
	        	SharedPreferences.Editor edit = prefs.edit();
	        	edit.putInt(jid, color);
	        	edit.commit();
				Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/contactColor.xml", true, false);
				Toast.makeText(ColorActivity.this, "Restart Whatsapp to apply change", Toast.LENGTH_SHORT).show();
	        	finish();
	        }
	                
	        @Override
	        public void onCancel(AmbilWarnaDialog dialog) {
	                finish();
	        }
	        @Override
	        public void onReset(AmbilWarnaDialog dialog){
	        	SharedPreferences.Editor edit = prefs.edit();
	        	edit.remove(jid);
	        	edit.commit();
				Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/contactColor.xml", true, false);
				Toast.makeText(ColorActivity.this, "Restart Whatsapp to apply change", Toast.LENGTH_SHORT).show();
				finish();
	        }
		});
		dialog.show();
	}
	
}
