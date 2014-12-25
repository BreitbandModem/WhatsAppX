package de.bidlingmeyer.xposed.WhatsAppX;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity implements OnCheckedChangeListener, OnClickListener, OnSeekBarChangeListener{
	
	boolean fromWhatsapp, cannot; 
	//boolean prefFavSwitch, prefFavCheck, prefWallSwitch, prefLockSwitch, prefRemindSwitch, prefPreviewSwitch, prefManipSwitch, prefLockCheck, prefRemindCheck;
	boolean prefGear, prefClick, prefLock, prefReminder, prefStar, prefSelfie, prefFavorites;
	int prefSize, prefColor;
	IntentFilter intentFilter;
	Intent starterIntent;
	Receiver rec;
	String jid="";
	SharedPreferences prefs;
	SharedPreferences.Editor edit;
	SeekBar seekBar;
	Switch switchClick, switchSelfie, switchFavorites;
	CheckBox switchGear, switchLock, switchReminder, switchStar;
	ImageView imageGear, imageLock, imageReminder, imageStar;
	Button favButton, wallButton, remindButton, previewButton, resetButton, colorButton;
	ImageView images[];
	GridLayout gridLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);			
		
		starterIntent = getIntent();
		fromWhatsapp = starterIntent.getBooleanExtra("whatsapp", false);
		jid = starterIntent.getStringExtra("jid");
		intentFilter = new IntentFilter("de.bidlingmeyer.xposed.WhatsAppX.cannot");
        rec = new Receiver();
        this.registerReceiver(rec, intentFilter);
        cannot = false;
		
        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        edit = prefs.edit();
        if(Helper.shell("sqlite3 /data/data/com.whatsapp/databases/msgstore.db 'Select MAX(_id) FROM messages WHERE key_from_me=0';", true).trim().length() < 1){
        	edit.putBoolean("sqlite3", false);
        	//Toast.makeText(getBaseContext(), "sqlite3 is not installed! This app will not work!", Toast.LENGTH_LONG).show();
        	new AlertDialog.Builder(SettingsActivity.this)
    	    .setTitle("sqlite3 is missing or doesn't work")
    	    .setMessage("This app will only work if sqlite3 is installed on your device!\nSearch the Play Store for 'SQLite installer'")
    	    .setIcon(android.R.drawable.ic_dialog_alert)
    	    .show();
        }else{
        	edit.putBoolean("sqlite3", true);
        }
        
        Boolean settings [] = new Boolean[7];
        
        settings[0] = prefGear = prefs.getBoolean("gear", true);
        settings[1] = prefClick = prefs.getBoolean("click", false);
        settings[2] = prefLock = prefs.getBoolean("lock", false);
        settings[3] = prefReminder = prefs.getBoolean("reminder", true);
        settings[4] = prefStar = prefs.getBoolean("star", false);
        settings[5] = prefSelfie = prefs.getBoolean("selfie", true);
        settings[6] = prefFavorites = prefs.getBoolean("favorites", true);
        prefSize = prefs.getInt("size", 30);
        prefColor = prefs.getInt("color", Color.WHITE);
        
        images = new ImageView[4];
        
        images[0] = imageGear = (ImageView) findViewById(R.id.imageViewGear);
        images[1] = imageLock = (ImageView) findViewById(R.id.imageViewLock);
        images[2] = imageReminder = (ImageView) findViewById(R.id.imageViewReminder);
        images[3] = imageStar = (ImageView) findViewById(R.id.imageViewStar);
        
        int k = Helper.convertDp(this, 5);
        int j = Helper.convertDp(this, 30);
        boolean background = false;
    	float[] hsv = new float[3];
    	Color.colorToHSV(prefColor, hsv);
    	if (hsv[2] < 0.2) {
    		background = true;
    	}
        for(int i=0; i<images.length; i++){
        	images[i].getLayoutParams().width = j;
        	images[i].setPadding(k, 0, k, 0);
        	images[i].getDrawable().setColorFilter(new LightingColorFilter( prefColor, prefColor ));
        	if(background){
        		images[i].setBackgroundColor(Color.WHITE);
        	}else{
        		images[i].setBackgroundColor(Color.TRANSPARENT);
        	}
        	images[i].invalidate();
        }
        
        gridLayout = (GridLayout) findViewById(R.id.gridLayout);
        
        CompoundButton switches [] = new CompoundButton[7];
        
        switches[0] = switchGear = (CheckBox) findViewById(R.id.switchGear);
        switches[1] = switchClick = (Switch) findViewById(R.id.switchClick);
        switches[2] = switchLock = (CheckBox) findViewById(R.id.switchLock);
        switches[3] = switchReminder = (CheckBox) findViewById(R.id.switchReminder);
        switches[4] = switchStar = (CheckBox) findViewById(R.id.switchStar);
        switches[5] = switchSelfie = (Switch) findViewById(R.id.switchSelfie);
        switches[6] = switchFavorites = (Switch) findViewById(R.id.switchFavorites);
        
        for(int i=0; i<switches.length; i++){
        	switches[i].setChecked(settings[i]);
        	switches[i].setOnCheckedChangeListener(this);
        }
        
        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setProgress(prefSize-20);
        
        
        favButton = (Button) findViewById(R.id.button1);
        wallButton = (Button) findViewById(R.id.button2);
        remindButton = (Button) findViewById(R.id.button5);
        previewButton = (Button) findViewById(R.id.button6);
        resetButton = (Button) findViewById(R.id.button7);
        colorButton = (Button) findViewById(R.id.buttonColor);
        favButton.setOnClickListener(this);
        wallButton.setOnClickListener(this);
        remindButton.setOnClickListener(this);
        previewButton.setOnClickListener(this);
	    resetButton.setOnClickListener(this);
	    colorButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				AmbilWarnaDialog dialog = new AmbilWarnaDialog(SettingsActivity.this, prefColor, new OnAmbilWarnaListener() {
			        @Override
			        public void onOk(AmbilWarnaDialog dialog, int color) {
			            // color is the color selected by the user.
			        	boolean background = false;
			        	float[] hsv = new float[3];
			        	Color.colorToHSV(color, hsv);
			        	if (hsv[2] < 0.2) {
			        		background = true;
			        	}
			        	for(int i=0; i<images.length; i++){
			            	images[i].getDrawable().setColorFilter(new LightingColorFilter( color, color ));
			            	if(background){
			            		images[i].setBackgroundColor(Color.WHITE);
			            	}else{
			            		images[i].setBackgroundColor(Color.TRANSPARENT);
			            	}
			            	images[i].invalidate();
			            }
			        	prefColor = color;
			        	edit.putInt("color", color);
			        }
			                
			        @Override
			        public void onCancel(AmbilWarnaDialog dialog) {
			                // cancel was selected by the user
			        }
				});
				dialog.show();
			}
	    });
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch(buttonView.getId()){
		case R.id.switchGear : edit.putBoolean("gear", isChecked);
							   prefGear = isChecked;
							   break;
		case R.id.switchClick : edit.putBoolean("click", isChecked);
							   prefClick = isChecked;
							   break;
		case R.id.switchLock : edit.putBoolean("lock", isChecked);
							   prefLock = isChecked;
							   break;
		case R.id.switchReminder : edit.putBoolean("reminder", isChecked);
							   prefReminder = isChecked;
							   break;
		case R.id.switchStar : edit.putBoolean("star", isChecked);
							   prefStar = isChecked;
							   break;
		case R.id.switchSelfie : edit.putBoolean("selfie", isChecked);
							   prefSelfie = isChecked;
							   break;
		case R.id.switchFavorites : edit.putBoolean("favorites", isChecked);
							   prefFavorites = isChecked;
							   break;
		}
	}
	
	
	String title="", message="", toast="";
	@Override
	public void onClick(final View v) {
		switch(v.getId()){
		case R.id.button1 : title = "Delete Favorites";
							message = "Are you sure you want to delete all favorites?";
							toast = "All Favorites have been deleted!";
							break;
		case R.id.button2 : title = "Delete Wallpapers";
							message = "Are you sure you want to delete all wallpapers?";
							toast = "All Wallpapers have been deleted!";
							break;
		case R.id.button5 : title = "Delete Reminders";
							message = "Are you sure you want to clear all reminders?";
							toast = "All reminders have been cleared!";
							break;
		case R.id.button6 : title = "Delete Preview Settings";
							message = "Are you sure you want to delete all preview preferences?";
							toast = "All preview preferences have been cleared!";
							break;
		case R.id.button7 : title = "Reset Everything";
							message = "Are you sure you want to reset all settings and delete all wallpapers?";
							toast = "Everything cleared";
							break;
		default : title = "error";
				  message = "error";
				  toast = "error";
				  break;
		}
		new AlertDialog.Builder(SettingsActivity.this)
	    .setTitle(title)
	    .setMessage(message)
	    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	        @Override
			public void onClick(DialogInterface dialog, int which) {
	        	click(v.getId());
				Toast.makeText(getBaseContext(), toast, Toast.LENGTH_LONG).show();
	        }
	     })
	    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	        @Override
			public void onClick(DialogInterface dialog, int which) { 
	            // do nothing
	        }
	     })
	    .setIcon(android.R.drawable.ic_dialog_alert)
	    .show();
	}
	
	public void click(int id){
		boolean reset = false;
		switch(id){
		case R.id.button7 : SharedPreferences s0 = getSharedPreferences("contacts", Context.MODE_PRIVATE);
							SharedPreferences.Editor edit0 = s0.edit();
							edit0.clear();
							edit0.commit();
							s0 = getSharedPreferences("preferences", Context.MODE_PRIVATE);
							edit0 = s0.edit();
							edit0.clear();
							edit0.commit();
							reset = true;
		case R.id.button1 : SharedPreferences s = getSharedPreferences("tags", Context.MODE_PRIVATE);
    						SharedPreferences.Editor edit = s.edit();
    						edit.clear();
    						edit.commit();
    						deleteDatabase(DatabaseContract.DATABASE_NAME);
    						if(!reset) break;
		case R.id.button2 : Helper.shell("find . /sdcard/WhatsApp/Media/WallPaper -maxdepth 1 -type f -name 'xposed_*' -exec rm -rfv {} +", true);
							if(!reset) break;
		case R.id.button5 : SharedPreferences s2 = getSharedPreferences("notification", Context.MODE_PRIVATE);
							SharedPreferences.Editor edit2 = s2.edit();
							edit2.clear();
							edit2.commit();
							if(!reset) break;
		case R.id.button6 : SharedPreferences s3 = getSharedPreferences("preview", Context.MODE_PRIVATE);
							SharedPreferences.Editor edit3 = s3.edit();
							edit3.clear();
							edit3.commit();
							break;
		}
		if(reset){
			startActivity(starterIntent);
			finish();
		}
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int j = Helper.convertDp(this, progress+20);
        for(int i=0; i<images.length; i++){
        	GridLayout.LayoutParams params = (GridLayout.LayoutParams) images[i].getLayoutParams();
        	params.width = j;
        	images[i].setLayoutParams(params);	
        }
        prefSize = progress;
        edit.putInt("size", progress+20);
	}
	
	 @Override
	    public void onBackPressed() {
	    	if(!cannot && fromWhatsapp){
	    		toContact();
	    	}else{
	    		super.onBackPressed();
	    	}
	    }
	    public void toContact(){
	    	if(jid.contains("@g.us"))
	    		jid = "";
	    	Uri uri = Uri.parse("smsto:"+jid);
	        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
	        intent.setPackage("com.whatsapp");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	        startActivity(intent);
	    }
	    
	    @Override
	    protected void onResume() {
	        super.onResume();
	        registerReceiver(rec, intentFilter);
	        cannot = false;
	    }
	    @Override
	    protected void onPause() {
	        super.onPause();
	        unregisterReceiver(rec);
	        edit.commit();
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/preferences.xml", true);
	    }
	    
	    @Override
	    protected void onDestroy() {
	        super.onDestroy();
	        edit.commit();
			Helper.shell("chmod 777 /data/data/de.bidlingmeyer.xposed.WhatsAppX/shared_prefs/preferences.xml", true);
	    }
	    
	    public class Receiver extends BroadcastReceiver {
	   	 @Override
	   	 public void onReceive(Context context, Intent intent) {
	   		 cannot = true;
	   	 }
	   }

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {			
		}

}
