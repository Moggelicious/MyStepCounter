package com.example.morga.mystepcounter;

import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
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
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Combine Recording API and History API of the Google Fit platform
 * to record steps, and display the daily current step count. It also demonstrates how to
 * authenticate a user with Google Play Services.
 */

public class MainActivity extends AppCompatActivity implements IFragmentToActivity {

    public static final String TAG = "StepCounter";
    private GoogleApiClient mClient = null;

    private PagerAdapter adapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        ArrayList<String> tabs = new ArrayList<>();
        tabs.add("DAILY STEPS");
        tabs.add("LAST SEVEN DAYS");
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        adapter = new PagerAdapter(getSupportFragmentManager(), tabs);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        TabFragment2 tabFragment2 = new TabFragment2();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, tabFragment2);
        transaction.commit();


        buildFitnessClient();
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
            int position = tabLayout.getSelectedTabPosition();
            Fragment fragment = adapter.getFragment(tabLayout.getSelectedTabPosition());
            if (fragment != null) {
                switch (position) {
                    case 0:
                        ((TabFragment1) fragment).onRefresh();
                        break;
                    case 2:
                        ((TabFragment2) fragment).onRefresh();
                        break;
                }
            }
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
        if (id == R.id.week) {
            readWeek();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * templates with interface IFragmentToActivity
     * to communicate between fragments and to fragments
     * authenticate a user with Google Play Services.
     */
    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void communicateToFragment2() {
        TabFragment2 fragment = (TabFragment2) adapter.getFragment(1);
        if (fragment != null) {
            fragment.fragmentCommunication();
        } else {
            Log.i(TAG, "Fragment 2 is not initialized");
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
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");


                                /**
                                 * autosubscribe and autoread daily steps
                                 */
                                subscribe();
                                readData();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.w(TAG, "Connection lost.  Cause: Network Lost.");
                                    Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_LONG).show();
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.w(TAG, "Connection lost.  Reason: Service Disconnected");
                                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG).show();
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
                                MainActivity.this.findViewById(R.id.main_content),
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
                               // Toast.makeText(getApplicationContext(), "Existing subscription for activity detected.", Toast.LENGTH_LONG).show();
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                                Snackbar.make(
                                        MainActivity.this.findViewById(R.id.main_content),
                                         "You are connected!",
                                        Snackbar.LENGTH_SHORT).show();
                                //Toast.makeText(getApplicationContext(), "Successfully subscribed!", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "There was a problem subscribing.");
                            Snackbar.make(
                                    MainActivity.this.findViewById(R.id.main_content),
                                    "There was a problem connecting.",
                                    Snackbar.LENGTH_SHORT).show();
                            //Toast.makeText(getApplicationContext(), "There was a problem subscribing.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
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
                                Toast.makeText(getApplicationContext(), "Already unsubscribed.", Toast.LENGTH_LONG).show();
                            } else {
                                Log.i(TAG, "Successfully unsubscribed!");
                                Snackbar.make(
                                        MainActivity.this.findViewById(R.id.main_content),
                                        "Pause mode on!",
                                        Snackbar.LENGTH_SHORT).show();
                                //Toast.makeText(getApplicationContext(), "Successfully unsubscribed!", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "There was a problem unsubscribing.");
                            Toast.makeText(getApplicationContext(), "There was a problem unsubscribing.", Toast.LENGTH_LONG).show();
                            setContentView(R.layout.activity_main);
                        }
                    }
                });
    }

    /**
     * Read the current daily step total, computed from midnight of the current day
     * on the device's current timezone.
     */

    private class DailySteps extends AsyncTask<Void, Void, Long> {

        TextView steps = (TextView) findViewById(R.id.mySteps);
        protected Long doInBackground(Void... params) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    steps.setText("");
                }
            });

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

            return total;
        }

        @Override
        protected void onPostExecute(Long total) {
            TabFragment1 tabFrag1 = (TabFragment1) getSupportFragmentManager().findFragmentById(R.id.tab1);
            if (tabFrag1 != null) {
                tabFrag1.onRefresh();

            } else {
                TabFragment1 newTabFrag1 = new TabFragment1();
                Bundle args = new Bundle();
                args.putLong("steps", total);
                newTabFrag1.setArguments(args);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.tab1, newTabFrag1);
                transaction.addToBackStack(null);
                transaction.commit();
            }
            steps.setText(String.valueOf(total));

            /**
             * If I want a timer to auto-update
             * stepData, but its unnecessary.
             */

            //Timer myTimer = new Timer();
            //myTimer.schedule(new TimerTask() {
              //@Override
            //public void run() {
              // readData();
            //}
            //}, 5000);
        }
    }

    private DataReadRequest displayLastWeeksData() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = DateFormat.getDateInstance();
        Log.e("History", "Range Start: " + dateFormat.format(startTime));
        Log.e("History", "Range End: " + dateFormat.format(endTime));

        Snackbar.make(
                MainActivity.this.findViewById(R.id.main_content),
                String.format(dateFormat.format(startTime) + " " + dateFormat.format(endTime)),
                Snackbar.LENGTH_SHORT).show();

        //Check how many steps were walked and recorded in the last 7 days

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);

        //Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    showDataSet(dataSet);
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.e("History", "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                showDataSet(dataSet);
            }
        }
        return readRequest;
    }

    private void showDataSet(DataSet dataSet) {
         TextView sevenDays = (TextView) findViewById(R.id.seven_days);
        Log.e("History", "Data returned for Data type: " + dataSet.getDataType().getName());

        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.e("History", "Data point:");
            Log.e("History", "\tType: " + dp.getDataType().getName());
            Log.e("History", "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.e("History", "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.e("History", "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    private class ViewWeekStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            displayLastWeeksData();

            return null;
        }
    }

    private void readData() {
        new DailySteps().execute();
    }

    private void readWeek() {
        new ViewWeekStepCountTask().execute();
    }
}
