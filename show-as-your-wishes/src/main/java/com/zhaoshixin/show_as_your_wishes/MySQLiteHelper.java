package com.zhaoshixin.show_as_your_wishes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MySQLiteHelper extends SQLiteOpenHelper {
    //调用父类构造器
    public MySQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version) {
        super(context, name, factory, version);
    }

    /**
     * 当数据库首次创建时执行该方法，一般将创建表等初始化操作放在该方法中执行.
     * 重写onCreate方法，调用execSQL方法创建表
     * */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists normal_save_message(id integer primary key,normalMessage varchar)");
        db.execSQL("create table if not exists connectDevice(id integer primary key,address varchar)");
    }

    //当打开数据库时传入的版本号与当前的版本号不同时会调用该方法
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}