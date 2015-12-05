package de.bidlingmeyer.xposed.WhatsAppX;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMessage {
	public boolean left, isGroup;
	public String conversationName, message, layoutTime, contact, jid;
	public String tags;
	public long _id;

	public ChatMessage(String conversationName, String message, String timestamp, String contact, long _id, String jid, String tags) {
		this.conversationName = conversationName;
		this.message = message;
		this.contact = contact;
		this.layoutTime = getTime(timestamp);
		this._id = _id;
		this.jid = jid;
		this.tags = tags;
		left = true;
		isGroup = true;
		if(contact.equals("myself")){
			isGroup = false;
			left = false;
		}else if(conversationName.equals(contact)){
			isGroup = false;
		}
	}
	
	public String getTime(String timestamp){
		if(timestamp.contains(":"))
			return timestamp;
		
		SimpleDateFormat sdfOutput = new SimpleDateFormat("dd.MMM HH:mm");
		SimpleDateFormat sdfCompare = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat sdfToday = new SimpleDateFormat("HH:mm");
		
		long l = Long.parseLong(timestamp);
		Date d = new Date(l*1000);
		
		if(sdfCompare.format(System.currentTimeMillis()).equals(sdfCompare.format(d))){
			return sdfToday.format(d);
		}else{
			return sdfOutput.format(d);
		}
	}
}