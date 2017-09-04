package com.example.morga.mystepcounter;



import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

 /**

     * Combine Recording API and History API of the Google Fit platform
     * to record steps, and display the daily current step count. It also demonstrates how to
     * authenticate a user with Google Play Services.
     */

    public class MainActivity extends AppCompatActivity {

     public static final String TAG = "StepCounter";
     private GoogleApiClient mClient = null;
     private Button mCancelSubscriptionsBtn;
     private Button mShowSubscriptionsBtn;

     private Toolbar toolbar;
     private TabLayout tabLayout;
     private ViewPager viewPager;



        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);


            initViews();

            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            viewPager = (ViewPager) findViewById(R.id.viewpager);
            setupViewPager(viewPager);
            tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);




            buildFitnessClient();
        }

     private void initViews() {






     }

     private void setupViewPager(ViewPager viewPager) {
            ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
            adapter.addFragment(new OneFragment(), "DAILY");
            adapter.addFragment(new TwoFragment(), "WEEK");
            adapter.addFragment(new ThreeFragment(), "SETUP");
            viewPager.setAdapter(adapter);
        }

        class ViewPagerAdapter extends FragmentPagerAdapter {
            private final List<Fragment> mFragmentList = new ArrayList<>();
            private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
        }


        /**
         * Build a {@link GoogleApiClient} to authenticate the user and allow the application
         * to connect to the Fitness APIs.
         * to resolve authentication failures (for example, the user has not signed in
         * before, or has multiple accounts and must specify which account to use).
         */
        private void buildFitnessClient() {
            // Create the Google API Client
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.HISTORY_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {

                                @Override
                                public void onConnected(Bundle bundle) {
                                    Log.i(TAG, "Connected!!!");
                                    Toast.makeText(getApplicationContext(), "Connected!",Toast.LENGTH_SHORT).show();
                                    // Now you can make calls to the Fitness APIs.  What to do?
                                    // Subscribe to some data sources!
                                    subscribe();
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    // If your connection to the sensor gets lost at some point,
                                    // you'll be able to determine the reason and react to it here.
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.w(TAG, "Connection lost.  Cause: Network Lost.");
                                        Toast.makeText(getApplicationContext(), "Connection lost",Toast.LENGTH_LONG).show();
                                    } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Log.w(TAG, "Connection lost.  Reason: Service Disconnected");
                                        Toast.makeText(getApplicationContext(), "Disconnected",Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.w(TAG, "Google Play services connection failed. Cause: " +
                                    result.toString());
                            Snackbar.make(
                                    MainActivity.this.findViewById(R.id.main_activity_view),
                                    "Exception while connecting to Google Play services: " +
                                            result.getErrorMessage(),
                                    Snackbar.LENGTH_INDEFINITE).show();
                        }
                    })
                    .build();
        }

        /**
         * Record step data by requesting a subscription to background step data.
         */
        public void subscribe() {
            // To create a subscription, invoke the Recording API. As soon as the subscription is
            // active, fitness data will start recording.

            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode()
                                        == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.i(TAG, "Existing subscription for activity detected.");
                                    Toast.makeText(getApplicationContext(), "Existing subscription for activity detected.",Toast.LENGTH_LONG).show();
                                } else {
                                    Log.i(TAG, "Successfully subscribed!");
                                    Toast.makeText(getApplicationContext(), "Successfully subscribed!",Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.w(TAG, "There was a problem subscribing.");
                                Toast.makeText(getApplicationContext(), "There was a problem subscribing.",Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        /**
         * Read the current daily step total, computed from midnight of the current day
         * on the device's current timezone.
         */

        private class VerifyDataTask extends AsyncTask<Void, Void, Void> {
            protected Void doInBackground(Void... params) {

                long total = 0;

                PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA);
                DailyTotalResult totalResult = result.await(5, TimeUnit.SECONDS);
                if (totalResult.getStatus().isSuccess()) {
                    DataSet totalSet = totalResult.getTotal();
                    total = totalSet.isEmpty()
                            ? 0
                            : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                } else {
                    Log.w(TAG, "There was a problem getting the step count.");
                }

                Log.i(TAG, "Total steps: " + total);
                Snackbar.make(
                        MainActivity.this.findViewById(R.id.main_activity_view),
                        "Steps: " + total,
                        Snackbar.LENGTH_SHORT).show();


                return null;
            }

            
        }



        private void readData() {
            new VerifyDataTask().execute();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the main; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_read_data) {
                readData();
                return true;
            }
            if (id == R.id.start) {
                subscribe();
                return true;
            }
            if (id == R.id.pause) {
                cancelSubscriptions();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

     private void cancelSubscriptions() {
             // To create a subscription, invoke the Recording API. As soon as the subscription is
             // active, fitness data will start recording.

             Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                     .setResultCallback(new ResultCallback<Status>() {
                         @Override
                         public void onResult(Status status) {
                             if (status.isSuccess()) {
                                 if (status.getStatusCode()
                                         == FitnessStatusCodes.SUCCESS_LISTENER_NOT_REGISTERED_FOR_FITNESS_DATA_UPDATES) {
                                     Log.i(TAG, "Already unsubscribed.");
                                     Toast.makeText(getApplicationContext(), "Already unsubscribed.",Toast.LENGTH_LONG).show();
                                 } else {
                                     Log.i(TAG, "Successfully unsubscribed!");
                                     Toast.makeText(getApplicationContext(), "Successfully unsubscribed!",Toast.LENGTH_LONG).show();
                                 }
                             } else {
                                 Log.w(TAG, "There was a problem unsubscribing.");
                                 Toast.makeText(getApplicationContext(), "There was a problem unsubscribing.",Toast.LENGTH_LONG).show();
                                 setContentView(R.layout.fragment_one);
                             }
                         }
                     });
         }


 }