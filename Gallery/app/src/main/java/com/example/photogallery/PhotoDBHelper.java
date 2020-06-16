package com.example.photogallery;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class PhotoDBHelper extends SQLiteOpenHelper {
    public static final String PHOTO_TABLE = "PHOTO_TABLE";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_PHOTO_ID = "PHOTO_" + COLUMN_ID;
    public static final String COLUMN_PHOTO_PATH = "PHOTO_PATH";

//
//    public PhotoDBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
//        super(context, name, factory, version);
//    }

    public PhotoDBHelper(@Nullable Context context) {
        super(context, "photodata.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableStatement = "CREATE TABLE " + PHOTO_TABLE + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_PHOTO_ID + " TEXT, " + COLUMN_PHOTO_PATH + " TEXT)";
        db.execSQL(createTableStatement);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean addOne (PhotoData photoData){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_PHOTO_ID, photoData.getId());
        cv.put(COLUMN_PHOTO_PATH, photoData.getPath());
        long insert = db.insert(PHOTO_TABLE,null,cv);
        if (insert == -1 ){
            return false;
        }
        else
            return true;
    }
}
