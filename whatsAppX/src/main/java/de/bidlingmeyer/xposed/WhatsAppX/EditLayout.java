package de.bidlingmeyer.xposed.WhatsAppX;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResForwarder;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SubMenu;
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
import de.robv.android.xposed.XC_MethodReplacement;
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
	Activity conversationActivity, mainActivity;

	boolean quickReplyPref, gearPref, selfiePref, lockPref, reminderPref, starPref, phonePref, favoritesPref, clickPref, keyboardPref;
	int menuPhonePref, sizePref, colorPref;

	public void loadPrefs(XSharedPreferences prefs){
		quickReplyPref = prefs.getBoolean("quickReply", true);
		gearPref = prefs.getBoolean("gear", true);
		selfiePref = prefs.getBoolean("selfie", true);
		lockPref = prefs.getBoolean("lock", true);
		reminderPref = prefs.getBoolean("reminder", true);
		starPref = prefs.getBoolean("star", true);
		phonePref = prefs.getBoolean("phone", true);
		favoritesPref = prefs.getBoolean("favorites", true);
		clickPref = prefs.getBoolean("click", true);
		keyboardPref = prefs.getBoolean("keyboard", true);

		menuPhonePref = prefs.getInt("menuPhone", 0);
		sizePref = prefs.getInt("size", 0);
		colorPref = prefs.getInt("color", 0);

		notificationText = prefs.getString("notificationText", "");
	}

	@Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.whatsapp"))
            return;

		XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		if(!prefs.getBoolean("sqlite3", false))
			return;
		loadPrefs(prefs);
		
		XSharedPreferences prefs2 = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "contactsJid");
		prefs2.makeWorldReadable();
		contacts = (Map<String, String>) prefs2.getAll();
		
        @SuppressWarnings("rawtypes")
		final Class conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", lpparam.classLoader);

		@SuppressWarnings("rawtypes")
		final Class bootReceiverClass = XposedHelpers.findClass("com.whatsapp.BootReceiver", lpparam.classLoader);

		@SuppressWarnings("rawtypes")
		final Class conversationsClass = XposedHelpers.findClass("com.whatsapp.ConversationsFragment", lpparam.classLoader);
		@SuppressWarnings("rawtypes")
		final Class popupNotificationClass = XposedHelpers.findClass("com.whatsapp.notification.PopupNotification", lpparam.classLoader);
        @SuppressWarnings("rawtypes")
		final Class wallpaperClass = XposedHelpers.findClass("com.whatsapp.wallpaper.WallPaperView", lpparam.classLoader);
		@SuppressWarnings("rawtypes")
		final Class settingsNotificationsClass = XposedHelpers.findClass("com.whatsapp.SettingsNotifications", lpparam.classLoader);
        @SuppressWarnings("rawtypes")
		final Class textClass = XposedHelpers.findClass("com.whatsapp.TextEmojiLabel", lpparam.classLoader);

		/*if(quickReplyPref)
			XposedBridge.hookAllMethods(android.database.sqlite.SQLiteDatabase.class, "query", new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if(param.args == null)
						return;
					if(param.args[0].equals("settings") || param.args[1].equals("settings")) {
						Cursor c = (Cursor) param.getResult();
						c.moveToFirst();
						while (c.isAfterLast() == false) {
							XposedBridge.log("jid: " + c.getString(1) + " popup: " + c.getString(8));
							c.moveToNext();
						}
					}
				}
			});
		/*if(quickReplyPref)
			XposedBridge.hookAllMethods(android.database.sqlite.SQLiteDatabase.class, "rawQuery", new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if(param.args == null)
						return;
					XposedBridge.log("rawQuery: "+param.args[0]);
					String [] selectionArgs = (String[]) param.args[1];
					for(int i=0; i<selectionArgs.length; i++)
						XposedBridge.log("rawQuery selection "+i+": "+selectionArgs[0]);
					XposedBridge.log("query Cursor:"+param.getResult());
				}
			});*/
		if(quickReplyPref)
		XposedHelpers.findAndHookMethod(popupNotificationClass, "onCreate", Bundle.class, new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Activity popupNotificationActivity = (Activity) param.thisObject;
				Intent i = popupNotificationActivity.getIntent();
				if(i.getBooleanExtra("WhatsAppX", false)){
					//XposedBridge.log("WhatsAppX popup");
				}else{
					popupNotificationActivity.finish();
				}
			}
		});

		if(quickReplyPref)
		XposedHelpers.findAndHookConstructor(android.app.Notification.Builder.class, Context.class, new XC_MethodHook(){
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				android.app.Notification.Builder builder = (Notification.Builder) param.thisObject;

				Intent i = new Intent();
				i.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.notification.PopupNotification"));
				i.setFlags(268697600);
				i.putExtra("WhatsAppX", true);
				PendingIntent pi = PendingIntent.getActivity((Context) param.args[0], 99999, i, 0);
				builder.addAction(0, "QUICK REPLY", pi);
			}
		});

		if(quickReplyPref)
			XposedHelpers.findAndHookMethod(settingsNotificationsClass, "onCreateOptionsMenu", Menu.class, new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Menu menu = (Menu) param.args[0];
					/*MenuBuilder mb = (MenuBuilder) menu.getItem(0);
					for(int i=0; i<20; i++)
						if (mb.getItem(i)!= null)
							XposedBridge.log("menu item: "+mb.getItem(i).getTitle());*/
				}
			});
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
            	
            	if(hideNextNotification){
            		param.args[0]=notificationText;
            		hideNextNotification = false;
            	}
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
            	
            	title = title.contains("@")? title.split("@")[0].trim() : title;
            	XSharedPreferences s = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "hideNotification");
				s.makeWorldReadable();
				Map<String, ?> allEntries = s.getAll();
				if(allEntries.containsKey(title)){
					hideNextNotification = true;
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
		    	        	if(conversationsLayout != null){
		    	        		conversationsLayout.getContext().startService(intent);
		    	        	}
		              		conversationName = jid.split("@")[0];
		              	}
		              	else if(conversationName.length() < 1)
		              		conversationName = jid.split("@")[0];
	            	}
            }
        });

		XposedHelpers.findAndHookMethod(conversationClass, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Activity conversationActivity = (Activity) param.thisObject;
				MenuItem item = (MenuItem) param.args[0];
				boolean tmpBool = false;
				if (previewContacts != null) {
					tmpBool = previewContacts.contains(conversationName);
				}
				final boolean hide = tmpBool;
				menuClick(conversationActivity, item, hide);
			}
		});
        XposedHelpers.findAndHookMethod(conversationClass, "onCreateOptionsMenu", Menu.class, new XC_MethodHook(){
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            	final Menu menu = (Menu) param.args[0];
				final MenuItem callMenu = menu.getItem(0);
            	if(callMenu.getTitle().equals("Call")){
            		if(menuPhonePref == 1){
	            		menu.removeItem(callMenu.getItemId());
	            	}else if(menuPhonePref == 2){
	            		callMenu.setOnMenuItemClickListener(new OnMenuItemClickListener(){
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								String telNum = jid.split("@")[0];
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.setData(Uri.parse("tel:+"+telNum));
								conversationLayout.getContext().startActivity(intent);
								return true;
							}
		            	});

						final String tmpJid = jid;
						final RelativeLayout tmpLayout = conversationLayout;
						callMenu.getActionView().setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								String telNum = tmpJid.split("@")[0];
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.setData(Uri.parse("tel:+"+telNum));
								tmpLayout.getContext().startActivity(intent);
							}
						});
					}
            	}
				SubMenu item = menu.addSubMenu(99, 99, Menu.NONE, "WhatsappX");
				showDialog(conversationsLayout.getContext(), item, true);
            }
        });
        
        //fix keyboard enter key
        @SuppressWarnings("rawtypes")
		final Class entryClass = XposedHelpers.findClass("com.whatsapp.ConversationTextEntry", lpparam.classLoader);
        
        if(keyboardPref)
	        XposedHelpers.findAndHookMethod(entryClass, "setInputEnterSend", boolean.class, new XC_MethodHook(){
	            @Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
	            	param.args[0] = false;
	            }
	        });
        
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
        
        //hide message preview, scramble/replace text
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
			/* Get Intent information:
			@Override
			protected void afterHookedMethod(MethodHookParam param)throws Throwable{
				conversationActivity = (Activity) param.thisObject;

				Intent i = conversationActivity.getIntent();
				XposedBridge.log("onCreate: "+i);
				Bundle bundle = i.getExtras();
				if( bundle != null) {
					for (String key : bundle.keySet()) {
						Object value = bundle.get(key);
						XposedBridge.log(String.format("extras: %s %s (%s)", key, value.toString(), value.getClass().getName()));
					}
				}
				XposedBridge.log("action: "+i.getAction()+"flags: "+i.getFlags()+"categories: "+i.getCategories()+"data: "
					+i.getData()+"component: "+i.getComponent()+"type: "+i.getType()+"clipData: "+i.getClipData()+"selector: "+i.getSelector());

				//call internal activity form external activity
				*/
        });
		XposedHelpers.findAndHookMethod(bootReceiverClass, "onReceive", Context.class, Intent.class, new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
				super.beforeHookedMethod(param);

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

		XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
		prefs.makeWorldReadable();
		if(!prefs.getBoolean("sqlite3", false))
			return;
		loadPrefs(prefs);
		
		if(selfiePref){
			resparam.res.setReplacement("com.whatsapp", "drawable", "input_cam", modRes.fwd(R.drawable.ic_star));
			resparam.res.setReplacement("com.whatsapp", "string", "cannot_start_camera", "sorry, whatsappX fail^^");
		}
		//resparam.res.setReplacement("com.whatsapp", "integer", "abc_max_action_buttons", 1);
		
		//camera
		if(selfiePref)
		resparam.res.hookLayout("com.whatsapp", "layout", "camera", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				if(!handleLocked){
					resumeCreate();
				}
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

				int i = Helper.convertDp(layout.getContext(), 5);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Helper.convertDp(layout.getContext(), sizePref), LayoutParams.MATCH_PARENT);
				XResForwarder x=null;
				
				if(gearPref){
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
			    if(colorPref == 0){
			    	d = new ColorDrawable(Color.TRANSPARENT);
			    }else{
			    	d = x.getResources().getDrawable(x.getId());
				    Mode mMode = Mode.SRC_ATOP;
				    d.setColorFilter(colorPref,mMode);
			    }
			    
			    //d.setColorFilter(new LightingColorFilter( color, color ));
			    settingsButton.setImageDrawable(d);
				layout.addView(settingsButton);
				}
				
				if(lockPref){
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
			    if(colorPref == 0){
			    	d = new ColorDrawable(Color.TRANSPARENT);
			    }else{
			    	d = x.getResources().getDrawable(x.getId());
				    Mode mMode = Mode.SRC_ATOP;
				    d.setColorFilter(colorPref,mMode);
			    }
			    lockButton.setImageDrawable(d);
				layout.addView(lockButton);
				}
				
				
				if(reminderPref){
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
				final String tmpJid = jid;
				final String tmpConversationName = conversationName;
				final LinearLayout tmpLayout = layout;
				remindButton.setOnClickListener(new OnClickListener(){
					@SuppressLint("ClickableViewAccessibility")
					@Override
					public void onClick(View v) {
						if(tmpConversationName.length() > 0 && !scramble && replaceText.length() == 0){
							Intent intent = new Intent();
				         	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
				         	intent.putExtra("contact", tmpConversationName);
				         	intent.putExtra("jid", tmpJid);
				        	tmpLayout.getContext().startService(intent);
						}
					}
				});
				remindButton.setLayoutParams(params);
			    remindButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			    remindButton.setAdjustViewBounds(true);
			    remindButton.setPadding(i, 0, i, 0);
			    x = modRes.fwd(R.drawable.ic_reminder);
			    Drawable d;
			    if(colorPref == 0){
			    	d = new ColorDrawable(Color.TRANSPARENT);
			    }else{
			    	d = x.getResources().getDrawable(x.getId());
				    Mode mMode = Mode.SRC_ATOP;
				    d.setColorFilter(colorPref,mMode);
			    }
			    remindButton.setImageDrawable(d);
				layout.addView(remindButton);
				}
				
				if(starPref){
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
				    if(colorPref == 0){
				    	d = new ColorDrawable(Color.TRANSPARENT);
				    }else{
				    	d = x.getResources().getDrawable(x.getId());
					    Mode mMode = Mode.SRC_ATOP;
					    d.setColorFilter(colorPref,mMode);
				    }
				    starButton.setImageDrawable(d);
					layout.addView(starButton);
				}
				
				final String phoneNumber = jid;
				if(!phoneNumber.contains("@g.us")){
					if(phonePref){
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
					    if(colorPref == 0){
					    	d = new ColorDrawable(Color.TRANSPARENT);
					    }else{
					    	d = x.getResources().getDrawable(x.getId());
						    Mode mMode = Mode.SRC_ATOP;
						    d.setColorFilter(colorPref,mMode);
					    }
					    phoneButton.setImageDrawable(d);
						layout.addView(phoneButton);
					}
				}
			}
		 });
		
		//Text Messages Left
		if(favoritesPref || clickPref)
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation_row_text_left", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("main_layout", "id", "com.whatsapp"));
				if(!handleLocked){
					resumeCreate();
				}
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
		if(favoritesPref || clickPref)
		resparam.res.hookLayout("com.whatsapp", "layout", "conversation_row_text_right", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
				final LinearLayout layout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("main_layout", "id", "com.whatsapp"));
				if(!handleLocked){
					resumeCreate();
				}

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
	public void addOptions(final Context context, Menu menu, boolean fromGear, boolean hide){
		XSharedPreferences notPrefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "notification");
		notPrefs.makeWorldReadable();
		boolean reminderActive = notPrefs.getInt(jid, 0) > 0;

		if(conversationName.length() > 0 && !scramble && replaceText.length() == 0){

			if(conversationName.length() > 0 && message.length() > 0 && layoutTime.length() > 0 && favoritesPref && !fromGear){
				menu.add(Menu.NONE, 100, 0, "Add to Favorites");
			}

			if(clickPref || fromGear){
				menu.add(Menu.NONE, 109, 3, "Highlight contact");

				/*MenuItem notificationItem = menu.add(Menu.NONE, 10, 5, "Hide notification text");
				notificationItem.setCheckable(true);
				if(hideNotification){
					//item = menu.add(Menu.NONE, 10, Menu.NONE, "show notification text");
					notificationItem.setChecked(true);
				}else{
					//item = menu.add(Menu.NONE, 10, Menu.NONE, "hide notification text");
					notificationItem.setChecked(false);
				}*/

				if(!reminderPref){
					MenuItem reminderItem = menu.add(Menu.NONE, 106, 1, "Set Reminder");
					reminderItem.setCheckable(true);
					if(reminderActive)
						reminderItem.setChecked(true);
					else
						reminderItem.setChecked(false);
				}

				if(!lockPref){
					MenuItem lockItem = menu.add(Menu.NONE, 101, 4, "Lock Contact");
					lockItem.setCheckable(true);
					if(contactLocked){
						//menu.add(Menu.NONE, 1, Menu.NONE, "Unlock Contact");
						lockItem.setChecked(true);
					}else{
						//menu.add(Menu.NONE, 1, Menu.NONE, "Lock Contact");
						lockItem.setChecked(false);
					}
				}
				if(!starPref){
					menu.add(Menu.NONE, 108, 7, "Open WhatsAppX");
				}
				if(hasWallpaper){
					menu.add(Menu.NONE, 102, 2, "Set/Delete Wallpaper");
				}else{
					menu.add(Menu.NONE, 102, 2, "Set Wallpaper");
				}

				MenuItem previewItem = menu.add(Menu.NONE, 105, 6, "Hide Message Preview");
				previewItem.setCheckable(true);
				if(hide){
					//menu.add(Menu.NONE, 5, Menu.NONE, "Show Message Preview");
					previewItem.setChecked(true);
				}else{
					//menu.add(Menu.NONE, 5, Menu.NONE, "Hide Message Preview");
					previewItem.setChecked(false);
				}

				menu.add(Menu.NONE, 107, 8, "Show Stats");
			}
		}
		if(clickPref || fromGear){
			MenuItem scrambleItem = menu.add(Menu.NONE, 104, 10, "Scramble Text");
			scrambleItem.setCheckable(true);
			if(scramble){
				//menu.add(Menu.NONE, 4, Menu.NONE, "Disable Scramble Text");
				scrambleItem.setChecked(true);
			}else{
				//menu.add(Menu.NONE, 4, Menu.NONE, "Enable Scramble Text");
				scrambleItem.setChecked(false);
			}
			MenuItem replaceItem = menu.add(Menu.NONE, 103, 9, "Replace Words");
			replaceItem.setCheckable(true);
			if(replaceText.length() > 0){
				//menu.add(Menu.NONE, 3, Menu.NONE, "Cancel Replace Words With");
				replaceItem.setChecked(true);
			}else{
				//menu.add(Menu.NONE, 3, Menu.NONE, "Replace Words With");
				replaceItem.setChecked(false);
			}
		}
	}
	public boolean menuClick(final Context context, final MenuItem item, boolean hide){
		switch(item.getItemId()){
			case 100 :
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
			case 101 :
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
			case 102 :
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
									String pathName = Environment.getExternalStorageDirectory().toString()+"/WhatsApp/Media/WallPaper/xposed_"+jid.split("@")[0]+".jpg";
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
			case 103 :
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
			case 104 :
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
			case 105 :
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
			case 106 :
				intent = new Intent();
				intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.NotificationService"));
				intent.putExtra("contact", conversationName);
				intent.putExtra("jid", jid);
				context.startService(intent);
				item.setChecked(!item.isChecked());
				keepMenuOpen(item, context);
				return false;
			case 107 :
				intent = new Intent();
				intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX", "de.bidlingmeyer.xposed.WhatsAppX.StatsActivity"));
				intent.putExtra("jid", jid);
				context.startActivity(intent);
				return true;
			case 108 :
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
			case 109:
				intent = new Intent();
				intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.ColorActivity"));
				intent.putExtra("jid", jid);
				context.startActivity(intent);
				return true;
			case 1010 :
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
	public void showDialog(final Context context, Menu m, boolean fromGear){
		boolean tmpBool = false;
		loadPreviewContacts();
		if(previewContacts != null){
			tmpBool = previewContacts.contains(conversationName);
		}
		final boolean hide = tmpBool;
		addOptions(context, m, fromGear, hide);
	}
	public void showDialog(final Context context, View v, boolean fromGear){
		boolean tmpBool = false;
		loadPreviewContacts();
		if(previewContacts != null){
			tmpBool = previewContacts.contains(conversationName);
		}
		final boolean hide = tmpBool;

		PopupMenu popup = new PopupMenu(context, v);

		addOptions(context, popup.getMenu(), fromGear, hide);

		/*popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			 @Override
			 public boolean onMenuItemClick(final MenuItem item) {

				 return menuClick(context, item, hide);
			 }
		 });*/

		popup.show();
	}
	
	public void keepMenuOpen(MenuItem item, Context context){
		 // Keep the popup menu open
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(context));
		try {
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
		}catch(Exception e){}
	}
}
