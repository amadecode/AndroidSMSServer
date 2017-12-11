package com.hfmp.server;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    ImageView imgNoInternet;
    ImageView imgWithInternet;
    TextView txtConnectionStatus;
    TextView txtCount;
    ProgressBar progressBar;

    DatabaseReference databaseReference;
    List<SMS> sms;

    Handler mHandler = new Handler();
    boolean isRunning = true;
    boolean isConnected = false;

    public static final String SMS_SENT_ACTION = "com.hfmp.server.SMS_SENT_ACTION";
    public static final String SMS_DELIVERED_ACTION = "com.hfmp.server.SMS_DELIVERED_ACTION";

    int loading = 1000;

    SMS currentSMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sms = new ArrayList<SMS>();
        imgNoInternet = (ImageView) findViewById(R.id.imgNoInternet);
        imgWithInternet = (ImageView) findViewById(R.id.imgWithInternet);
        txtConnectionStatus = (TextView) findViewById(R.id.txtConnectionStatus);
        txtCount = (TextView) findViewById(R.id.txtCount);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        databaseReference = FirebaseDatabase.getInstance().getReference("sms");

        imgNoInternet.setVisibility(View.INVISIBLE);
        imgWithInternet.setVisibility(View.INVISIBLE);
        txtConnectionStatus.setText("Loading");

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.SEND_SMS)){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.SEND_SMS},1);
            }else{
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
            }
        }else{
            // do nothing
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (isRunning) {
                    try {
                        Thread.sleep(loading);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                // Write your code here to update the UI.
                                displayData();
                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (isRunning) {
                    try {
                        Thread.sleep(10000);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                // Write your code here to update the UI.

                                if(isConnected) {
                                    databaseReference.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                                String key = postSnapshot.getKey();
                                                SMS _sms = postSnapshot.getValue(SMS.class);
                                                if(!_sms.getSent()){
                                                    Toast.makeText(MainActivity.this, "Sending SMS : " + key, Toast.LENGTH_SHORT).show();
                                                    _sms.setSent(true);
                                                    currentSMS = _sms;
                                                    databaseReference.child(key).setValue(_sms).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            sendSMS(currentSMS.getTo(),currentSMS.getMsg());
                                                        }
                                                    });
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = 0;
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()){
                    SMS _sms = postSnapshot.getValue(SMS.class);
                    sms.add(_sms);
                    if(!_sms.getSent())count++;
                }
                txtCount.setText(count+"");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1: {
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(this, "No permission granted!", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
            }
        }
    }

    public void displayData(){
        ConnectivityManager ConnectionManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=ConnectionManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()==true ){
            //Toast.makeText(MainActivity.this, "Network Available", Toast.LENGTH_LONG).show();
            connectionStatus(true);
        }else{
            //Toast.makeText(MainActivity.this, "Network Not Available", Toast.LENGTH_LONG).show();
            connectionStatus(false);
        }
    }

    public void connectionStatus(boolean status){
        progressBar.setVisibility(View.INVISIBLE);
        if(status){
            imgNoInternet.setVisibility(View.INVISIBLE);
            imgWithInternet.setVisibility(View.VISIBLE);
            txtConnectionStatus.setText("Connected");
            isConnected = true;
        }else{
            imgNoInternet.setVisibility(View.VISIBLE);
            imgWithInternet.setVisibility(View.INVISIBLE);
            txtConnectionStatus.setText("No Internet Connection");
            isConnected = false;
        }
        loading = 100;

    }

    public void sendSMS(String to, String msg){
        String number = to;
        String sms = msg;
        try{
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, sms, null, null);
            Toast.makeText(MainActivity.this, "Sent!", Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_LONG).show();
        }
    }
}
