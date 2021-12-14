package com.whu.gnss.gnsslogger.nav;

/**
 * Created by KuboLab on 2018/02/12.
 */

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteManager extends SQLiteOpenHelper {
    public static final String TAG = "DBOpenHelper";
    public static final String DB_NAME = "GNSSNavigation.db";
    public static final int DB_VERSION = 1;

    //这个是建立GPS星历文件的一个表
    public static final String CREATE_EPHGPSBOOK = "create table ephgpsBook("
            +"id integer primary key autoincrement,"

            +"Svid integer,"

            +"Toc real,"
            +"af0 real,"
            +"af1 real,"
            +"af2 real,"

            +"iode real,"
            +"Crs real,"
            +"delta_n real,"
            +"M0 real,"

            +"Cuc real,"
            +"es real,"
            +"Cus real,"
            +"sqrtA real,"

            +"Toe integer,"
            +"Cic real,"
            +"Omega_0 real,"
            +"Cis real,"

            +"i0 real,"
            +"Crc real,"
            +"w real,"
            +"Omega_dot real,"

            +"i_dot real,"
            +"L2code real,"
            +"gpsweek real,"
            +"L2Flag integer,"

            +"svAccur integer,"
            +"svHealth integer,"
            +"iodc real,"
            +"TGD real)";


    public SQLiteManager( Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        Log.d(TAG, "onCreate version : " + db.getVersion());
        db.execSQL(CREATE_EPHGPSBOOK);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        db.execSQL(CREATE_EPHGPSBOOK);
    }

    public double searchIndex(final SQLiteDatabase db, final String Table, final String column){
        double tmp = 0;
        // テーブルからデータを検索
        if(existTable(db,Table) && existColumn(db,Table,column)) {
            Cursor cursor = db.query(Table, new String[]{column}, null, null, null, null, null);
            // 参照先を一番始めに
            boolean isEof = cursor.moveToFirst();
            // データを取得していく
            while (isEof) {
                tmp = cursor.getDouble(cursor.getColumnIndex(column));
                isEof = cursor.moveToNext();
            }
            // 忘れずに閉じる
            cursor.close();
        }
        return tmp;
    }

    public boolean existTable(final SQLiteDatabase db, final String name){
        String query = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + name + "';";
        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        String result = c.getString(0);
        c.close();
        if(result.indexOf("1") != -1) {
            return true;
        }else {
            return false;
        }
    }

    public boolean existColumn(final SQLiteDatabase db, final String table, final String column){
        String query = "SELECT count(*) from sqlite_master  where name = '" + table + "' and sql like '%"+ column + "%'";
        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        String result = c.getString(0);
        c.close();
        if(result.contains("1")) {
            return true;
        }else {
            return false;
        }
    }
    public void createTable(final SQLiteDatabase db, final String name){
        String query_table1 = "CREATE TABLE " + name + " ( id INTEGER PRIMARY KEY , name  STRING ) ";
        db.execSQL(query_table1);
    }


}
