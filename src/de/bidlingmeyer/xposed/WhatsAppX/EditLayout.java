package de.bidlingmeyer.xposed.WhatsAppX;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResForwarder;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class EditLayout implements IXposedHookInitPackageResources, IXposedHookZygoteInit, IXposedHookLoadPackage{
	
	String MODULE_PATH, conversationName="", message="", layoutTime="", contact="", replaceText="", conversationNameConfirmed="";
	XModuleResources modRes;
	static ImageButton button = null;
	FrameLayout cameraLayout;
	int width, height;
	boolean scramble = false, handleLocked = false, conversationsScreen = false, previewIsGroup = false, previewHide = false, contactLocked, hasWallpaper, fromConversations;
	RelativeLayout conversationLayout;
	ImageButton lockButton, settingsButton, starButton;
	LinearLayout topLayout, conversationsLayout;
	long actionbarLoad, resumeCreate = 0;
	ArrayList<String> previewContacts = new ArrayList<String>();
	ArrayList<Integer> previewContactsIsGroup = new ArrayList<Integer>();
	
	@Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.whatsapp"))
            return;
        
        XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		if(!prefs.getBoolean("sqlite3", true))
			return;
        
        //String s = Helper.shell("sqlite3 /data/data/com.whatsapp/databases/wa.db 'Select display_name, jid FROM wa_contacts WHERE is_whatsapp_user=1'\n", false).trim();
        //XposedBridge.log(s);
        
        @SuppressWarnings("rawtypes")
		final Class conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", lpparam.classLoader);
        
        @SuppressWarnings("rawtypes")
		final Class conversationsClass = XposedHelpers.findClass("com.whatsapp.Conversations", lpparam.classLoader);
        
        @SuppressWarnings("rawtypes")
		final Class wallpaperClass = XposedHelpers.findClass("com.whatsapp.wallpaper.WallPaperView", lpparam.classLoader);
        
        @SuppressWarnings("rawtypes")
		final Class textClass = XposedHelpers.findClass("com.whatsapp.TextEmojiLabel", lpparam.classLoader);
        

        XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				View layout = ((Toast) param.thisObject).getView();
				TextView view = (TextView) layout.findViewById(android.R.id.message);
				if(view.getText().toString().equals("xposed whatsapp fail^^")){
					Intent intent = new Intent();
					intent.setAction("de.bidlingmeyer.xposed.WhatsAppX.cannot");
					layout.getContext().sendBroadcast(intent);
					param.setResult(null);
				}
			}
        });
        
        XposedHelpers.findAndHookMethod(textClass, "setText", CharSequence.class, TextView.BufferType.class, new XC_MethodHook(){

			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				
				String text = "";
				try{
					text = ((CharSequence) param.args[0]).toString();
				}catch(Exception e){
					return;
				}
   				
   				if(text.length() == 0)
   					return;
   				
   				long time = System.currentTimeMillis();
			    if(conversationName.length() < 1 & time-actionbarLoad < 500){//so that after 500ms, no more texts will be scanned
					XSharedPreferences s = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "contacts");
					s.makeWorldReadable();
					Map<String, ?> allEntries = s.getAll();
		       		for(Map.Entry<String, ?> entry : allEntries.entrySet()) {
		       			String [] sp = entry.getKey().split("\\W");
		       			int contains = 0;
		       			for(int i=0; i<sp.length; i++){
			       			 if(text.contains(sp[i])){
			       				 contains++;
			       			 }
		       			}
		       			if(contains == sp.length)
		       				conversationName = text;
		       		}
		       		
		       		//long elapsedTime = System.currentTimeMillis() - time;
			    }
			    
			    if(conversationsScreen && previewContacts.size() > 0){
			    	//XposedBridge.log("text: "+text);
				    time = System.currentTimeMillis();

				    if(previewIsGroup){
				    	//XposedBridge.log("isgroup");
				    	if( ! text.trim().contains(":")){
				    		previewHide = false;
				    	}
				    	param.args[0] = "";
				    	previewIsGroup = false;
				    }else if(previewHide){
				    	//XposedBridge.log("previewHide: "+previewHide);
	   					param.args[0] = "";
	   					previewHide = false;
	   				}else{
	   					int index = previewContacts.indexOf(text.trim());

	   					if(index >= 0){
	   						previewIsGroup = previewContactsIsGroup.get(index) == 2; //is group
	   						previewHide = true;
	   						//XposedBridge.log("previewIsGroup: "+previewIsGroup+" previewHide: "+previewHide);
	   		       		}
	   		       		
	   				}
	   				//long elapsedTime = System.currentTimeMillis() - time;
			    }
   				
			    Pattern pattern1 = Pattern.compile("\\w+");
			    Matcher matcher1 = pattern1.matcher(text);
	       		
	       		if(replaceText.length() > 0){
	       			String result = "";
	       			if(!matcher1.find())
	       				result = text;
	       			
	       			int index = 0;
	       			while(matcher1.find(index)){//text contains alpha
	       				if(matcher1.start()-index == 0){//starts with alpha
	       					result += replaceText;
	       					index = matcher1.end();//
	       				}else{//starts with non alpha
	       					result += text.substring(index, matcher1.start());//add all non alphas
	       					index = matcher1.start();
	       				}
	       			}
	       			if(index < text.length()-1)
	       				result += text.substring(index);
		       		if(replaceText.equals("Vorhaut"))
		       			result += "by Nico";
		       		param.args[0] = result;
		       	}
	       		
	       		if(scramble){
	       			text = ((CharSequence) param.args[0]).toString();
	       			matcher1 = pattern1.matcher(text);
	       			String result = "";
	       			if(!matcher1.find())
	       				result = text;
	       			int index = 0;
	       			while(matcher1.find(index)){//text contains alpha
	       				if(matcher1.start()-index == 0){//starts with alpha
	       					char [] letters = text.substring(index, matcher1.end()).toCharArray();
	       					for(int i=1; i<letters.length-1; i++){
					    		int rand = (int) (Math.random()*(letters.length-3))+1;
					    		char tmp = letters[i];
					    		letters[i] = letters[rand];
					    		letters[rand] = tmp;
					    	}
					    	for(char c : letters){
					    		result += c;
					    	}
	       					index = matcher1.end();//
	       				}else{//starts with non alpha
	       					result += text.substring(index, matcher1.start());//add all non alphas
	       					index = matcher1.start();
	       				}
	       			}
	       			if(index < text.length()-1)
	       				result += text.substring(index);
	       			param.args[0] = result;
	       		}
			}
        	
        });
        
        XposedHelpers.findAndHookMethod(wallpaperClass, "setDrawable", Drawable.class, new XC_MethodHook(){

			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				//XResForwarder x = modRes.fwd(R.drawable.default_wallpaper);
				if(conversationName.length() < 1)
					return;
				
				int value = 0;
			    for(int i=0; i<conversationName.length(); i++){
			    	char c = conversationName.charAt(i);
			    	int b = Character.getNumericValue(c);
			    	value += b;
			    }
			    
				String pathName = Environment.getExternalStorageDirectory().toString()+"/WhatsApp/Media/WallPaper/xposed_"+value+".jpg";
				 Drawable d = Drawable.createFromPath(pathName);
				if(d != null){
					param.args[0] = d;
					hasWallpaper = true;
				}
			}
        	
        });
        
        XposedHelpers.findAndHookMethod(conversationClass, "onCreate", Bundle.class, new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				resumeCreate();
			}
        });
        
        XposedHelpers.findAndHookMethod(conversationClass, "onResume", new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				resumeCreate();
			}
        });
        
        XposedHelpers.findAndHookMethod(conversationClass, "onPause", new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				handleLocked = false;
				contactLocked = false;
				actionbarLoad = 0;
				hasWallpaper = false;
				fromConversations = false;
			}
        });
        
        XposedHelpers.findAndHookMethod(conversationClass, "onDestroy", new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				conversationName = "";
				resumeCreate = 0;
				conversationLayout = null;
				handleLocked = false;
				contactLocked = false;
				actionbarLoad = 0;
				hasWallpaper = false;
				fromConversations = false;
			}
        });
        
        
        
        XposedHelpers.findAndHookMethod(conversationsClass, "onCreate", Bundle.class, new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				conversationsScreen = true;
				loadPreviewContacts();
			}
        });
        
        XposedHelpers.findAndHookMethod(conversationsClass, "onResume", new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				conversationsScreen = true;
				loadPreviewContacts();
			}
        });
        
        XposedHelpers.findAndHookMethod(conversationsClass, "onPause", new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				conversationsScreen = false;
			}
        });
        
        XposedHelpers.findAndHookMethod(conversationsClass, "onDestroy", new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);
				conversationsScreen = false;
				Intent intent = new Intent();
	        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.RefreshContactsService"));
	        	XposedBridge.log("start intent, layout: "+conversationsLayout);
	        	if(conversationsLayout != null) conversationsLayout.getContext().startService(intent);
	        	XposedBridge.log("started");
			}
        });
    }
	
	@Override
	public void handleInitPackageResources(final InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals("com.whatsapp")) {
            return;
        }
		modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

		XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		if(!prefs.getBoolean("sqlite3", true))
			return;
		
		if(prefs.getBoolean("selfie", true)){
			resparam.res.setReplacement("com.whatsapp", "drawable", "ic_camera", modRes.fwd(R.drawable.ic_star));
			resparam.res.setReplacement("com.whatsapp", "string", "cannot_start_camera", "xposed whatsapp fail^^");
		}
		
		
		//conversations actionbar
		/*resparam.res.hookLayout("com.whatsapp", "layout", "abs__action_bar_home", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				final ImageView view = (ImageView) liparam.view.findViewById(liparam.res.getIdentifier("abs__up", "id", "com.whatsapp"));
				XposedBridge.log("view: "+view);
				view.setOnTouchListener(new OnTouchListener(){
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						XposedBridge.log("touched");
						Intent intent = new Intent();
						intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.PagerActivity"));
						intent.putExtra("whatsapp", true);
						int i, k;
						Resources res = view.getResources();
						i = res.getIdentifier("slide_out_left", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
						k = res.getIdentifier("slide_in_right", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
						ActivityOptions opts = ActivityOptions.makeCustomAnimation(view.getContext(), k, i);
						view.getContext().startActivity(intent, opts.toBundle());
						return false;
					}
				});
			}
		});*/
		
		//camera
		resparam.res.hookLayout("com.whatsapp", "layout", "camera", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				
				if(!handleLocked){
					resumeCreate();
				}
				
				XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
				prefs.makeWorldReadable();
				if(prefs.getBoolean("selfie", true)){
					cameraLayout = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("camera_layout", "id", "com.whatsapp"));
					Intent intent = new Intent();
					intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.PagerActivity"));//startet die pager activity
					intent.putExtra("conversationName", conversationName);
	   			 	//intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP); 
					intent.putExtra("whatsapp", true);
					int i, k;
					Resources res = cameraLayout.getResources();
					i = res.getIdentifier("slide_out_left", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
					k = res.getIdentifier("slide_in_right", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
					ActivityOptions opts = ActivityOptions.makeCustomAnimation(cameraLayout.getContext(), k, i);
					cameraLayout.getContext().startActivity(intent, opts.toBundle());
				}
			}
		});
		
		//conversations
		resparam.res.hookLayout("com.whatsapp", "layout", "conversations", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
					conversationsLayout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("chats_layout", "id", "com.whatsapp"));
					Intent intent = new Intent();
		         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
		        	conversationsLayout.getContext().startService(intent);
			}
		});
		
		//Contact Picker
				resparam.res.hookLayout("com.whatsapp", "layout", "conversations_row", new XC_LayoutInflated() {
					@Override
					public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
						final TextView text = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("conversations_row_contact_name", "id", "com.whatsapp"));
						final FrameLayout l = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("contact_selector", "id", "com.whatsapp"));
						RelativeLayout layout = (RelativeLayout) l.getParent();
						layout.setOnTouchListener(new OnTouchListener()
						{
							@SuppressLint("ClickableViewAccessibility")
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								conversationName = text.getText().toString().trim();
								fromConversations = true;
								return false;
							}
						});
					}
				});
				
		//Conversation Header
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation_actionbar", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				actionbarLoad = System.currentTimeMillis();
				final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("back", "id", "com.whatsapp"));
				//resparam.res.setReplacement("com.whatsapp", "drawable", "default_wallpaper", modRes.fwd(R.drawable.default_wallpaper));
				XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
				prefs.makeWorldReadable();
				
				int i = Helper.convertDp(layout.getContext(), 5);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Helper.convertDp(layout.getContext(), 25), LayoutParams.MATCH_PARENT);
				XResForwarder x=null;
				
				if(prefs.getBoolean("gear", true)){
				settingsButton = new ImageButton(layout.getContext());
				settingsButton.setBackgroundColor(Color.TRANSPARENT);
				settingsButton.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						showDialog(layout.getContext(), v, true);
					}
				});
				x = modRes.fwd(R.drawable.ic_settings);
			    settingsButton.setLayoutParams(params);
			    settingsButton.setPadding(i, 0, i, 0);
			    settingsButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			    settingsButton.setAdjustViewBounds(true);
			    settingsButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
				layout.addView(settingsButton);
				}
				
				if(prefs.getBoolean("lock", false)){
				lockButton = new ImageButton(layout.getContext());
				
			    lockButton.setBackgroundColor(Color.TRANSPARENT);
				
			    lockButton.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View view) {
						if(conversationName.length() < 1 || scramble || replaceText.length() > 0)
							return;
						XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "locked");
						prefs.makeWorldReadable();
						long locked = prefs.getLong(conversationName, -1);
						if(locked >= 0){//unlock contact:
							Intent intent = new Intent();
				         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.LockContactService"));
				         	intent.putExtra("contact", conversationName);
				         	intent.putExtra("lock", false);
				        	layout.getContext().startService(intent);
				        	conversationLayout.setVisibility(View.VISIBLE);
							XResForwarder x = modRes.fwd(R.drawable.ic_unlocked);
							lockButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
							contactLocked = false;
						}else if(locked < 0){//lock contact:
							Intent intent = new Intent();
				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.PinActivity"));
				        	intent.putExtra("contact", conversationName);
				        	intent.putExtra("set", true);
				        	layout.getContext().startActivity(intent);
						}
					}
				});
			    
			    lockButton.setLayoutParams(params);
			    lockButton.setPadding(i, 0, i, 0);
			    lockButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			    lockButton.setAdjustViewBounds(true);
			    x = modRes.fwd(R.drawable.ic_unlocked);
			    lockButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
				layout.addView(lockButton);
				}
				
				
				if(prefs.getBoolean("reminder", true)){
				ImageButton remindButton = new ImageButton(layout.getContext());
				remindButton.setBackgroundColor(Color.TRANSPARENT);
				remindButton.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if(conversationName.length() > 0 && !scramble && replaceText.length() == 0){
							Intent intent = new Intent();
				         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
				         	intent.putExtra("contact", conversationName);
				        	layout.getContext().startService(intent);
						}
					}
				});
				
				remindButton.setLayoutParams(params);
			    remindButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			    remindButton.setAdjustViewBounds(true);
			    remindButton.setPadding(i, 0, i, 0);
			    x = modRes.fwd(R.drawable.ic_reminder);
			    remindButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
				layout.addView(remindButton);
				}
				
				if(prefs.getBoolean("star", false)){
					starButton = new ImageButton(layout.getContext());
					starButton.setBackgroundColor(Color.TRANSPARENT);
					starButton.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							Intent intent = new Intent();
							intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.PagerActivity"));//startet die pager activity
							intent.putExtra("conversationName", conversationName);
							intent.putExtra("whatsapp", true);
							int i, k;
							Resources res = layout.getResources();
							i = res.getIdentifier("slide_out_left", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
							k = res.getIdentifier("slide_in_right", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
							ActivityOptions opts = ActivityOptions.makeCustomAnimation(layout.getContext(), k, i);
							layout.getContext().startActivity(intent, opts.toBundle());
						}
					});
					x = modRes.fwd(R.drawable.ic_star_white);
				    starButton.setLayoutParams(params);
				    starButton.setPadding(i, 0, i, 0);
				    starButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
				    starButton.setAdjustViewBounds(true);
				    starButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
					layout.addView(starButton);
				}
			}
		 });
		
		//Text Messages Left
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation_text_row_left", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("text_content_layout", "id", "com.whatsapp"));
				
				if(!handleLocked){
					resumeCreate();
				}
				
				XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
				prefs.makeWorldReadable();
				if(prefs.getBoolean("favorites", true) || prefs.getBoolean("click", false))
				layout.setOnLongClickListener(new OnLongClickListener()
				{
				    @Override
					public boolean onLongClick(View v)
				    {
						TextView messageView = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("message_text", "id", "com.whatsapp"));
						TextView dateView = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("date", "id", "com.whatsapp"));
						TextView groupContactView = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("name_in_group_tv", "id", "com.whatsapp"));
						message = messageView.getText().toString().trim();
						layoutTime = dateView.getText().toString().trim();
						contact = groupContactView.getText().toString().trim();
						if(contact.length() < 1)
							contact = conversationName;
						
						showDialog(layout.getContext(), v, false);
						
						return false;
					}
				});
			}
		 });
		
		//Text Messages Right
				resparam.res.hookLayout("com.whatsapp", "layout", "conversation_text_row_right", new XC_LayoutInflated() {
					@Override
					public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
						final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("text_content_layout", "id", "com.whatsapp"));
						
						if(!handleLocked){
							resumeCreate();
						}
						
						XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
						prefs.makeWorldReadable();
						if(prefs.getBoolean("favorites", true) || prefs.getBoolean("click", false))
						layout.setOnLongClickListener(new OnLongClickListener()
						{
						    @Override
							public boolean onLongClick(View v)
						    {
								
								TextView messageView = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("message_text", "id", "com.whatsapp"));
								TextView dateView = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("date", "id", "com.whatsapp"));
								
								message = messageView.getText().toString().trim();
								layoutTime = dateView.getText().toString().trim();
								contact = "myself";
								
								showDialog(layout.getContext(), v, false);
								
								return false;
							}
						});
						
			       			
						}
				 });
				
				//conversation
				resparam.res.hookLayout("com.whatsapp", "layout", "conversation", new XC_LayoutInflated() {
					@Override
					public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
						conversationLayout = (RelativeLayout) liparam.view.findViewById(liparam.res.getIdentifier("conversation_layout", "id", "com.whatsapp"));
					}
				});
				
				/*resparam.res.hookLayout("com.whatsapp", "layout", "conversation_entry", new XC_LayoutInflated() {
					@Override
					public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
						
						final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("footer", "id", "com.whatsapp"));
						layout.setOnTouchListener(new OnTouchListener(){
							@Override
							public boolean onTouch(View arg0, MotionEvent arg1) {
								Toast.makeText(layout.getContext(), "locked", Toast.LENGTH_SHORT).show();
								return true;
							}							
						});
						layout.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View v) {
								Toast.makeText(layout.getContext(), "locked", Toast.LENGTH_SHORT).show();
							}
						});
					}
				});/*
				
		
		//Image Text Left
		/*resparam.res.hookLayout("com.whatsapp", "layout", "conversation_row_image_left", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				int imageId = liparam.res.getIdentifier("image", "id", "com.whatsapp");
				final ImageView view = (ImageView) liparam.view.findViewById(imageId);
				final TextView c = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("name_in_group_tv", "id", "com.whatsapp"));
				final TextView t = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("date", "id", "com.whatsapp"));
				contact = c.getText().toString().trim();
				layoutTime = t.getText().toString().trim();
				
				final RelativeLayout layout = (RelativeLayout) view.getParent();
				ImageButton b = new ImageButton(layout.getContext());
				
				b.setBackgroundColor(Color.TRANSPARENT);
				b.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View view) {
						
						new AlertDialog.Builder(layout.getContext())
					    .setTitle("Set Favorite")
					    .setMessage("Are you sure you want to add this image to favorites?")
					    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					        @Override
							public void onClick(DialogInterface dialog, int which) {
					        }
					     })
					    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					        @Override
							public void onClick(DialogInterface dialog, int which) { 
					            // do nothing
					        }
					     })
					    .setIcon(layout.getContext().getResources().getIdentifier("compose", "drawable", layout.getContext().getPackageName()))
					     .show();
					}
				});
				layout.addView(b);
			}
		});*/
		
		//MediaView
		/*resparam.res.hookLayout("com.whatsapp", "layout", "media_view", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				ImageView view = (ImageView) liparam.view.findViewById(liparam.res.getIdentifier("share_btn", "id", "com.whatsapp"));
				final LinearLayout layout = (LinearLayout) view.getParent();
				//Toast.makeText(layout.getContext(), "image: "+image, Toast.LENGTH_SHORT).show();

				ImageButton b = new ImageButton(layout.getContext());
				final float scale = layout.getContext().getResources().getDisplayMetrics().density;
				int pixels = (int) (48.0 * scale + 0.5f);
				b.setLayoutParams(new LinearLayout.LayoutParams(pixels, pixels));
				
				final String packName = "de.bidlingmeyer.xposed.WhatsAppX";
			    String mDrawableName = "favorite_icon";
			    Bitmap bMap = null;
			    try {
			        PackageManager manager = layout.getContext().getPackageManager();
			        Resources res = manager.getResourcesForApplication(packName);
			        int id = res.getIdentifier(mDrawableName, "drawable", packName);
			        bMap = BitmapFactory.decodeResource(res, id);
			        bMap = getResizedBitmap(bMap,pixels,pixels);
					b.setImageBitmap(bMap);
			    }
			    catch (NameNotFoundException e) {
			        e.printStackTrace();
			    }
				b.setBackgroundColor(Color.TRANSPARENT);
				b.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View view) {
						Toast.makeText(layout.getContext(), "image: "+image, Toast.LENGTH_SHORT).show();
						
						new AlertDialog.Builder(layout.getContext())
					    .setTitle("Set Favorite")
					    .setMessage("Are you sure you want to add this image to favorites?")
					    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					        @Override
							public void onClick(DialogInterface dialog, int which) {
					            Intent intent = new Intent();
								intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.AddFavorite"));
								intent.putExtra("type", "bitmap");
								intent.putExtra("bitmap", drawableToBitmap(image));
								intent.putExtra("conversationName", conversationName);
								intent.putExtra("contact", contact);
								intent.putExtra("time", time);
								layout.getContext().startActivity(intent);
					        }
					     })
					    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					        @Override
							public void onClick(DialogInterface dialog, int which) { 
					            // do nothing
					        }
					     })
					    .setIcon(layout.getContext().getResources().getIdentifier("compose", "drawable", layout.getContext().getPackageName()))
					     .show();
					}
				});
				layout.addView(b);
			}
		});*/
	}	
	
	public void resumeCreate(){
		if(handleLocked) return;
		
		if(conversationName.length() < 1){
			handleLocked = false;
			return;
		}
		
		if(conversationLayout == null)
   			return;
		
		if(System.currentTimeMillis()-resumeCreate < 200){
			return;
		}
		resumeCreate = System.currentTimeMillis();
		
		XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "locked");
		prefs.makeWorldReadable();
   		XResForwarder x = null;
   		
   		long time = prefs.getLong(conversationName, -1);
   		//XposedBridge.log("time: "+time+"   currTime: "+System.currentTimeMillis());
   		//XposedBridge.log("difference: "+(System.currentTimeMillis()-time));
   		if(time < 0){
   			x = modRes.fwd(R.drawable.ic_unlocked);
			conversationLayout.setVisibility(View.VISIBLE);
			contactLocked = false;
   		}else if((System.currentTimeMillis() - time) < 5000){
   			x = modRes.fwd(R.drawable.ic_locked);
			conversationLayout.setVisibility(View.VISIBLE);
			contactLocked = true;
			/*Intent intent = new Intent();
         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.LockContactService"));
         	intent.putExtra("contact", conversationName);
         	intent.putExtra("lock", true);
         	conversationLayout.getContext().startService(intent);*/
   		}else{
   			x = modRes.fwd(R.drawable.ic_locked);
   			conversationLayout.setVisibility(View.GONE);
   			Intent intent = new Intent();
        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.PinActivity"));
        	intent.putExtra("contact", conversationName);
        	intent.putExtra("fromConversations", fromConversations);
        	conversationLayout.getContext().startActivity(intent);
        	contactLocked = true;
   		}
   		if(lockButton != null) lockButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
   		handleLocked = true;
	}
	
	public void loadPreviewContacts(){
		XSharedPreferences s = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preview");
		s.makeWorldReadable();
		Map<String, ?> allEntries = s.getAll();
		previewContacts = new ArrayList<String>();
		previewContactsIsGroup = new ArrayList<Integer>();
   		for(Map.Entry<String, ?> entry : allEntries.entrySet()){
   			previewContactsIsGroup.add( (Integer) entry.getValue() );
   			previewContacts.add(entry.getKey());
   		}
	}
	
	public void showDialog(final Context context, View v, boolean fromGear){
		XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		PopupMenu popup = new PopupMenu(context, v);
		final boolean hide = previewContacts.contains(conversationName);
		if(conversationName.length() > 0 && !scramble && replaceText.length() == 0){
			
			if(conversationName.length() > 0 && message.length() > 0 && layoutTime.length() > 0 && prefs.getBoolean("favorites", true) && !fromGear){
				popup.getMenu().add(Menu.NONE, 0, Menu.NONE, "Add to Favorites");
			}
			
			if(prefs.getBoolean("click", false) || fromGear){
				if(!prefs.getBoolean("reminder", true))
					popup.getMenu().add(Menu.NONE, 6, Menu.NONE, "Add Reminder");
				
				if(!prefs.getBoolean("lock", false)){
					if(contactLocked){
						popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "Unlock Contact");
					}else{
						popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "Lock Contact");
					}
				}
					if(hasWallpaper){
						popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "Set/Delete Wallpaper");
					}else{
						popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "Set Wallpaper");
					}
				
				
					if(hide){
						popup.getMenu().add(Menu.NONE, 5, Menu.NONE, "Show Message Preview");
					}else{
						popup.getMenu().add(Menu.NONE, 5, Menu.NONE, "Hide Message Preview");
					}
					
					popup.getMenu().add(Menu.NONE, 7, Menu.NONE, "Show Stats");
			}
		}
		
		if(prefs.getBoolean("click", false) || fromGear){
			if(scramble){
				popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "Disable Scramble Text");
			}else{
				popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "Enable Scramble Text");
			}
			if(replaceText.length() > 0){
				popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "Cancel Replace Words With");
			}else{
				popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "Replace Words With");
			}
		}
		
		//popup.getMenu().add(Menu.NONE, 6, Menu.NONE, "Lock Contact");
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {  
             @Override
			public boolean onMenuItemClick(MenuItem item) {
            	 if(item.getItemId()==0){
            		 	Intent intent = new Intent();
			        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.SaveInfoService"));
			        	intent.putExtra("message", message);
			        	intent.putExtra("conversationName", conversationName);
			        	intent.putExtra("layoutTime", layoutTime);
			        	intent.putExtra("contact", contact);
			        	context.startService(intent);
			        	Toast.makeText(context, "Message added to Favorites", Toast.LENGTH_SHORT).show();
            	 }else if(item.getItemId()==1){
            		 	XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "locked");
						prefs.makeWorldReadable();
						long locked = prefs.getLong(conversationName, -1);
						if(locked >= 0){//unlock contact:
							Intent intent = new Intent();
				         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.LockContactService"));
				         	intent.putExtra("contact", conversationName);
				         	intent.putExtra("lock", false);
				        	context.startService(intent);
				        	conversationLayout.setVisibility(View.VISIBLE);
							XResForwarder x = modRes.fwd(R.drawable.ic_unlocked);
							if(lockButton != null) lockButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
							contactLocked = false;
						}else if(locked < 0){//lock contact:
							Intent intent = new Intent();
				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.PinActivity"));
				        	intent.putExtra("contact", conversationName);
				        	intent.putExtra("set", true);
				        	context.startActivity(intent);
						}
            	 }
            	 if(item.getItemId()==2){
            		 	if(hasWallpaper){
            		 		new AlertDialog.Builder(context)
                		    .setTitle("Delete or Set a new Wallpaper")
                		    .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                		        public void onClick(DialogInterface dialog, int whichButton) {
                		        	Intent intent = new Intent();
        				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.WallpaperActivity"));
        				        	intent.putExtra("conversationName", conversationName);
        				        	intent.putExtra("width", width);
        				        	intent.putExtra("height", height);
        				        	context.startActivity(intent);
                		        }
                		    }).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                		        public void onClick(DialogInterface dialog, int whichButton) {
                					String pathName = Environment.getExternalStorageDirectory().toString()+"/WhatsApp/Media/WallPaper/xposed_"+conversationName+".jpg";
                					File file = new File(pathName);
                					boolean deleted = file.delete();
                					if(deleted) Toast.makeText(context, "Wallpaper successfully deleted", Toast.LENGTH_SHORT).show();
                		        }
                		    }).show();
            		 	}else{
	            		 	Intent intent = new Intent();
				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.WallpaperActivity"));
				        	intent.putExtra("conversationName", conversationName);
				        	intent.putExtra("width", width);
				        	intent.putExtra("height", height);
				        	context.startActivity(intent);
            		 	}
            	 }else if(item.getItemId()==3){
            		 	final EditText input = new EditText(context);
            		 	new AlertDialog.Builder(context)
            		    .setTitle("Set replace text")
            		    .setView(input)
            		    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            		        public void onClick(DialogInterface dialog, int whichButton) {
            		            replaceText = input.getText().toString().trim(); 
            		        }
            		    }).setNegativeButton("Disable", new DialogInterface.OnClickListener() {
            		        public void onClick(DialogInterface dialog, int whichButton) {
            		            replaceText = "";
            		        }
            		    }).show();
            	 }else if(item.getItemId()==4){
         		 		scramble = !scramble;
         		 		if(scramble){
         		 			Toast.makeText(context, "Scramble Text enabled", Toast.LENGTH_SHORT).show();
         		 		}else{
         		 			Toast.makeText(context, "Scramble Text disabled", Toast.LENGTH_SHORT).show();
         		 		}
            	 }else if(item.getItemId()==5){
            		 	Intent intent = new Intent();
			        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.HidePreviewService"));
			        	intent.putExtra("contact", conversationName);
			        	intent.putExtra("isGroup", !contact.equals(conversationName));
			        	context.startService(intent);
			        	if(hide){
			        		Toast.makeText(context, "Show Prview", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(context, "Hide Prview", Toast.LENGTH_SHORT).show();
						}
            	 }else if(item.getItemId()==6){
            		 	Intent intent = new Intent();
			         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
			         	intent.putExtra("contact", conversationName);
			        	context.startService(intent);
            	 }else if(item.getItemId()==7){
            		 	Intent intent = new Intent();
			         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.StatsActivity"));
			         	intent.putExtra("conversationName", conversationName);
			        	context.startActivity(intent);
            	 }
            	 
            	 return true;
             }
            });
		popup.show();
	}
}
