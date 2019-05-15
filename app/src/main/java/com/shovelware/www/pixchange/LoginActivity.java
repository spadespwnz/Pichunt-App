package com.shovelware.www.pixchange;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

public class LoginActivity extends AppCompatActivity {
    String url;
    AsyncHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        httpClient = new AsyncHttpClient();
        url = getString(R.string.server_url) + getString(R.string.user_route);
        Button signupButton = (Button) findViewById(R.id.signup_button);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });
        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        checkToken();
    }
    protected void checkToken(){
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE);
        String token = sharedPref.getString("token", "");
        if (token.equals("")) return;

        httpClient.get(url + "tokenCheck/" + token, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String res = new String(responseBody, "UTF-8");
                    if (res.equals("true")){
                        Intent intent = new Intent(getBaseContext(), ParentActivity.class);
                        startActivity(intent);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }
    protected void signup(){
        RequestParams params = new RequestParams();
        params.add("username", ((TextView)findViewById(R.id.login_username)).getText().toString());
        params.add("password", ((TextView)findViewById(R.id.login_password)).getText().toString());
        httpClient.post(url + "signup", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONObject res = null;
                boolean success = false;
                try {
                    res = new JSONObject(new String(responseBody));
                    success = res.getBoolean("success");
                } catch (JSONException e) {
                    e.printStackTrace();
                    createDialog("Error", "Error Parsing Response From Server. success value missing from response.");
                }

                if (!success){
                    try {
                        String err = res.getString("err");
                        createDialog("Error", err);
                    } catch (JSONException e) {
                        createDialog("Error", "Error Parsing Response From Server. err value missing from response.");
                    }

                    return;
                }
                try {
                    String token = res.getString("token");
                    Context context = getApplicationContext();
                    SharedPreferences sharedPref = context.getSharedPreferences(
                            getString(R.string.pref_file_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();

                    editor.putString("token", token);
                    editor.commit();
                    Toast.makeText(LoginActivity.this, "Account Created.",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getBaseContext(), ParentActivity.class);
                    startActivity(intent);
                } catch (JSONException e) {
                    System.out.println(e.getMessage());
                    createDialog("Error", "Error Parsing Response From Server. token value missing from response.");
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                System.out.println(error.getMessage());
                createDialog("Error", "Could not connect to server.");
            }
        });
    }
    protected void login() {
        RequestParams params = new RequestParams();
        params.add("username", ((TextView)findViewById(R.id.login_username)).getText().toString());
        params.add("password", ((TextView)findViewById(R.id.login_password)).getText().toString());
        httpClient.post(url + "login", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONObject res = null;
                boolean success = false;
                try {
                    res = new JSONObject(new String(responseBody));
                    success = res.getBoolean("success");
                } catch (JSONException e) {
                    e.printStackTrace();
                    createDialog("Error", "Error Parsing Response From Server. success value missing from response.");
                }

                if (!success){
                    try {
                        String err = res.getString("err");
                        createDialog("Error", err);
                    } catch (JSONException e) {
                        createDialog("Error", "Error Parsing Response From Server. err value missing from response.");
                    }

                    return;
                }

                try {
                    String token = res.getString("token");
                    Context context = getApplicationContext();
                    SharedPreferences sharedPref = context.getSharedPreferences(
                            getString(R.string.pref_file_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();

                    editor.putString("token", token);
                    editor.commit();

                    Toast.makeText(LoginActivity.this, "Login Success.",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getBaseContext(), ParentActivity.class);
                    startActivity(intent);
                } catch (JSONException e) {
                    createDialog("Error", "Error Parsing Response From Server. token value missing from response.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                createDialog("Error", "Could not connect to server.\n"+error.getMessage());

            }
        });
    }

    protected void createDialog(String title, String content){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(content);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}