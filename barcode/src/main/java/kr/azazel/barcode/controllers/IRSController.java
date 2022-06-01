package kr.azazel.barcode.controllers;

import android.database.Cursor;
import android.net.Uri;

/**
 * Created by JJ on 2015-06-13.
 */
public interface IRSController {

    public Cursor query(Uri uri);
}