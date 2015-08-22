package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

public class SaveInfoService extends IntentService {

	public SaveInfoService() {
		super("saveInfo");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Helper.saveInfo(this, intent.getStringExtra("conversationName"), intent.getStringExtra("jid"), intent.getStringExtra("message"), intent.getStringExtra("layoutTime"), intent.getStringExtra("contact"));
		Toast.makeText(this, "Message added to Favorites", Toast.LENGTH_SHORT).show();
	}

}
