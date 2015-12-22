package de.bidlingmeyer.xposed.WhatsAppX;

import java.util.ArrayList;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

public class PagerActivity extends FragmentActivity {
  ViewPager tab;
  TabPagerAdapter tabAdapter;
  ActionBar actionBar;
  String conversationName="", jid="";
  boolean cannot, fromWhatsapp;
  Receiver rec;
  IntentFilter intentFilter;
  
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        
        setContentView(R.layout.viewpager);
        
        intentFilter = new IntentFilter("de.bidlingmeyer.xposed.WhatsAppX.cannot");
        rec = new Receiver();
        this.registerReceiver(rec, intentFilter);
        cannot = false;
        
        Intent intent = getIntent();
        fromWhatsapp = intent.getBooleanExtra("whatsapp", false);
        conversationName = intent.getStringExtra("conversationName");
        jid = intent.getStringExtra("jid");
			//Toast.makeText(this, "", Toast.LENGTH_LONG).show();        
        
        //SharedPreferences pref2 = getSharedPreferences("contacts", Context.MODE_PRIVATE);
		//jid = pref2.getString(conversationName, "");
        
        ArrayList<String[]> tabs = Helper.getTabs(this);
        if(tabs.size() == 0){
        	Intent i = new Intent(this, SettingsActivity.class);
        	i.putExtra("whatsapp", fromWhatsapp);
        	i.putExtra("jid", jid);
            startActivity(i);
            finish();
        }
        
        tabAdapter = new TabPagerAdapter(getSupportFragmentManager(), this, tabs, conversationName);
        tab = (ViewPager)findViewById(R.id.pager);
        tab.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                      actionBar = getActionBar();
                      actionBar.setSelectedNavigationItem(position);                    }
        });
        tab.setAdapter(tabAdapter);
        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener(){
		      @Override
		      public void onTabReselected(android.app.ActionBar.Tab tab,
		          FragmentTransaction ft) {
		      }
		      @Override
		       public void onTabSelected(ActionBar.Tab t, FragmentTransaction ft) {
		              tab.setCurrentItem(t.getPosition());
		          }
		      @Override
		      public void onTabUnselected(android.app.ActionBar.Tab tab,
		          FragmentTransaction ft) {
      }};
      
      //Add New Tab
      for(int i=0; i<tabs.size(); i++){
    	  if(tabs.get(i)[2].equals("")){
    		  actionBar.addTab(actionBar.newTab().setText(tabs.get(i)[0]).setTabListener(tabListener));
    	  }else{
    		  actionBar.addTab(actionBar.newTab().setText(("< "+tabs.get(i)[2])+" >").setTabListener(tabListener));
    	  }
    	  if(tabs.get(i)[0].equals(conversationName) || tabs.get(i)[2].equals("< "+conversationName+" >")){
    		  tab.setCurrentItem(i);
    		  if(jid.equals("")){
    			  jid = tabs.get(i)[1];
    		  }
    	  }
      }
    }
    
    @Override
    public void onBackPressed() {
        Log.i("whatsapp", "backpressed: "+cannot+", "+fromWhatsapp);
    	if(!cannot && fromWhatsapp){
    		toContact();
    	}else{
    		super.onBackPressed();
    	}
    }
    public void toContact(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.whatsapp","com.whatsapp.Conversation"));
        intent.putExtra("jid", jid);
        intent.setFlags(335544320);
        startActivity(intent);
    	/*if(jid.contains("@g.us"))
    		jid = "";
    	Uri uri = Uri.parse("smsto:"+jid);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("test", "test2");
        intent.setPackage("com.whatsapp");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
        startActivity(intent);*/
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(rec, intentFilter);
        cannot = false;
    }
    @Override
    protected void onPause() {
        unregisterReceiver(rec);
        super.onPause();
    }
    @Override
    protected void onDestroy() {
    	try{
    		unregisterReceiver(rec);
    	}catch(IllegalArgumentException e) {}
        super.onDestroy();
    }
    
    public class Receiver extends BroadcastReceiver {
   	 @Override
   	 public void onReceive(Context context, Intent intent) {
   		 cannot = true;
   	 }
   }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pager_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.action_deleteAll:
        		ChatBubbleFragment c = (tabAdapter.getFrag(tab.getCurrentItem())); 
        		if(c != null) c.deleteAll(this);
        		return true;
            case R.id.action_settings:
            	Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}