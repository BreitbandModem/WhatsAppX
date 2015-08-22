package de.bidlingmeyer.xposed.WhatsAppX;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class StatsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		setContentView(R.layout.stats);
		String stats = Helper.getStats(intent.getStringExtra("jid"), this);
		TextView t = (TextView) findViewById(R.id.statsText);
		t.setText(stats);
		//Toast.makeText(this, "Stats: "+stats, Toast.LENGTH_LONG).show();
	}

}
