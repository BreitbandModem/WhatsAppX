package de.bidlingmeyer.xposed.WhatsAppX;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class WallpaperActivity extends Activity {
	
	 private Uri mImageCaptureUri;
     private String conversationName;
     private int width, height;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		conversationName = i.getStringExtra("conversationName");
		width = i.getIntExtra("width", 0);
		height = i.getIntExtra("height", 0);
       /* if(conversationName.equals("") || width == 0 || height == 0)
        	finish();*/
		
		width = 720;
		height = 1280;
		
        if(width > height){
        	int w = width;
        	width = height;
        	height = w;
        }
        
		Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, 0);
	}


	public class CropOptionAdapter extends ArrayAdapter<CropOption> {
		private ArrayList<CropOption> mOptions;
		private LayoutInflater mInflater;
		
		public CropOptionAdapter(Context context, ArrayList<CropOption> options) {
			super(context, R.layout.crop_selector, options);
			mOptions = options;
			mInflater = LayoutInflater.from(context);
		}
			
		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup group) {
			if (convertView == null)
				convertView = mInflater.inflate(R.layout.crop_selector, null);
			
			CropOption item = mOptions.get(position);
			if (item != null) {
				((ImageView) convertView.findViewById(R.id.iv_icon)).setImageDrawable(item.icon);
				((TextView) convertView.findViewById(R.id.tv_name)).setText(item.title);
				return convertView;
			}
			
			return null;
		}
	}
	
	public class CropOption {
		public CharSequence title;
		public Drawable icon;
		public Intent appIntent;
	}
		
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
		if (resultCode != RESULT_OK || data == null)
		finish();
		
		switch (requestCode) {
		case 0:
			try{
				mImageCaptureUri = data.getData();
			}catch(Exception e){}
			doCrop();
			break;
		
		case 2:
			Toast.makeText(this, "Reenter Conversation to apply changes!",Toast.LENGTH_LONG).show();
			//Helper.refreshContactList(this);
			finish();
			break;
		}
	}
		
	private void doCrop() {
		try{
			final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();
			/**
			* Open image crop app by starting an intent
			* ‘com.android.camera.action.CROP‘.
			*/
			Intent intent = new Intent("com.android.camera.action.CROP");
			intent.setType("image/*");
			
			/**
			* Check if there is image cropper app installed.
			*/
			List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
			
			int size = list.size();
			
			/**
			* If there is no image cropper app, display warning message
			*/
			if (size == 0) {
				Toast.makeText(this, "Can not find image crop app",Toast.LENGTH_SHORT).show();
				return;
			} else {
			/**
			* Specify the image path, crop dimension and scale
			*/
			intent.setData(mImageCaptureUri);
			intent.putExtra("outputX", width);
			intent.putExtra("outputY", height);
			intent.putExtra("aspectX", 100);
			float fl = (float) height/width;
			int r = (int) (fl*100);
			intent.putExtra("aspectY", r);
			intent.putExtra("scale", false);
		    intent.putExtra("crop", "true");
			intent.putExtra("return-data", false);
			
			File f = createNewFile(conversationName);
		    try {
		        f.createNewFile();
		    } catch (IOException ex) {
		          
		    }
		
		    Uri uri = Uri.fromFile(f);
		    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
			/**
			* There is posibility when more than one image cropper app exist,
			* so we have to check for it first. If there is only one app, open
			* then app.
			*/
			
			if (size == 1) {
				Intent i = new Intent(intent);
				ResolveInfo res = list.get(0);
				i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
				startActivityForResult(i, 2);
			} else {
				/**
				* If there are several app exist, create a custom chooser to
				* let user selects the app.
				*/
				for (ResolveInfo res : list) {
					final CropOption co = new CropOption();
					co.title = getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
					co.icon = getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
					co.appIntent = new Intent(intent);
					co.appIntent.setComponent(new ComponentName(res.activityInfo.packageName,res.activityInfo.name));
					cropOptions.add(co);
				}
			
				CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Choose Crop App");
				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				            @Override
							public void onClick(DialogInterface dialog, int item) {
				                   startActivityForResult(cropOptions.get(item).appIntent, 2);
				            }
				});
				
				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
					     if (mImageCaptureUri != null) {
					            try{
					            	getContentResolver().delete(mImageCaptureUri, null, null);
					            }catch(Exception e){}
					            mImageCaptureUri = null;
					     }
					     finish();
					}
				});
				
				AlertDialog alert = builder.create();
				
				alert.show();
				}
			}
		}catch(Exception e){
			Toast.makeText(this, "Can not find image crop app",Toast.LENGTH_SHORT).show();
		}
	}
	
	private File createNewFile(String name){
	    File newDirectory = new File(Environment.getExternalStorageDirectory().toString()+"/WhatsApp/Media/WallPaper/");
	    if(!newDirectory.exists()){
	        newDirectory.mkdir();
	    }
	    
	    int value = 0;
	    for(int i=0; i<name.length(); i++){
	    	char c = name.charAt(i);
	    	int b = Character.getNumericValue(c);
	    	value += b;
	    }
	    
	    File file = new File(newDirectory, ("xposed_"+value+".jpg"));
	    if(file.exists()){
	        //this wont be executed
	        file.delete();
	        try {
	            file.createNewFile();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	
	    return file;
	}
	
}