package de.bidlingmeyer.xposed.WhatsAppX;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

public class WallpaperActivity extends Activity {
	
     private String jid;
     private int width, height;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		jid = i.getStringExtra("jid");
		width = i.getIntExtra("width", 0);
		height = i.getIntExtra("height", 0);
		width = 720;
		height = 1280;
        
		Crop.pickImage(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            doCrop(data.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
	}
	
	private void doCrop(Uri source){
		float fl = (float) height/width;
		int y = (int) (fl*100);
		
		File newDirectory = new File(Environment.getExternalStorageDirectory().toString()+"/WhatsApp/Media/WallPaper/");
		Uri destination = Uri.fromFile(new File(newDirectory, ("xposed_"+jid.split("@")[0]+".jpg")));
		
		Crop.of(source, destination).withAspect(100, y).start(this);
	}
	
	 private void handleCrop(int resultCode, Intent result) {
	        if (resultCode == RESULT_OK) {
	        	Toast.makeText(this, "Reenter Conversation to apply changes", Toast.LENGTH_SHORT).show();
	        	finish();
	        } else if (resultCode == Crop.RESULT_ERROR) {
	            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
	            finish();
	        }
	    }
}