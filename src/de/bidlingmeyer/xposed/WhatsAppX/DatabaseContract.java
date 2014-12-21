package de.bidlingmeyer.xposed.WhatsAppX;

import android.provider.BaseColumns;

public final class DatabaseContract {

    public static final  int    DATABASE_VERSION   = 1;
    public static final  String DATABASE_NAME      = "favorites.db";
    private static final String TEXT_TYPE          = " TEXT";
    private static final String COMMA_SEP          = ",";
    
    private DatabaseContract() {}

    public static abstract class Table1 implements BaseColumns {
        public static final String TABLE_NAME       = "favorites";
        public static final String COLUMN_NAME_CONVERSATION = "conversationName";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_CONTACT = "contact";
        public static final String COLUMN_NAME_JID = "jid";
        public static final String COLUMN_NAME_TAGS = "tags";


        public static final String CREATE_TABLE = 
        		"CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_CONVERSATION + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_MESSAGE + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
        		COLUMN_NAME_CONTACT + TEXT_TYPE +  COMMA_SEP + 
        		COLUMN_NAME_JID + TEXT_TYPE +  COMMA_SEP +
        		COLUMN_NAME_TAGS + TEXT_TYPE +  " )";
                
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
