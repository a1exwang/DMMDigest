package org.a1ex.dmmdigest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";
    ImageView imageView;
    Button prev, next, mark;
    TextView desgn;

    String currentDesg;
    String currentPath;
    int currentMarked;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.image);
        prev = (Button) findViewById(R.id.prev);
        next = (Button) findViewById(R.id.next);
        mark = (Button) findViewById(R.id.mark);
        desgn = (TextView) findViewById(R.id.desgn);

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissions, WRITE_REQUEST_CODE);
    }

    static final int WRITE_REQUEST_CODE = 1;

    static String dirPath = "file://" + Environment.getExternalStorageDirectory() + "/kawd";
    static File dbFile = new File(Environment.getExternalStorageDirectory() + "/kawd/db.sqlite3");
    static String imageDir = dirPath + "/files";

    static SQLiteDatabase s_db = null;
    static SQLiteDatabase getDb() {
        if (s_db == null) {
            s_db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        }
        return s_db;
    }

    static void createMarkColumnIfDoesNotExist() {
        boolean created  = false;
        SQLiteDatabase db = getDb();

        Cursor c = db.rawQuery("PRAGMA table_info(covers)", null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            String colName = c.getString(1);
            if (colName.equalsIgnoreCase("marked")) {
                created = true;
                break;
            }
            c.moveToNext();
        }
        c.close();

        if (!created) {
            db.execSQL("ALTER TABLE covers ADD COLUMN marked integer");
        }
    }

    private Cursor getCursor() {
        SQLiteDatabase db = getDb();
        String[] projection = {
                "file_path",
                "designation",
                "marked",
        };

        Cursor c = db.query(
                "covers",                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                "RANDOM()", // order by
                "1000"
        );

        c.moveToFirst();
        return c;
    }

    void setCurrentByCursor(Cursor c) {
        setCurrent(c.getString(0), c.getString(1), c.getInt(2));
    }
    void setCurrent(String path, String desg, int marked) {
        currentPath = path;
        currentDesg = desg;
        currentMarked = marked;
    }

    void reload() {
        Picasso.with(this).load(imageDir + "/" + currentPath).into(imageView);
        desgn.setText(currentDesg);
        desgn.setTextColor(currentMarked == 1 ? Color.RED : Color.BLACK);
    }
    void next() {
        if (cursor != null) {
            if (cursor.isAfterLast()) {
                cursor.moveToFirst();
            }
            cursor.moveToNext();

            currentPath = cursor.getString(0);
            currentDesg = cursor.getString(1);
            currentMarked = cursor.getInt(2);
            reload();
        }
    }
    void prev() {
        if (cursor != null) {
            if (cursor.isBeforeFirst()) {
                cursor.moveToLast();
            }
            cursor.moveToPrevious();

            currentPath = cursor.getString(0);
            currentDesg = cursor.getString(1);
            currentMarked = cursor.getInt(2);
            reload();
        }
    }

    void toggleMark() {
        SQLiteDatabase db = getDb();
        if (currentMarked == 1) {
            currentMarked = 0;
            String sql = "UPDATE covers SET marked = 0 WHERE file_path = \"" + currentPath+ "\"";
            db.execSQL(sql);
            Log.i(TAG, sql);
            Toast.makeText(this, "Unmarked " + currentDesg + " at \"" + currentPath+ "\"", Toast.LENGTH_SHORT).show();
            reload();
        }
        else {
            currentMarked = 1;
            String sql = "UPDATE covers SET marked = 1 WHERE file_path = \"" + currentPath + "\"";
            db.execSQL(sql);
            Log.i(TAG, sql);
            Toast.makeText(this, "Marked " + currentDesg + " at \"" + currentPath + "\"", Toast.LENGTH_SHORT).show();
            reload();
        }
    }
    void unmarkAll() {
        SQLiteDatabase db = getDb();
        String sql = "UPDATE covers SET marked = 0 WHERE 1 = 1";
        db.execSQL(sql);
        Log.i(TAG, sql);

        currentMarked = 0;
        reload();
    }

    Cursor cursor;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    createMarkColumnIfDoesNotExist();
                    setCurrentByCursor(cursor = getCursor());
                    reload();
                    next.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.next();
                        }
                    });
                    next.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            MainActivity.this.viewMarked();
                            return false;
                        }
                    });
                    prev.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.prev();
                        }
                    });
                    mark.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.toggleMark();
                        }
                    });
                    mark.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Unmark all?")
                                    .setMessage("This cannot be undone.")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            unmarkAll();
                                        }})
                                    .setNegativeButton(android.R.string.no, null).show();
                            return false;
                        }
                    });


                }
                break;
        }
    }

    void viewMarked() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setIcon(android.R.drawable.ic_dialog_alert);
        builderSingle.setTitle("Marked");

        SQLiteDatabase db = getDb();
        final Cursor c = db.query(
                true,
                "covers",
                new String[] {"rowid _id", "designation"},
                "marked = 1", null, "designation", null, "designation", null);
        final CursorAdapter adapter = new CursorAdapter(MainActivity.this, c, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                TextView tv = new TextView(MainActivity.this);
                tv.setText(cursor.getString(1));
                return tv;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView tv = (TextView) view;
                tv.setText(cursor.getString(1));
            }
        };
        builderSingle.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        c.close();
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(adapter, null);
        builderSingle.show();
    }
}
