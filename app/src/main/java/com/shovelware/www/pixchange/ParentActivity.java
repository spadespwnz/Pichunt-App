package com.shovelware.www.pixchange;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Picture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ParentActivity extends AppCompatActivity {


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_hunts:
                    switchToHunts();
                    return true;
                case R.id.navigation_gallery:
                    switchToGallery();
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent);
        final Context context = this;
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setSelectedItemId(R.id.navigation_home);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Button logoutButton = (Button) findViewById(R.id.button_logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(
                        getString(R.string.pref_file_name), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("token", "");
                editor.apply();
                Intent i = new Intent(context, LoginActivity.class);
                startActivity(i);
            }
        });
    }
    protected void switchToHunts(){
        Intent intent = new Intent(this, HuntList.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }
    protected void switchToLogin(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
    protected void switchToGallery(){
        Intent intent = new Intent(this, PictureGallery.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

}
