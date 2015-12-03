package de.bidlingmeyer.xposed.WhatsAppX;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;

import de.bidlingmeyer.xposed.WhatsAppX.DatabaseContract.Table1;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.widget.Toast;

public class Helper {
	
	public static ArrayList<String[]> getTabs(Context context){
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		String[] projection = {Table1.COLUMN_NAME_CONVERSATION, Table1.COLUMN_NAME_JID, Table1.COLUMN_NAME_TAGS};
		// How you want the results sorted in the resulting Cursor
		String sortOrder = Table1.COLUMN_NAME_CONVERSATION + " DESC";
		Cursor c = db.query(
		    Table1.TABLE_NAME,  // The table to query
		    projection,                               // The columns to return
		    null,                                // The columns for the WHERE clause
		    null,                            // The values for the WHERE clause
		    null,                                     // don't group the rows
		    null,                                     // don't filter by row groups
		    sortOrder                                 // The sort order
		);
		
		ArrayList<String[]> result = new ArrayList<String[]>();
		ArrayList<String> dbTags = new ArrayList<String>();
		int index = 0;
		while(c.moveToNext()){
			String cs = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_TAGS));
			if(cs != null){
				String arr [] = cs.split("[\\x7C]");
				for(String s : arr){
					if(!s.equals("") && !dbTags.contains(s))
						dbTags.add(s);
				}
			}
			
			String n = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_CONVERSATION));
			String j = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_JID));
			String [] str = {n, j, ""};
			if(index > 0 && result.get(index-1)[0].equals(n)){
				if(result.get(index-1)[1].equals("")){
					result.set(index-1, str);
				}
				continue;
			}
			result.add(str);
			index++;
		}
		
		SharedPreferences s = context.getSharedPreferences("tags", Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = s.edit();
		ArrayList<String> prefTags = new ArrayList<String>();
		Map<String, ?> allEntries = s.getAll();
		 for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			 prefTags.add(entry.getKey());
		 }
		for(String prefTag : prefTags){
			if(dbTags.contains(prefTag)){
				String [] res = {"", "-1", prefTag};
				result.add(res);
			}else{
				edit.remove(prefTag).commit();
			}
		}
		return result;
	}
	
	public static void deleteMessage(Context context, ChatMessage m, boolean isTag, String tag){
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if(!isTag){
			String selection = BaseColumns._ID + " =?";
			String[] selectionArgs = { Long.toString(m._id) };
			db.delete(Table1.TABLE_NAME, selection, selectionArgs);
		}else{
			ContentValues values = new ContentValues();
			if(m.tags == null){
				values.put(Table1.COLUMN_NAME_TAGS, "");
			}else{
				String mtags = m.tags;
				String newTags = mtags.replace("|"+tag+"|", "|");
				if(newTags.equals("|"))
					newTags = "";
				values.put(Table1.COLUMN_NAME_TAGS, newTags);
			}
			
			String selection = BaseColumns._ID + "="+m._id;
			
			db.update(
				    Table1.TABLE_NAME,
				    values,
				    selection,
				    null);
		}
	}
	
	public static void saveInfo(Context context, String conversationName, String jid, String message, String layoutTime, String contactName){
		String []info = getMessage(context, conversationName, jid, message, contactName, layoutTime);
		jid = "";
		if(info != null){
			jid = info[1];
			if(info[0].length() > 5)
				layoutTime = info[0];
		}
		
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(Table1.COLUMN_NAME_CONVERSATION, conversationName);
		values.put(Table1.COLUMN_NAME_MESSAGE, message);
		values.put(Table1.COLUMN_NAME_TIMESTAMP, layoutTime);
		values.put(Table1.COLUMN_NAME_CONTACT, contactName);
		values.put(Table1.COLUMN_NAME_JID, jid);

		// Insert the new row, returning the primary key value of the new row
		db.insert(Table1.TABLE_NAME, null, values);
	}
	
	public static boolean addTag(Context context, ChatMessage m, String tag){
		if(m.tags != null && (m.tags.contains("|"+tag+"|"))){
			Toast.makeText(context, "Tag already assigned", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		if(m.tags == null){
			values.put(Table1.COLUMN_NAME_TAGS, "|"+tag+"|");
		}else{
			values.put(Table1.COLUMN_NAME_TAGS, m.tags+tag+"|");
		}
		
		String selection = BaseColumns._ID + "="+m._id;
		
		db.update(
			    Table1.TABLE_NAME,
			    values,
			    selection,
			    null);
		
		Toast.makeText(context, "Tag successfully assigned", Toast.LENGTH_SHORT).show();
		return true;
	}
	
	public static ArrayList<ChatMessage> loadInfo(Context context, String conversationName, boolean tag){
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		// Define a projection that specifies which columns from the database
		// you will actually use after this query.
		String[] projection = {
			BaseColumns._ID,
		    Table1.COLUMN_NAME_CONVERSATION,
		    Table1.COLUMN_NAME_MESSAGE,
		    Table1.COLUMN_NAME_TIMESTAMP,
		    Table1.COLUMN_NAME_CONTACT,
		    Table1.COLUMN_NAME_JID,
		    Table1.COLUMN_NAME_TAGS
		};
		
		String selection;
		String [] selectionArgs = new String[1];
		if(tag){
			selection = Table1.COLUMN_NAME_TAGS+" LIKE ?";
			selectionArgs[0] = ("%|"+conversationName+"|%");
		}else{
			selection = Table1.COLUMN_NAME_CONVERSATION+"=?";
			selectionArgs[0] = conversationName;
		}
		String sortOrder = Table1.COLUMN_NAME_TIMESTAMP + " ASC";


		Cursor c = db.query(
		    Table1.TABLE_NAME,  // The table to query
		    projection,                               // The columns to return
		    selection,                                // The columns for the WHERE clause
		    selectionArgs,                            // The values for the WHERE clause
		    null,                                     // don't group the rows
		    null,                                     // don't filter by row groups
		    sortOrder                                 // The sort order
		    );
		
		ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
		
		while(c.moveToNext()){
			String message = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_MESSAGE));
			String timestamp = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_TIMESTAMP));
			String contact = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_CONTACT));
			long _id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
			String jid = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_JID));
			String tags = c.getString(c.getColumnIndexOrThrow(Table1.COLUMN_NAME_TAGS));
			messages.add(new ChatMessage(conversationName, message, timestamp, contact, _id, jid, tags));
		}
		
		return messages;
	}
	
	/*public static boolean isContactName(String string){
		String Jid = shell("sqlite3 /data/data/com.whatsapp/databases/wa.db 'Select jid FROM wa_contacts WHERE is_whatsapp_user AND display_name=\""+string+"\"'" + "\n").trim();
		if(Jid.length() != 0)
			return true;
		return false;
	}*/
	
	/*public static String[] loadInfoWhatsapp(Context context, String conversationName, String message, String layoutTime, String contact){
		//shell("chmod 777 /data/data/com.whatsapp/databases");
		DatabaseHelperWhatsapp dbHelper = new DatabaseHelperWhatsapp(context, "wa.db");
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String [] projection = {"jid"};
		String [] selectionArgs = {conversationName};
			Cursor c = db.query(
			    "wa_contacts",  // The table to query
			    projection,                               // The columns to return
			    "is_whatsapp_user=1 AND display_name=?",                                // The columns for the WHERE clause
			    selectionArgs,                            // The values for the WHERE clause
			    null,                                     // don't group the rows
			    null,                                     // don't filter by row groups
			    null                                 // The sort order
			    );
		c.moveToFirst();
		String conversationJid = c.getString(c.getColumnIndexOrThrow("jid"));
		String groupContactJid=null;
		String key_from_me = "0";

		if(contact.equals("myself")){
			key_from_me = "1";
		}
		else if(!conversationName.equals(contact)){
			String [] projection2 = {"jid"};
			String [] selectionArgs2 = {contact};
				c = db.query(
				    "wa_contacts",  // The table to query
				    projection2,                               // The columns to return
				    "is_whatsapp_user=1 AND display_name=?",                                // The columns for the WHERE clause
				    selectionArgs2,                            // The values for the WHERE clause
				    null,                                     // don't group the rows
				    null,                                     // don't filter by row groups
				    null                                 // The sort order
				    );
			c.moveToFirst();
			groupContactJid = c.getString(c.getColumnIndexOrThrow("jid"));
		}
		String[] emojiSplit = message.split("\\P{InBasic_Latin}");

		dbHelper = new DatabaseHelperWhatsapp(context, "wa.db");
		db = dbHelper.getReadableDatabase();
		
		String [] projection3 = {"timestamp"};
		String selection3;
		String [] selectionArgs3;
		int k;
		if(groupContactJid != null){
			selection3 = "key_remote_jid=? AND key_from_me=? AND remote_resource=?";
			selectionArgs3 = new String[3+emojiSplit.length];
			selectionArgs3[0] =	conversationJid;
			selectionArgs3[1] = key_from_me; 
			selectionArgs3[2] = groupContactJid;
			k=3;
		}else{
			selection3 = "key_remote_jid=? AND key_from_me=?";
			selectionArgs3 = new String[2+emojiSplit.length];
			selectionArgs3[0] =	conversationJid;
			selectionArgs3[1] = key_from_me; 
			k=2;
		}

		for(int i=0; i<emojiSplit.length; i++){
			selection3 += " AND instr(data, ?) > 0";
			selectionArgs3[i+k] = emojiSplit[i];
		}
		String sortOrder = "timestamp" + " DESC";
			c = db.query(
			    "messages",  // The table to query
			    projection3,                               // The columns to return
			    selection3,                                // The columns for the WHERE clause
			    selectionArgs3,                            // The values for the WHERE clause
			    null,                                     // don't group the rows
			    null,                                     // don't filter by row groups
			    sortOrder                                // The sort order
			    );
		String timestamp = "0";
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

		while(c.moveToNext()){
			String t = c.getString(c.getColumnIndexOrThrow("timestamp"));
			if(t.length() < 10)
				continue;
			long l = Long.parseLong(t.substring(0, 10));
			Date d = new Date(l*1000);
			String tmpDate = sdf.format(d);
			if(tmpDate.equals(layoutTime)){
				timestamp = Long.toString(l);
				break;
			}
		}

		String[] result = {timestamp, conversationJid};
		return result;
	}*/
	
	public static ArrayList<String> refreshContactList(){		
		String str = shell("sqlite3 /data/data/com.whatsapp/databases/wa.db 'Select display_name, jid FROM wa_contacts WHERE is_whatsapp_user=1';", true).trim();
		String[] split = str.split("\n");//"[\\x7C]"
		ArrayList<String> list = new ArrayList<String>();
		for(int i=0; i<split.length; i++){
			list.add(split[i]);
		}
		return list;
	}
	
	public static String getStats(String jid, Context context){
		String stats = "";
		String str = shell("sqlite3 /data/data/com.whatsapp/databases/msgstore.db 'Select _id FROM messages WHERE key_from_me=0 AND key_remote_jid=\""+jid+"\"';", true).trim();
		String [] split = str.split("\n");
		stats = "Messages received: "+split.length;
		str = shell("sqlite3 /data/data/com.whatsapp/databases/msgstore.db 'Select _id FROM messages WHERE key_from_me=1 AND key_remote_jid=\""+jid+"\"';", true).trim();
		split = str.split("\n");
		stats += "\nMessages sent: "+split.length;
		return stats;
	}
		
	/*public static Bitmap getIconBitmap(Context context, String iconName, int pixelsX, int pixelsY){
		Bitmap bMap=null;
	    Resources res = getRes(context);
		int id = res.getIdentifier(iconName, "drawable", "de.bidlingmeyer.xposed.WhatsAppX");
		bMap = BitmapFactory.decodeResource(res, id);
		bMap = getResizedBitmap(bMap,pixelsX,pixelsY);
	    return bMap;
	}
	
	public static Resources getRes(Context context){
		Resources res = null;
		try {
			res = context.getPackageManager().getResourcesForApplication("de.bidlingmeyer.xposed.WhatsAppX");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) { 

		int width = bm.getWidth(); 
		int height = bm.getHeight(); 

		float scaleWidth = ((float) newWidth) / width; 
		float scaleHeight = ((float) newHeight) / height; 

		// create a matrix for the manipulation 
		Matrix matrix = new Matrix(); 

		// resize the bit map 
		matrix.postScale(scaleWidth, scaleHeight); 

		// recreate the new Bitmap 
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false); 

		return resizedBitmap; 
	}
	
	
	
	public static Bitmap drawableToBitmap(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
        return ((BitmapDrawable)drawable).getBitmap();
    }

    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap); 
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
}*/
	
	public static String getJid(Context context, String name){
		//String Jid = shell("sqlite3 /data/data/com.whatsapp/databases/wa.db 'Select jid FROM wa_contacts WHERE is_whatsapp_user AND display_name=\""+name+"\"'" + "\n").trim();
		SharedPreferences pref = context.getSharedPreferences("contactsName", Context.MODE_PRIVATE);
		String jid = pref.getString(name, "");
		return jid;
	}
	
	public static String deleteMessage(String jid, String message){
		String result = shell("sqlite3 /data/data/com.whatsapp/databases/msgstore.db 'Delete FROM messages WHERE key_remote_jid=\""+jid+"\" AND data=\""+message+"\"';", true).trim();
		return result;
	}
	
	@SuppressLint("SimpleDateFormat")
	public static String[] getMessage(Context context, String jid, String conversationName, String message, String contact, String layoutTime){
		String[] msgFrg = message.split("\\W");//"\\P{InBasic_Latin}"
		
		String conversationJid="";;
		if(jid.length() < 5){
			return null;
		}
		String groupContactJid=null;
		int key_from_me = 0;
		if(contact.equals("myself")){
			key_from_me = 1;
		}
		else if(!conversationName.equals(contact)){
			SharedPreferences pref = context.getSharedPreferences("contactsName", Context.MODE_PRIVATE);
			groupContactJid = pref.getString(contact, "");
		}else{
			conversationJid = jid;
		}

		String query = "sqlite3 /data/data/com.whatsapp/databases/msgstore.db 'Select timestamp FROM messages WHERE key_remote_jid=\""+conversationJid+"\" AND key_from_me="+key_from_me;
							if(groupContactJid != null){
								query += " AND remote_resource=\""+groupContactJid+"\"";
							}
							for(String str : msgFrg){
								query += " AND instr(data, \""+str+"\") > 0";
							}
							query += "';";
		String result = shell(query, true);
		String output[] = result.split("[\\x7C]");
		long timestamp = 0;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm"); // the format of your date
		for(int i=0; i<output.length; i++){
			if(output[i].length() < 10)
				continue;
			long l = Long.parseLong(output[i].substring(0, 10));
			Date d = new Date(l*1000);
			String tmpDate = sdf.format(d);
			if(tmpDate.equals(layoutTime)){
				timestamp = l;
				break;
			}
		}
		String [] res = {Long.toString(timestamp), conversationJid};
		return res;
	}
	
	public static int convertDp(Context context, int pixels){
		float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pixels * scale + 0.5f);
	}

	
public static String shell(String command, boolean root){
        
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        OutputStreamWriter osw = null;
        StringBuffer output = new StringBuffer();
        
        try { // Run Script

            if(root){
            	proc = runtime.exec("su");
            }else{
            	proc = runtime.exec("");
            }
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command);
            osw.flush();
            osw.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    e.printStackTrace();                    
                }
            }
        }
        try {
            if (proc != null)
                proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line = "";			
		try {
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        if (proc.exitValue() != 0) {}
        return output.toString().trim();
	}
	
}
