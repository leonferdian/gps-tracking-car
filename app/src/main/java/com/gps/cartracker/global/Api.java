package com.gps.cartracker.global;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Environment;

public class Api {
    public static final String APP_NAME = "GPS Tracker";
    public static final String JSON_PHOTOS = "Photos";
    public static final String PARAM_START = "Start";

    public static final String TAG_ID = "id";
    public static final String TAG_USERNAME = "username";
    public static final String TAG_EMAIL = "email";
    public final static String TAG_TELP = "telp";
    public static final String TAG_KATEGORI_USER = "kategori_user";
    public static final String TAG_COMPANY_IS_SELECTED = "company_selected";
    public static final String TAG_COMPANY_USER_ID = "company_user_id";
    public static final String TAG_COMPANY_USER_NAME = "company_user_name";
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_ID_EMPLOYEE_DMS3 = "id_employee_dms3";
    public final static String TAG_DB_EMPLOYEE_DMS3 = "db_employee_dms3";
    public final static String TAG_NIK = "nik";
    public final static String TAG_JABATAN = "jabatan";
    public final static String TAG_DIVISI = "divisi";
    public static final String my_shared_preferences = "gpstrack_shared_preferences";
    public static SharedPreferences sharedpreferences;
    public static ConnectivityManager conMgr;

    public static final String PARAM_DATA = "Data";

    public static final String getFileImagePath() {
        return Environment.getExternalStorageDirectory() + "/" + APP_NAME;
    }
}
