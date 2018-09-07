package com.dsrv.ciprian.findr;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Eteru on 12/26/2017.
 */

public class Popup extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int w = (int)(dm.widthPixels * 0.8);
        int h = 800;

        getWindow().setLayout(w, h);

        final String title = getIntent().getStringExtra("title");
        String rating = getIntent().getStringExtra("rating");
        String open = getIntent().getStringExtra("open");
        String address = getIntent().getStringExtra("address");
        final String lat = getIntent().getStringExtra("lat");
        final String lng = getIntent().getStringExtra("lng");

        final String olat = getIntent().getStringExtra("olat");
        final String olng = getIntent().getStringExtra("olng");
        //address = address.replace(",", ",\n");
        setTitle(title);

        TextView text = (TextView) findViewById(R.id.popup_text_view);
        text.clearComposingText();
        text.append(rating + " rating.\n\n");
        text.append(open.equals("true") ? "Open now.\n\n" : "Closed.\n\n" );
        text.append(address);

        findViewById(R.id.view_on_map).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lng);

            intent.putExtra("olat", olat);
            intent.putExtra("olng", olng);
            startActivity(intent);
            }
        });

        findViewById(R.id.save_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "Location saved.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
