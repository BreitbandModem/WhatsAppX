package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class DeleteMsgService extends IntentService{

	public DeleteMsgService() {
		super("HideNotificationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String message = intent.getStringExtra("message");
		String jid = intent.getStringExtra("jid");
		
		String result = Helper.deleteMessage(jid, message.trim());
		Log.i("whatsappx", result);
		//Toast.makeText(this, "result"+result, Toast.LENGTH_SHORT).show();
	}

}
