package de.bidlingmeyer.xposed.WhatsAppX;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

	private TextView chatText;
	private ArrayList<ChatMessage> chatMessageList;
	private LinearLayout singleMessageContainer;
	private Context context;

	@Override
	public void add(ChatMessage object) {
		chatMessageList.add(object);
		super.add(object);
	}

	public ChatArrayAdapter(Context context, int textViewResourceId, String conversationName, boolean tag) {
		super(context, textViewResourceId);
		this.context = context;
		chatMessageList = Helper.loadInfo(context, conversationName, tag);
	}

	@Override
	public int getCount() {
		return this.chatMessageList.size();
	}

	@Override
	public ChatMessage getItem(int index) {
		return this.chatMessageList.get(index);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.singlemessage, parent, false);
		}

		ChatMessage chatMessageObj = getItem(position);
		
		singleMessageContainer = (LinearLayout) row.findViewById(R.id.singleMessageContainer);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		if(chatMessageObj.left){
			layoutParams.setMargins(Helper.convertDp(context, 2), Helper.convertDp(context, 2), Helper.convertDp(context, 40), Helper.convertDp(context, 2));
			singleMessageContainer.setLayoutParams(layoutParams);
			singleMessageContainer.setBackgroundResource(R.drawable.balloon_left);
			singleMessageContainer.setPadding(Helper.convertDp(context, 15), Helper.convertDp(context, 6), Helper.convertDp(context, 6), Helper.convertDp(context, 6));
			LinearLayout outermost = (LinearLayout) row.findViewById(R.id.outermost);
			outermost.setGravity(Gravity.LEFT);
		}else{
			layoutParams.setMargins(Helper.convertDp(context, 40), Helper.convertDp(context, 2), Helper.convertDp(context, 2), Helper.convertDp(context, 2));
			singleMessageContainer.setLayoutParams(layoutParams);
			singleMessageContainer.setBackgroundResource(R.drawable.balloon_right);
			singleMessageContainer.setPadding(Helper.convertDp(context, 6), Helper.convertDp(context, 6), Helper.convertDp(context, 15), Helper.convertDp(context, 6));
			LinearLayout outermost = (LinearLayout) row.findViewById(R.id.outermost);
			outermost.setGravity(Gravity.RIGHT);
		}
		
		chatText = (TextView) row.findViewById(R.id.singleMessage);
		chatText.setText(chatMessageObj.message);
		
		TextView date = (TextView) row.findViewById(R.id.date);
		date.setText(chatMessageObj.layoutTime);
		
		if(chatMessageObj.isGroup){
			TextView contact = (TextView) row.findViewById(R.id.contact);
			contact.setVisibility(View.VISIBLE);
			contact.setText(chatMessageObj.contact);
		}
		
		return row;
	}
	
	public boolean addTag(int position, String tag){
		return Helper.addTag(context, chatMessageList.get(position), tag);
	}
	
	public void deleteMessage(int position, boolean isTag, String tag){
		Helper.deleteMessage(context, chatMessageList.get(position), isTag, tag);
		super.remove(chatMessageList.get(position));
		chatMessageList.remove(position);
	}
	
	public void deleteAll(boolean isTag, String tag){
		for(ChatMessage cm : chatMessageList){
			Helper.deleteMessage(context, cm, isTag, tag);
		}
	}
	
	public String getMessage(int i){
		return chatMessageList.get(i).message;
	}

	public Bitmap decodeToBitmap(byte[] decodedByte) {
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}

}