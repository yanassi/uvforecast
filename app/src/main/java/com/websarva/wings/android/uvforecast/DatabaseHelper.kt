package com.websarva.wings.android.uvforecast

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "UVI.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sb = StringBuilder()
        sb.append("CREATE TABLE UVIs (")
        sb.append("_id TEXT PRIMARY KEY,")
        sb.append("data TEXT,")
        sb.append("date TEXT")
        sb.append(");")
        val sql = sb.toString()

        //SQLの実行。
        db.execSQL(sql)

    }


    //抽象メソッド
    override fun onUpgrade(db:SQLiteDatabase, oldVersion: Int, newVersioin: Int) {}


}