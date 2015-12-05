package de.bidlingmeyer.xposed.WhatsAppX;

import java.util.ArrayList;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.PopupMenu;

public class ChatBubbleFragment extends Fragment {

    public ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    public String conversationName, originalConversationName;
    private boolean t;
    ArrayList<String> keys;

	public static ChatBubbleFragment newInstance(String conversationName, String tag, String origName) {
		ChatBubbleFragment fragment = new ChatBubbleFragment();

		Bundle args = new Bundle();
		args.putString("conversationName", conversationName);
		args.putString("tag", tag);
		args.putString("origName", origName);
		fragment.setArguments(args);

		return fragment;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		originalConversationName = getArguments().getString("origName");
		if(getArguments().getString("conversationName").equals("")){
			this.conversationName = getArguments().getString("tag").toString();
			t = true;
		}else{
			this.conversationName = getArguments().getString("conversationName");
			t = false;
		}


        View view = inflater.inflate(R.layout.chat, container, false);
        listView = (ListView) view.findViewById(R.id.listView1);
        chatArrayAdapter = new ChatArrayAdapter(getActivity(), R.layout.singlemessage, conversationName, t);
        if(chatArrayAdapter.getCount() <= 0)
        	//reload();
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);
        listView.setSelection(chatArrayAdapter.getCount());
        
        listView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
				
				PopupMenu popup = new PopupMenu(getActivity(), view);
	            //Inflating the Popup using xml file  
	            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());  
	            //registering popup with OnMenuItemClickListener  
	            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {  
	             @Override
				public boolean onMenuItemClick(MenuItem item) {  
	            	 if(item.getItemId() == R.id.copy){
	            		 ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
	            		 ClipData clip = ClipData.newPlainText("label", chatArrayAdapter.getMessage(position));
	            		 clipboard.setPrimaryClip(clip);
	 					Toast.makeText(getActivity(), "message copied to clipboard", Toast.LENGTH_SHORT).show();
	            	 }else if(item.getItemId() == R.id.delete){
	            		 chatArrayAdapter.deleteMessage(position, t, conversationName);
	            		 if(chatArrayAdapter.getCount() <= 0)
	            			 reload();
	            	 }else if(item.getItemId() == R.id.tag){
	            		 final Dialog dialog = new Dialog(view.getContext());
	            		 dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	            		 LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            		 View v = li.inflate(R.layout.add_tag, null, false);
	            		 ListView list = (ListView) v.findViewById(R.id.listView2);
	            		 
	            		 final SharedPreferences s = ChatBubbleFragment.this.getActivity().getSharedPreferences("tags", Context.MODE_PRIVATE);
	            		 keys = new ArrayList<String>();
	            		 Map<String, ?> allEntries = s.getAll();
	            		 for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
	            			 keys.add(entry.getKey());
	            		 }
	            		 keys.add(0, "< New Tag >");
	            		 ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, keys.toArray(new String[keys.size()]));
	            		 list.setAdapter(adapter);
	            		 list.setOnItemClickListener(new OnItemClickListener() {
	                         @Override
	                         public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {	                          
	                             if(pos == 0){
	                            	 final Dialog d = new Dialog(view.getContext());
	        	            		 d.requestWindowFeature(Window.FEATURE_NO_TITLE);
	        	            		 LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        	            		 View vi = li.inflate(R.layout.tag_input, null, false);
	        	            		 EditText editText = (EditText) vi.findViewById(R.id.tag_input);
	        	            		 editText.setOnEditorActionListener(new OnEditorActionListener() {
	        	            			    @Override
	        	            			    public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
	        	            			        boolean handled = false;
	        	            			        if (actionId == EditorInfo.IME_ACTION_DONE) {
	        	            			            String t = v.getText().toString().trim();
	        	            			            if(t.length()!=0){
		        	            			            if(!keys.contains(t)){
		        	            			            	SharedPreferences.Editor edit = s.edit();
		        	            			            	edit.putString(t, "");
		        	            			            	edit.commit();
		        	            			            }
		        		                            	chatArrayAdapter.addTag(position, t);
		        		                            	d.dismiss();
		        		                            	reload();
		        	            			            handled = true;
	        	            			            }
	        	            			        }
	        	            			        return handled;
	        	            			    }
	        	            			});
	        	            		 d.setContentView(vi);
	        	            		 d.show();
	        	            		 dialog.dismiss();
	                             }else{
	                            	 if(chatArrayAdapter.addTag(position, keys.get(pos))){
	                            		 dialog.dismiss();
	                            		 reload();
	                            	 }else{
	                            		 dialog.dismiss();
	                            	 }
	                             }
	                         }
	                    }); 
	            		 dialog.setContentView(v);
	            		 dialog.show();
	            	 }
	              return true;  
	             }  
	            });  

	            popup.show();
				
				return true;
			}
        });        
        
        //to scroll the list view to bottom on data change
        /*chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });*/
        
        return view;
    }
    
    public void reload(){
    	Intent intent = new Intent();
    	intent.setComponent(new ComponentName("de.bidlingmeyer.xposed.WhatsAppX","de.bidlingmeyer.xposed.WhatsAppX.PagerActivity"));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK); 
		intent.putExtra("conversationName", originalConversationName);
		intent.putExtra("whatsapp", true);
		startActivity(intent);
    }
    
    public void deleteAll(Context context){
    	new AlertDialog.Builder(context)
	    .setTitle("Delete All Messages")
	    .setMessage("Are you sure you want to delete all Messages from this contact?")
	    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	        @Override
			public void onClick(DialogInterface dialog, int which) {
	        	chatArrayAdapter.deleteAll(t, conversationName);
	        	reload();
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
}