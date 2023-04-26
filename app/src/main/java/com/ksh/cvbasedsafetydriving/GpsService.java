/*
 * Create by KSH on 2020. 8. 18.
 * Copyright (c) 2020. KSH. All rights reserved.
 */

package com.ksh.cvbasedsafetydriving;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GpsService extends Service {

    private final IBinder mBinder = new ReturnBinder();

    private LocationManager locationManager;

    private ArrayList<String> arrayList = null;

    private HashMap<String, Integer> guGunMap;

    private int guGunCode = 0;
    private boolean animationState = false;

    private Animation animation;

    public class ReturnBinder extends Binder {
        GpsService getService() {
            return GpsService.this;
        }
    }

    public GpsService() {
    }

    public void locationKnown() {
        Log.d("su", "service func call");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (location != null) {
            //Log.d("Location", "Location Fail");
            Log.d("Location", "Latitude : " + location.getLatitude() + " Longitude : " + location.getLongitude() + " Time : " + location.getTime());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        guGunMap = new HashMap<String, Integer>();
        guGunInput();

        animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(250);
        animation.setStartOffset(0);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, gpsLocationListener);
    }

    private void guGunInput()
    {
        guGunMap.put("강남구", 680);
        guGunMap.put("강동구", 740);
        guGunMap.put("강북구", 305);
        guGunMap.put("강서구", 500);
        guGunMap.put("관악구", 620);
        guGunMap.put("광진구", 215);
        guGunMap.put("구로구", 530);
        guGunMap.put("금천구", 545);
        guGunMap.put("노원구", 350);
        guGunMap.put("도봉구", 320);
        guGunMap.put("동대문구", 230);
        guGunMap.put("동작구", 590);
        guGunMap.put("마포구", 440);
        guGunMap.put("서대문구", 410);
        guGunMap.put("서초구", 650);
        guGunMap.put("성동구", 200);
        guGunMap.put("성북구", 290);
        guGunMap.put("송파구", 710);
        guGunMap.put("양천구", 470);
        guGunMap.put("영등포구", 560);
        guGunMap.put("용산구", 170);
        guGunMap.put("은평구", 380);
        guGunMap.put("종로구", 110);
        guGunMap.put("중구", 140);
        guGunMap.put("중랑구", 260);
    }

    private void locationCompare(Double latitude, Double longitude)
    {
        for(int i = 0; i < arrayList.size(); i++)
        {
            String[] splitArray = arrayList.get(i).split("/");

            //double tmpLongitude = Double.parseDouble(String.format("%.2f", Double.parseDouble(splitArray[0])));
            //double tmpLatitude = Double.parseDouble(String.format("%.2f", Double.parseDouble(splitArray[1])));

            //double tmpLatitude = Math.round((Double.parseDouble(splitArray[1]))*100)/100.0;
            //double tmpLongitude = Math.round((Double.parseDouble(splitArray[0]))*100)/100.0;
            double tmpLatitude = Double.parseDouble(splitArray[1]);
            double tmpLongitude = Double.parseDouble(splitArray[0]);

            //Log.d("Location", tmpLatitude + ", " + tmpLongitude);

            if(((tmpLatitude - 0.01) < latitude) && ((tmpLatitude + 0.01) > latitude)) // latitude
            {
                if(((tmpLongitude - 0.01) < longitude) && ((tmpLongitude + 0.01) > longitude)) // longitude
                {
                    Log.d("Danger", "Dangerous");

                    CameraView.bicycleRiskView.setVisibility(View.VISIBLE);
                    CameraView.bicycleRiskView.setTextColor(ContextCompat.getColor(this, R.color.ORANGE));
                    CameraView.bicycleRiskView.startAnimation(animation);

                    break;
                }
                else
                {
                    Log.d("Danger", "Not Dangerous1");

                    CameraView.bicycleRiskView.setVisibility(View.INVISIBLE);
                }
            }
            else
            {
                Log.d("Danger", "Not Dangerous2");

                CameraView.bicycleRiskView.setVisibility(View.INVISIBLE);
            }
        }

        //arrayList.clear();
    }

    private void roadRiskAnalysis(Double latitude, Double longitude)
    {
        int riskGrade = 0;

        try
        {
            String res = new RoadRisk().execute(latitude, longitude).get();

            riskGrade = roadRiskJsonParser(res);
        }
        catch(InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }

        CameraView.roadRiskView.setText("현재 주행도로 상황 : ");

        if(animationState == true)
        {
            CameraView.roadRiskGrade.clearAnimation();

            animationState = false;
        }

        if(riskGrade == 0)
        {
            // 파싱 에러
            Log.d("RoadRisk", "Parsing Error");

            CameraView.roadRiskGrade.setText("정보 없음");
            CameraView.roadRiskGrade.setTextColor(Color.GRAY);
        }
        else if(riskGrade == 1)
        {
            // 안전
            Log.d("RoadRisk", "Grade : " + riskGrade);

            CameraView.roadRiskGrade.setText("안전");
            CameraView.roadRiskGrade.setTextColor(Color.GREEN);
        }
        else if(riskGrade == 2)
        {
            // 주의
            Log.d("RoadRisk", "Grade : " + riskGrade);

            CameraView.roadRiskGrade.setText("주의");
            CameraView.roadRiskGrade.setTextColor(Color.YELLOW);
        }
        else if(riskGrade == 3)
        {
            // 심각
            Log.d("RoadRisk", "Grade : " + riskGrade);

            CameraView.roadRiskGrade.setText("심각");
            CameraView.roadRiskGrade.setTextColor(ContextCompat.getColor(this, R.color.ORANGE));
        }
        else if(riskGrade >= 4)
        {
            // 위험
            Log.d("RoadRisk", "Grade : " + riskGrade);

            CameraView.roadRiskGrade.setText("위험");
            CameraView.roadRiskGrade.setTextColor(Color.RED);
            CameraView.roadRiskGrade.startAnimation(animation);

            animationState = true;
        }

        CameraView.roadRiskView.setVisibility(View.VISIBLE);
        CameraView.roadRiskGrade.setVisibility(View.VISIBLE);
    }

    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            Address address = null;

            //double latitude = Double.parseDouble(String.format("%.2f", location.getLatitude()));
            //double longitude = Double.parseDouble(String.format("%.2f", location.getLongitude()));

            //double latitude = Math.round(location.getLatitude()*100)/100.0;
            //double longitude = Math.round(location.getLongitude()*100)/100.0;
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            Geocoder geocoder = new Geocoder(GpsService.this, Locale.KOREA);

            try
            {
                List<Address> addressList = geocoder.getFromLocation(Math.round(latitude*100)/100.0, Math.round(longitude*100)/100.0, 1);

                address = addressList.get(0);

                //ArrayList<String> addressArrayList = new ArrayList<String>();

                if(address.getSubLocality() != null)
                {
                    Log.d("Address", address.getSubLocality());

                    int code = guGunMap.get(address.getSubLocality());

                    Log.d("guGunCode", String.valueOf(code));

                    if(code != guGunCode)
                    {
                        guGunCode = code;

                        try {
                            String guGunCodeString = Integer.toString(guGunCode);

                            String res = new Bicycle().execute(guGunCodeString).get();
                            //Log.d("TaskRes", res);

                            arrayList = bicycleJsonParser(res);
                        }
                        catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }

                    locationCompare(latitude, longitude);
                }

                /*
                for(int i = 0; i <= addressList.size(); i++)
                {
                    addressArrayList.add(address.getAddressLine(i));
                    Log.d("Address", addressArrayList.get(i));
                }
                */

                //Log.d("Address", address);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            roadRiskAnalysis(location.getLatitude(), location.getLongitude());

            //Log.d("Location", "Latitude : " + location.getLatitude() + " Longitude : " + location.getLongitude() + " Time : " + location.getTime());
            Log.d("Location", latitude + ", " + longitude);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    public ArrayList<String> bicycleJsonParser(String jsonResult)
    {
        ArrayList<String> arrayList = new ArrayList<>();

        try
        {
            String tmp = new JSONObject(jsonResult).getString("items");

            JSONArray jsonArray = new JSONObject(tmp).getJSONArray("item");

            for(int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String result = jsonObject.getString("lo_crd").concat("/");
                result = result.concat(jsonObject.getString("la_crd"));

                arrayList.add(result);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return arrayList;
    }

    public int roadRiskJsonParser(String jsonResult)
    {
        int result = 0;

        try
        {
            String resultMsg = new JSONObject(jsonResult).getString("resultMsg");

            if(resultMsg.equals("INVALID_REQUEST_PARAMETER_ERROR"))
            {
                return 0;
            }
            else
            {
                String tmp = new JSONObject(jsonResult).getString("items");

                JSONArray jsonArray = new JSONObject(tmp).getJSONArray("item");

                JSONObject jsonObject = jsonArray.getJSONObject(0);

                result = Integer.parseInt(jsonObject.getString("anals_grd"));
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return true;
    }

    @Override
    public void onDestroy()
    {
        locationManager.removeUpdates(gpsLocationListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }

    public class Bicycle extends AsyncTask<String, Void, String>
    {
        private String jsonResult;
        private String str;

        //ProgressDialog progressDialog;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            //progressDialog = ProgressDialog.show(GpsService.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);

            //progressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... guGunCode)
        {
            URL url;

            String bicycleUrl = "http://taas.koroad.or.kr/data/rest/frequentzone/bicycle?authKey=ITyMgfLEEiBFBIJQqzZ7zHH68MeBvaSx1IOvWFD33i9yDtglWYEBvjwRbew2DbdE&searchYearCd=2020037&sido=11&guGun="
                    + guGunCode[0] + "&type=json";

            try
            {
                url = new URL(bicycleUrl);

                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                /*connection.setReadTimeout(5000);
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Accept-Charset", "utf-8");
                connection.connect();
                */
                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    InputStreamReader tmp = new InputStreamReader(connection.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuffer buffer = new StringBuffer();

                    while((str = reader.readLine()) != null)
                    {
                        buffer.append(str);
                    }

                    jsonResult = buffer.toString();

                    reader.close();
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            Log.d("JsonResultBicycle", jsonResult);

            return jsonResult;
        }
    }

    public class RoadRisk extends AsyncTask<Double, Void, String>
    {
        private String jsonResult;
        private String str;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(Double... location)
        {
            URL url;

            String roadRiskUrl = "http://taas.koroad.or.kr/data/rest/road/dgdgr/link?authKey=XKtdqtNGlRzTcALHxmpwk1ROXlOYEovcOkE7TlA8rld5JxHraJNduf8V%2FYVzz%2FJX&searchLineString=LineString%20("
                                + location[1] + "%20" + location[0] + ",%20" + location[1] + "%20" + location[0] + ")&vhctyCd=01&type=json";

            try
            {
                url = new URL(roadRiskUrl);

                HttpURLConnection connection = (HttpURLConnection)url.openConnection();

                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    InputStreamReader tmp = new InputStreamReader(connection.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuffer buffer = new StringBuffer();

                    while((str = reader.readLine()) != null)
                    {
                        buffer.append(str);
                    }

                    jsonResult = buffer.toString();

                    reader.close();
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            Log.d("JsonResultRoadRisk", jsonResult);

            return jsonResult;
        }
    }
}
