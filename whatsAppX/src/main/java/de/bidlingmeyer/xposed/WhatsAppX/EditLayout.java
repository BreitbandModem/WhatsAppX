package de.bidlingmeyer.xposed.WhatsAppX;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
	
	String MODULE_PATH, conversationName="", jid="", message="", layoutTime="", contact="", replaceText="", notificationText="";
	XModuleResources modRes;
	FrameLayout cameraLayout, conversationsLayout;
	int width, height;
	boolean scramble = false, handleLocked = false, conversationsScreen = false, previewIsGroup = false, previewHide = false, hideNextNotification = false, hideNotification, contactLocked, hasWallpaper, fromConversations;
	RelativeLayout conversationLayout;
	ImageButton lockButton, settingsButton, starButton, phoneButton;
	long actionbarLoad, resumeCreate = 0;
	ArrayList<String> previewContacts;
	ArrayList<Integer> previewContactsIsGroup;
    private static Map<String, String> contacts = new HashMap<String, String>();
    
	@Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.whatsapp"))
            return;
        
        final XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		if(!prefs.getBoolean("sqlite3", true))
			return;
		
		XSharedPreferences prefs2 = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "contactsJid");
		prefs2.makeWorldReadable();
		contacts = (Map<String, String>) prefs2.getAll();
		
		notificationText = prefs.getString("notificationText", "");
		
        @SuppressWarnings("rawtypes")
		final Class conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", lpparam.classLoader);
        
        @SuppressWarnings("rawtypes")
		final Class conversationsClass = XposedHelpers.findClass("com.whatsapp.ConversationsFragment", lpparam.classLoader);
        
        @SuppressWarnings("rawtypes")
		final Class wallpaperClass = XposedHelpers.findClass("com.whatsapp.wallpaper.WallPaperView", lpparam.classLoader);
        
        @SuppressWarnings("rawtypes")
		final Class textClass = XposedHelpers.findClass("com.whatsapp.TextEmojiLabel", lpparam.classLoader);
        
        /*
        //notification
        XposedHelpers.findAndHookMethod(android.app.Notification.Builder.class, "setContentText", CharSequence.class, new XC_MethodHook(){
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            	String text;
            	if(((CharSequence) param.args[0]) != null)
            		text = ((CharSequence) param.args[0]).toString().trim();
            	else
            		return;
            	XposedBridge.log("notification text: "+text);
            	
            	if(hideNextNotification){
            		XposedBridge.log("parameter resetted");
            		param.args[0]=notificationText;
            		hideNextNotification = false;
            	}
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            	XposedBridge.log("parameter check: "+param.args[0]);
            	android.app.Notification.Builder builder = (Builder) param.getResult();
            	if(builder != null)
            		if(prefs.getBoolean("ticker", false))
            			builder.setTicker((CharSequence) param.args[0]);
            }            
        });
        XposedHelpers.findAndHookMethod(android.app.Notification.Builder.class, "setContentTitle", CharSequence.class, new XC_MethodHook(){
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            	String title;
            	if(((CharSequence) param.args[0]) != null)
            		title = ((CharSequence) param.args[0]).toString().trim();
            	else
            		return;
            	XposedBridge.log("notification title: "+title);
            	
            	title = title.contains("@")? title.split("@")[0].trim() : title;
            	XposedBridge.log("message from: "+title);
            	XSharedPreferences s = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "hideNotification");
				s.makeWorldReadable();
				Map<String, ?> allEntries = s.getAll();
				if(allEntries.containsKey(title)){
					hideNextNotification = true;
					XposedBridge.log("hideNextNotification = true");
				}else{
					hideNextNotification = false;
				}
            }
        });*/
       
        
        //Get contact jid, name
        XposedHelpers.findAndHookMethod(Intent.class, "getStringExtra", String.class, new XC_MethodHook(){
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            	String result = (String) param.getResult();
            	if(result != null)
	            	if(result.contains("@")){
		              	jid = result;
		              	conversationName = contacts.get(jid);
		              	if(conversationName == null){
		              		Intent intent = new Intent();
		    	        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.RefreshContactsService"));
		    	        	if(conversationLayout != null){
		    	        		conversationsLayout.getContext().startService(intent);
		    	        	}
		              		conversationName = jid.split("@")[0];
		              	}
		              	else if(conversationName.length() < 1)
		              		conversationName = jid.split("@")[0];
	            	}
            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreateOptionsMenu", Menu.class, new XC_MethodHook(){
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            	int choice = prefs.getInt("menuPhone", -20);
            	final Menu menu = (Menu) param.args[0];
				final MenuItem callMenu = menu.getItem(0);
            	if(callMenu.getTitle().equals("Call")){
            		if(choice == 1){
	            		menu.removeItem(callMenu.getItemId());
	            	}else if(choice == 2){
	            		callMenu.setOnMenuItemClickListener(new OnMenuItemClickListener(){
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								XposedBridge.log("onItemClick");
								String telNum = jid.split("@")[0];
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.setData(Uri.parse("tel:+"+telNum));
								conversationLayout.getContext().startActivity(intent);
								return true;
							}
		            	});
						callMenu.getActionView().setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								XposedBridge.log("actionViewOnClick");
								String telNum = jid.split("@")[0];
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.setData(Uri.parse("tel:+"+telNum));
								conversationLayout.getContext().startActivity(intent);
							}
						});
					}
            	}
            }
        });
        
        //fix keyboard enter key
        @SuppressWarnings("rawtypes")
		final Class entryClass = XposedHelpers.findClass("com.whatsapp.ConversationTextEntry", lpparam.classLoader);
        
        if(prefs.getBoolean("keyboard", false)){
	        XposedHelpers.findAndHookMethod(entryClass, "setInputEnterSend", boolean.class, new XC_MethodHook(){
	            @Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
	            	param.args[0] = false;
	            }
	        });
        }
        
        /*Encryption:
        XposedBridge.hookAllConstructors(entryClass, new XC_MethodHook(){
        	@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            	textEntry = (EditText) param.thisObject;
            }
        });
        XposedHelpers.findAndHookMethod(android.widget.ImageView.class, "setImageDrawable", Drawable.class, new XC_MethodHook(){
        	@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        		ImageButton sendButton;
        		try{
        			sendButton = (ImageButton) param.thisObject;
        		}catch(Exception e){
        			return;
        		}
         		sendButton.setOnTouchListener(new OnTouchListener()
 				{
 					@SuppressLint("ClickableViewAccessibility")
 					@Override
 					public boolean onTouch(View view, MotionEvent event) {
 						//on send button press
 						return false;
 					}
 				});
        	}
        });*/
        
        
        //Hide toast from selfie fail
        XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				View layout = ((Toast) param.thisObject).getView();
				TextView view = (TextView) layout.findViewById(android.R.id.message);
				if(view.getText().toString().equals("sorry, whatsappX fail^^")){
					Intent intent = new Intent();
					intent.setAction("de.bidlingmeyer.xposed.WhatsAppX.cannot");
					layout.getContext().sendBroadcast(intent);
					param.setResult(null);
				}
			}
        });
        
        //hide message preview, scramble/replace text, decrypt
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
			    
			    if(conversationsScreen && previewContacts.size() > 0){

				    if(previewIsGroup){
				    	if( ! text.trim().contains(":")){
				    		previewHide = false;
				    	}
				    	param.args[0] = "";
				    	previewIsGroup = false;
				    }else if(previewHide){
	   					param.args[0] = "";
	   					previewHide = false;
	   				}else{
	   					int index = previewContacts.indexOf(text.trim());

	   					if(index >= 0){
	   						previewIsGroup = previewContactsIsGroup.get(index) == 2; //is group
	   						previewHide = true;
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
        
        
        //set background picture
        XposedHelpers.findAndHookMethod(wallpaperClass, "setDrawable", Drawable.class, new XC_MethodHook(){

			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				if(jid.length() < 1)
					return;
			    
				String pathName = Environment.getExternalStorageDirectory().toString()+"/WhatsApp/Media/WallPaper/xposed_"+jid.split("@")[0]+".jpg";
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
				hideNotification = false;
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
				jid="";
				resumeCreate = 0;
				conversationLayout = null;
				handleLocked = false;
				contactLocked = false;
				hideNotification = false;
				actionbarLoad = 0;
				hasWallpaper = false;
				fromConversations = false;
			}
        });
        
        
        
        XposedHelpers.findAndHookMethod(conversationsClass, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook(){
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
	        	if(conversationsLayout != null) conversationsLayout.getContext().startService(intent);
			}
        });
    }
	
	
	//layout hacks;
	@Override
	public void handleInitPackageResources(final InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals("com.whatsapp")) {
            return;
        }
		modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

		final XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		if(!prefs.getBoolean("sqlite3", true))
			return;
		
		if(prefs.getBoolean("selfie", true)){
			resparam.res.setReplacement("com.whatsapp", "drawable", "input_cam", modRes.fwd(R.drawable.ic_star));
			resparam.res.setReplacement("com.whatsapp", "string", "cannot_start_camera", "sorry, whatsappX fail^^");
		}
		//resparam.res.setReplacement("com.whatsapp", "integer", "abc_max_action_buttons", 1);
		
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
					intent.putExtra("jid", jid);
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
					conversationsLayout = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("conversations_empty", "id", "com.whatsapp"));
					Intent intent = new Intent();
		         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
		        	conversationsLayout.getContext().startService(intent);
			}
		});

		//conversation
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				conversationLayout = (RelativeLayout) liparam.view.findViewById(liparam.res.getIdentifier("conversation_layout", "id", "com.whatsapp"));
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
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Helper.convertDp(layout.getContext(), prefs.getInt("size", 30)), LayoutParams.MATCH_PARENT);
				final int color = prefs.getInt("color", Color.WHITE);
				XResForwarder x=null;
				
				if(prefs.getBoolean("gear", true)){
				settingsButton = new ImageButton(layout.getContext());
				settingsButton.setBackgroundColor(Color.TRANSPARENT);
				settingsButton.setOnTouchListener(new OnTouchListener(){
					@SuppressLint("ClickableViewAccessibility")
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if(event.getAction() == MotionEvent.ACTION_DOWN){
							settingsButton.setBackgroundColor(Color.LTGRAY);
						}else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
							settingsButton.setBackgroundColor(Color.TRANSPARENT);
						}
						return false;
					}
				});
				settingsButton.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						showDialog(conversationLayout.getContext()/*layout.getContext()*/, v, true);
					}
				});
				x = modRes.fwd(R.drawable.ic_settings);
			    settingsButton.setLayoutParams(params);
			    settingsButton.setPadding(i, 0, i, 0);
			    settingsButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			    settingsButton.setAdjustViewBounds(true);
			    Drawable d;
			    if(color == 0){
			    	d = new ColorDrawable(Color.TRANSPARENT);
			    }else{
			    	d = x.getResources().getDrawable(x.getId());
				    Mode mMode = Mode.SRC_ATOP;
				    d.setColorFilter(color,mMode);
			    }
			    
			    //d.setColorFilter(new LightingColorFilter( color, color ));
			    settingsButton.setImageDrawable(d);
				layout.addView(settingsButton);
				}
				
				if(prefs.getBoolean("lock", false)){
				lockButton = new ImageButton(layout.getContext());
				
			    lockButton.setBackgroundColor(Color.TRANSPARENT);
			    lockButton.setOnTouchListener(new OnTouchListener(){
			    	@SuppressLint("ClickableViewAccessibility")
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if(event.getAction() == MotionEvent.ACTION_DOWN){
							lockButton.setBackgroundColor(Color.LTGRAY);
						}else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
							lockButton.setBackgroundColor(Color.TRANSPARENT);
						}
						return false;
					}
				});
			    lockButton.setOnClickListener(new OnClickListener(){
					@SuppressLint("ClickableViewAccessibility")
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
			    Drawable d;
			    if(color == 0){
			    	d = new ColorDrawable(Color.TRANSPARENT);
			    }else{
			    	d = x.getResources().getDrawable(x.getId());
				    Mode mMode = Mode.SRC_ATOP;
				    d.setColorFilter(color,mMode);
			    }
			    lockButton.setImageDrawable(d);
				layout.addView(lockButton);
				}
				
				
				if(prefs.getBoolean("reminder", true)){
				final ImageButton remindButton = new ImageButton(layout.getContext());
				remindButton.setBackgroundColor(Color.TRANSPARENT);
				remindButton.setOnTouchListener(new OnTouchListener(){
					@SuppressLint("ClickableViewAccessibility")
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if(event.getAction() == MotionEvent.ACTION_DOWN){
							remindButton.setBackgroundColor(Color.LTGRAY);
						}else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
							remindButton.setBackgroundColor(Color.TRANSPARENT);
						}
						return false;
					}
				});
				remindButton.setOnClickListener(new OnClickListener(){
					@SuppressLint("ClickableViewAccessibility")
					@Override
					public void onClick(View v) {
						if(conversationName.length() > 0 && !scramble && replaceText.length() == 0){
							Intent intent = new Intent();
				         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
				         	intent.putExtra("contact", conversationName);
				         	intent.putExtra("jid", jid);
				        	layout.getContext().startService(intent);
						}
					}
				});
				
				remindButton.setLayoutParams(params);
			    remindButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			    remindButton.setAdjustViewBounds(true);
			    remindButton.setPadding(i, 0, i, 0);
			    x = modRes.fwd(R.drawable.ic_reminder);
			    Drawable d;
			    if(color == 0){
			    	d = new ColorDrawable(Color.TRANSPARENT);
			    }else{
			    	d = x.getResources().getDrawable(x.getId());
				    Mode mMode = Mode.SRC_ATOP;
				    d.setColorFilter(color,mMode);
			    }
			    remindButton.setImageDrawable(d);
				layout.addView(remindButton);
				}
				
				if(prefs.getBoolean("star", false)){
					starButton = new ImageButton(layout.getContext());
					starButton.setBackgroundColor(Color.TRANSPARENT);
					starButton.setOnTouchListener(new OnTouchListener(){
						@SuppressLint("ClickableViewAccessibility")
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if(event.getAction() == MotionEvent.ACTION_DOWN){
								starButton.setBackgroundColor(Color.LTGRAY);
							}else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
								starButton.setBackgroundColor(Color.TRANSPARENT);
							}
							return false;
						}
					});
					starButton.setOnClickListener(new OnClickListener(){
						@SuppressLint("ClickableViewAccessibility")
						@Override
						public void onClick(View v) {
							Intent intent = new Intent();
							intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.PagerActivity"));//startet die pager activity
							intent.putExtra("conversationName", conversationName);
							intent.putExtra("whatsapp", true);
							intent.putExtra("jid", jid);
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
				    Drawable d;
				    if(color == 0){
				    	d = new ColorDrawable(Color.TRANSPARENT);
				    }else{
				    	d = x.getResources().getDrawable(x.getId());
					    Mode mMode = Mode.SRC_ATOP;
					    d.setColorFilter(color,mMode);
				    }
				    starButton.setImageDrawable(d);
					layout.addView(starButton);
				}
				
				final String phoneNumber = jid;
				if(!phoneNumber.contains("@g.us")){
					if(prefs.getBoolean("phone", true)){
						phoneButton = new ImageButton(layout.getContext());
						phoneButton.setBackgroundColor(Color.TRANSPARENT);
						phoneButton.setOnTouchListener(new OnTouchListener(){
							@SuppressLint("ClickableViewAccessibility")
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								if(event.getAction() == MotionEvent.ACTION_DOWN){
									phoneButton.setBackgroundColor(Color.LTGRAY);
								}else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
									phoneButton.setBackgroundColor(Color.TRANSPARENT);
								}
								return false;
							}
						});
						phoneButton.setOnClickListener(new OnClickListener(){
							@SuppressLint("ClickableViewAccessibility")
							@Override
							public void onClick(View v) {
								//start phone
								
								String telNum = phoneNumber.split("@")[0];
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.setData(Uri.parse("tel:+"+telNum));
								layout.getContext().startActivity(intent);
							}
						});
						x = modRes.fwd(R.drawable.ic_audio_phone_am);
					    phoneButton.setLayoutParams(params);
					    phoneButton.setPadding(i, 0, i, 0);
					    phoneButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
					    phoneButton.setAdjustViewBounds(true);
					    Drawable d;
					    if(color == 0){
					    	d = new ColorDrawable(Color.TRANSPARENT);
					    }else{
					    	d = x.getResources().getDrawable(x.getId());
						    Mode mMode = Mode.SRC_ATOP;
						    d.setColorFilter(color,mMode);
					    }
					    phoneButton.setImageDrawable(d);
						layout.addView(phoneButton);
					}
				}
			}
		 });
		
		//Text Messages Left
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation_row_text_left", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("main_layout", "id", "com.whatsapp"));
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
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation_row_text_right", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("main_layout", "id", "com.whatsapp"));
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
		
		XSharedPreferences prefsNotif = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "hideNotification");
		prefsNotif.makeWorldReadable();
		hideNotification = prefsNotif.getBoolean(conversationName, false);
		
		XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "locked");
		prefs.makeWorldReadable();
   		XResForwarder x = null;
   		
   		long time = prefs.getLong(conversationName, -1);
   		if(time < 0){
   			x = modRes.fwd(R.drawable.ic_unlocked);
			conversationLayout.setVisibility(View.VISIBLE);
			contactLocked = false;
   		}else if((System.currentTimeMillis() - time) < 5000){
   			x = modRes.fwd(R.drawable.ic_locked);
			conversationLayout.setVisibility(View.VISIBLE);
			contactLocked = true;
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
		XSharedPreferences notPrefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "notification");
		notPrefs.makeWorldReadable();
		boolean reminderActive = notPrefs.getInt(jid, 0) > 0;
		PopupMenu popup = new PopupMenu(context, v);
		boolean tmpBool = false;
    	loadPreviewContacts();
		if(previewContacts != null){
			tmpBool = previewContacts.contains(conversationName);
		}
		final boolean hide = tmpBool;
		
		if(conversationName.length() > 0 && !scramble && replaceText.length() == 0){

			if(conversationName.length() > 0 && message.length() > 0 && layoutTime.length() > 0 && prefs.getBoolean("favorites", true) && !fromGear){
				popup.getMenu().add(Menu.NONE, 0, 0, "Add to Favorites");
			}

			if(prefs.getBoolean("click", false) || fromGear){
				popup.getMenu().add(Menu.NONE, 9, 3, "Highlight contact");
				
				/*MenuItem notificationItem = popup.getMenu().add(Menu.NONE, 10, 5, "Hide notification text");
				notificationItem.setCheckable(true);
				if(hideNotification){
					//item = popup.getMenu().add(Menu.NONE, 10, Menu.NONE, "show notification text");
					notificationItem.setChecked(true);
				}else{
					//item = popup.getMenu().add(Menu.NONE, 10, Menu.NONE, "hide notification text");
					notificationItem.setChecked(false);
				}*/
				
				if(!prefs.getBoolean("reminder", true)){
					MenuItem reminderItem = popup.getMenu().add(Menu.NONE, 6, 1, "Set Reminder");
					reminderItem.setCheckable(true);
					if(reminderActive)
						reminderItem.setChecked(true);
					else
						reminderItem.setChecked(false);
				}
				
				if(!prefs.getBoolean("lock", false)){
					MenuItem lockItem = popup.getMenu().add(Menu.NONE, 1, 4, "Lock Contact");
					lockItem.setCheckable(true);
					if(contactLocked){
						//popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "Unlock Contact");
						lockItem.setChecked(true);
					}else{
						//popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "Lock Contact");
						lockItem.setChecked(false);
					}
				}
				if(!prefs.getBoolean("star", false)){
					popup.getMenu().add(Menu.NONE, 8, 7, "Open WhatsAppX");
				}
					if(hasWallpaper){
						popup.getMenu().add(Menu.NONE, 2, 2, "Set/Delete Wallpaper");
					}else{
						popup.getMenu().add(Menu.NONE, 2, 2, "Set Wallpaper");
					}
				
					MenuItem previewItem = popup.getMenu().add(Menu.NONE, 5, 6, "Hide Message Preview");
					previewItem.setCheckable(true);
					if(hide){
						//popup.getMenu().add(Menu.NONE, 5, Menu.NONE, "Show Message Preview");
						previewItem.setChecked(true);
					}else{
						//popup.getMenu().add(Menu.NONE, 5, Menu.NONE, "Hide Message Preview");
						previewItem.setChecked(false);
					}
					
					popup.getMenu().add(Menu.NONE, 7, 8, "Show Stats");
			}
		}
		if(prefs.getBoolean("click", false) || fromGear){
			MenuItem scrambleItem = popup.getMenu().add(Menu.NONE, 4, 10, "Scramble Text");
			scrambleItem.setCheckable(true);
			if(scramble){
				//popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "Disable Scramble Text");
				scrambleItem.setChecked(true);
			}else{
				//popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "Enable Scramble Text");
				scrambleItem.setChecked(false);
			}
			MenuItem replaceItem = popup.getMenu().add(Menu.NONE, 3, 9, "Replace Words");
			replaceItem.setCheckable(true);
			if(replaceText.length() > 0){
				//popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "Cancel Replace Words With");
				replaceItem.setChecked(true);
			}else{
				//popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "Replace Words With");
				replaceItem.setChecked(false);
			}
		}
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {  
             @Override
			public boolean onMenuItemClick(final MenuItem item) {
            	 switch(item.getItemId()){
            	 case 0 :
            		 	Intent intent = new Intent();
			        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.SaveInfoService"));
			        	intent.putExtra("message", message);
			        	intent.putExtra("conversationName", conversationName);
			        	intent.putExtra("layoutTime", layoutTime);
			        	intent.putExtra("contact", contact);
			        	intent.putExtra("jid", jid);
			        	context.startService(intent);
			        	Toast.makeText(context, "Message added to Favorites", Toast.LENGTH_SHORT).show();
			        	return true;
            	 case 1 :
            		 	XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "locked");
						prefs.makeWorldReadable();
						long locked = prefs.getLong(conversationName, -1);
						if(locked >= 0){//unlock contact:
							intent = new Intent();
				         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.LockContactService"));
				         	intent.putExtra("contact", conversationName);
				         	intent.putExtra("lock", false);
				        	context.startService(intent);
				        	conversationLayout.setVisibility(View.VISIBLE);
							XResForwarder x = modRes.fwd(R.drawable.ic_unlocked);
							if(lockButton != null) lockButton.setImageDrawable(x.getResources().getDrawable(x.getId()));
							contactLocked = false;
							item.setChecked(false);
						}else if(locked < 0){//lock contact:
							intent = new Intent();
				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.PinActivity"));
				        	intent.putExtra("contact", conversationName);
				        	intent.putExtra("set", true);
				        	context.startActivity(intent);
				        	item.setChecked(true);
						}
						keepMenuOpen(item, context);
						return false;
            	 case 2 :
            		 	if(hasWallpaper){
            		 		new AlertDialog.Builder(context)
                		    .setTitle("Delete or Set a new Wallpaper")
                		    .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                		        public void onClick(DialogInterface dialog, int whichButton) {
                		        	Intent intent = new Intent();
        				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.WallpaperActivity"));
        				        	intent.putExtra("jid", jid);
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
	            		 	intent = new Intent();
				        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.WallpaperActivity"));
				        	intent.putExtra("jid", jid);
				        	intent.putExtra("width", width);
				        	intent.putExtra("height", height);
				        	context.startActivity(intent);
            		 	}
            		 	return true;
            	 case 3 :
            		 	final EditText input = new EditText(context);
            		 	new AlertDialog.Builder(context)
            		    .setTitle("Set replace text")
            		    .setView(input)
            		    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            		        public void onClick(DialogInterface dialog, int whichButton) {
            		            replaceText = input.getText().toString().trim(); 
            		            item.setChecked(true);
            		        }
            		    }).setNegativeButton("Disable", new DialogInterface.OnClickListener() {
            		        public void onClick(DialogInterface dialog, int whichButton) {
            		            replaceText = "";
            		            item.setChecked(false);
            		        }
            		    }).show();
            		 	keepMenuOpen(item, context);
            		 	return false;
            	 case 4 :
         		 		scramble = !scramble;
         		 		if(scramble){
         		 			Toast.makeText(context, "Scramble Text enabled", Toast.LENGTH_SHORT).show();
         		 			//item.setTitle("Disable Scramble Text");
         		 			item.setChecked(true);
         		 		}else{
         		 			Toast.makeText(context, "Scramble Text disabled", Toast.LENGTH_SHORT).show();
         		 			//item.setTitle("Enable Scramble Text");
         		 			item.setChecked(false);
         		 		}
         		 		keepMenuOpen(item, context);
         		 		return false;
            	 case 5 :
            		 	intent = new Intent();
			        	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.HidePreviewService"));
			        	intent.putExtra("contact", conversationName);
			        	intent.putExtra("isGroup", !contact.equals(conversationName));
			        	context.startService(intent);
			        	if(hide){
			        		Toast.makeText(context, "Show Preview", Toast.LENGTH_SHORT).show();
			        		item.setChecked(false);
			        		//item.setTitle("Hide Message Preview");
						}else{
							Toast.makeText(context, "Hide Preview", Toast.LENGTH_SHORT).show();
							item.setChecked(true);
							//item.setTitle("Show Message Preview");
						}
			        	keepMenuOpen(item, context);
			        	return false;
            	 case 6 :
            		 	intent = new Intent();
			         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
			         	intent.putExtra("contact", conversationName);
			         	intent.putExtra("jid", jid);
			        	context.startService(intent);
			        	item.setChecked(!item.isChecked());
			        	keepMenuOpen(item, context);
			        	return false;
            	 case 7 :
            		 	intent = new Intent();
			         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.StatsActivity"));
			         	intent.putExtra("jid", jid);
			        	context.startActivity(intent);
			        	return true;
            	 case 8 :
            		 	intent = new Intent();
	 					intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.PagerActivity"));//startet die pager activity
	 					intent.putExtra("conversationName", conversationName); 
	 					intent.putExtra("whatsapp", true);
	 					intent.putExtra("jid", jid);
	 					int i, k;
	 					Resources res = context.getResources();
	 					i = res.getIdentifier("slide_out_left", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
	 					k = res.getIdentifier("slide_in_right", "anim", "de.bidlingmeyer.xposed.WhatsAppX");
	 					ActivityOptions opts = ActivityOptions.makeCustomAnimation(context, k, i);
	 					context.startActivity(intent, opts.toBundle());	
	 					return true;
            	 case 9:
         		 		intent = new Intent();
	 					intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.ColorActivity"));
	 					intent.putExtra("jid", jid);
	 					context.startActivity(intent);	
	 					return true;
            	 case 10 :
         		 		intent = new Intent();
	 					intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.HideNotificationService"));
	 					intent.putExtra("contact", conversationName);
	 					context.startService(intent);	
	 					if(hideNotification){
	 						Toast.makeText(context, "notifications will be shown", Toast.LENGTH_SHORT).show();
	 						//item.setTitle("hide notification text");
	 						item.setChecked(false);
	 					}else{
	 						Toast.makeText(context, "notifications will be hidden", Toast.LENGTH_SHORT).show();
	 						//item.setTitle("show notification text");
	 						item.setChecked(true);
	 					}
	 					hideNotification = !hideNotification;
	 					keepMenuOpen(item, context);
	 					return false;
            	 }
            	 return true;
             }
            });
		popup.show();
	}
	
	public void keepMenuOpen(MenuItem item, Context context){
		 // Keep the popup menu open
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(context));
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return false;
            }
        });
	}
}
