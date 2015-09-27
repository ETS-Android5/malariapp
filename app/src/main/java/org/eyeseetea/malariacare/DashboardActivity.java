/*
 * Copyright (c) 2015.
 *
 * This file is part of Facility QA Tool App.
 *
 *  Facility QA Tool App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Facility QA Tool App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.raizlabs.android.dbflow.sql.language.Select;

import org.eyeseetea.malariacare.database.model.Tab;
import org.eyeseetea.malariacare.database.model.User;
import org.eyeseetea.malariacare.database.utils.PopulateDB;
import org.eyeseetea.malariacare.database.utils.Session;
import org.eyeseetea.malariacare.fragments.DashboardSentFragment;
import org.eyeseetea.malariacare.fragments.DashboardUnsentFragment;
import org.eyeseetea.malariacare.services.SurveyService;

import java.io.IOException;
import java.util.List;


public class DashboardActivity extends BaseActivity {

    private final static String TAG=".DDetailsActivity";

    private LocationListener locationListener;

    private boolean reloadOnResume=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dashboard);

        try {
            initDataIfRequired();
            loadSessionIfRequired();
        } catch (IOException e){
            Log.e(".DashboardActivity", e.getMessage());
        }

        if (savedInstanceState == null) {
            DashboardUnsentFragment detailsFragment = new DashboardUnsentFragment();
            detailsFragment.setArguments(getIntent().getExtras());
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.dashboard_details_container, detailsFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            DashboardSentFragment completedFragment = new DashboardSentFragment();
            detailsFragment.setArguments(getIntent().getExtras());
            FragmentTransaction ftr = getFragmentManager().beginTransaction();
            ftr.add(R.id.dashboard_completed_container, completedFragment);
            ftr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ftr.commit();
        }
    }

    @Override
    protected void initTransition(){
        this.overridePendingTransition(R.transition.anim_slide_in_right, R.transition.anim_slide_out_right);
    }

    @Override
    public void onResume(){
        super.onResume();
        getSurveysFromService();

        prepareLocationListener();
    }

    @Override
    public void onPause(){
        super.onPause();

        //No locationListener working no need to unregister
        if(locationListener==null){
            return;
        }
        LocationManager locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
    }

    private void prepareLocationListener(){
        locationListener=new DashboardLocationListener();
        LocationManager locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d(TAG,"requestLocationUpdates via GPS");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
        }

        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            Log.d(TAG,"requestLocationUpdates via NETWORK");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
        }else{
            locationListener=null;
            Location lastLocation=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Log.d(TAG,"location not available via GPS|NETWORK, last know: "+lastLocation);
            Session.setLocation(lastLocation);
        }
    }

    public void setReloadOnResume(boolean doReload){
        this.reloadOnResume=false;
    }

    public void getSurveysFromService(){
        Log.d(TAG, "getSurveysFromService ("+reloadOnResume+")");
        if(!reloadOnResume){
            //El flag se consume
            reloadOnResume=true;
            return;
        }
        Intent surveysIntent=new Intent(this, SurveyService.class);
        surveysIntent.putExtra(SurveyService.SERVICE_METHOD, SurveyService.RELOAD_DASHBOARD_ACTION);
        this.startService(surveysIntent);
    }

    /**
     * Just to avoid trying to navigate back from the dashboard. There's no parent activity here
     */
    @Override
    public void onBackPressed() {
        Log.d(".DashboardDetails", "back pressed");
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit the app?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).create().show();
    }

    public class DashboardLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG,"onLocationChanged "+location.toString());
            Session.setLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

/**
     * In case data is not yet populated (detected by looking at the Tab table) we populate the data
     * @throws IOException in case any IO error occurs while populating DB
     */
    private void initDataIfRequired() throws IOException {
        if (new Select().count().from(Tab.class).count()!=0) {
            return;
        }

        Log.i(".DashboardActivity", "Populating DB");

        // This is only executed the first time the app is loaded
        try {
            User user = new User();
            user.save();
            PopulateDB.populateDB(getAssets());
        } catch (IOException e) {
            Log.e(".DashboardActivity", "Error populating DB", e);
            throw e;
        }
        Log.i(".DashboardActivity", "DB populated");
    }

    /**
     * In case Session doesn't have the user set, here we set it to the first entry of User table
     */
    private void loadSessionIfRequired(){
        if (Session.getUser() == null){
            List<User> users = new Select().all().from(User.class).queryList();
            if (users.size() == 0){
                User user = new User();
                user.setName("");
                user.save();
                Session.setUser(user);
            } else {
                Session.setUser(users.get(0));
            }
        }
    }
}
