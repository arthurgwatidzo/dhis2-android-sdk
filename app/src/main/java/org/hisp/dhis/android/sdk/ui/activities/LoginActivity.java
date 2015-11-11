/*
 * Copyright (c) 2015, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.sdk.ui.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import java.net.URL;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.squareup.okhttp.HttpUrl;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.DhisService;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.network.Credentials;
import org.hisp.dhis.android.sdk.persistence.preferences.AppPreferences;
import org.hisp.dhis.android.sdk.network.APIException;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.utils.UiUtils;

/**
 *
 */
public class LoginActivity extends Activity implements OnClickListener {
    /**
     *
     */
    private final static String CLASS_TAG = "LoginActivity";

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Spinner serverSpinner;
    private Button loginButton;
    private ProgressBar progressBar;
    private View viewsContainer;

    private AppPreferences mPrefs;
    // declare the dialog as a member field of your activity
    ProgressDialog mProgressDialog;
    // public static Date sdate,newdevicedate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mPrefs = new AppPreferences(getApplicationContext());
        setupUI();

        /*
        @Arthur :Functionality to add the Auto-Download + Auto-Update Functionality
         */
        //@Arthur:instantiate it within the onCreate method
        mProgressDialog = new ProgressDialog(LoginActivity.this);
        mProgressDialog.setMessage("A new app update is available");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);


        //@Arthur:execute this when the downloader must be fired
        final DownloadTask downloadTask = new DownloadTask(LoginActivity.this);
        downloadTask.execute("http://hisp.org/downloads/public/dhis2/mobile/app-release.apk");

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
                Log.e(CLASS_TAG, "onCancel Method Invoked!");
            }
        });

            downloadTask.isFlagUpToDate();

        Log.v("FLAG_UP_TO_DATE", "The flag's status is currently :" + downloadTask.isFlagUpToDate());
        //if(downloadTask.isCancelled()||downloadTask.getStatus()== AsyncTask.Status.FINISHED){
        if(downloadTask.isCancelled()||downloadTask.getStatus()== AsyncTask.Status.FINISHED){
            Log.v("ASYNC_TASK_CANCELLED","The download task has been interrupted and aborted");
            downloadTask.cancel(true);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Dhis2Application.bus.unregister(this);
    }

    //@Arthur : method to do APK installation only
    public void performAPKInstall(String filename){

        File apkfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/app-release.apk");
        if (!apkfile.exists()) {
            return;
        }
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.setDataAndType(
                Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        startActivity(installIntent);

    } //End performAPKInstall

    @Override
    public void onResume() {
        super.onResume();
        Dhis2Application.bus.register(this);
    }

    /**
     * Sets up the initial UI elements
     */
    private void setupUI() {
        viewsContainer = findViewById(R.id.login_views_container);
        usernameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);
        serverSpinner = (Spinner) findViewById(R.id.server_url);
        loginButton = (Button) findViewById(R.id.login_button);

        String server = null;//mPrefs.getServerUrl();
        String username = null;//mPrefs.getUsername();
        String password = null;

        DhisController.getInstance().init();
        if(DhisController.isUserLoggedIn()) {
            server = DhisController.getInstance().getSession().getServerUrl().toString();
            username = DhisController.getInstance().getSession().getCredentials().getUsername();
            password = DhisController.getInstance().getSession().getCredentials().getPassword();
        }

        if (server == null) {
            server = "https://";
        }

        if (username == null) {
            username = "";
            password = "";
        }

       // serverEditText.setText(server);
        usernameEditText.setText(username);
        passwordEditText.setText(password);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        loginButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // If default spinner option is selected then user should be warned
        if(serverSpinner.getSelectedItemPosition()==0) {
            Toast.makeText(LoginActivity.this,
                    "       ERROR      " +
                            "\nPlease Select a Server URL", Toast.LENGTH_LONG).show();
            return;
        }
        String serverURL = serverSpinner.getSelectedItem().toString();

        //remove whitespace as last character for username
        if (username.charAt(username.length() - 1) == ' ') {
            username = username.substring(0, username.length() - 1);
        }

        login(serverURL, username, password);
    }

    public void login(String serverUrl, String username, String password) {
        showProgress();
        //NetworkManager.getInstance().setServerUrl(serverUrl);
        //NetworkManager.getInstance().setCredentials(NetworkManager.getInstance().getBase64Manager()
        //        .toBase64(username, password));
        //Dhis2.getInstance().saveCredentials(this, serverUrl, username, password);
        //Dhis2.getInstance().login(onLoginCallback, username, password);
        HttpUrl serverUri = HttpUrl.parse(serverUrl);
        DhisService.logInUser(
                serverUri, new Credentials(username, password)
        );
    }

    @Subscribe
    public void onLoginFinished(NetworkJob.NetworkJobResult<ResourceType> result) {
        if(result!=null && result.getResourceType().equals(ResourceType.USERS)) {
            if(result.getResponseHolder().getApiException() == null) {
                launchMainActivity();
            } else {
                onLoginFail(result.getResponseHolder().getApiException());
            }
        }
    }

    private void showProgress() {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.out_up);
        viewsContainer.startAnimation(anim);
        viewsContainer.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void onLoginFail(APIException e) {
        Dialog.OnClickListener listener = new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLoginDialog();
            }
        };

        if (e.getResponse() == null) {
            String type = "error";
            //if (e.isHttpError()) type = "HttpError";
            //else if (e.isUnknownError()) type = "UnknownError";
            //else if (e.isNetworkError()) type = "NetworkError";
            //else if (e.isConversionError()) type = "ConversionError";
            UiUtils.showErrorDialog(this, getString(R.string.error_message), type + ": "
                    + e.getMessage(), listener);
        } else {
            if (e.getResponse().getStatus() == 401) {
                UiUtils.showErrorDialog(this, getString(R.string.error_message),
                        getString(R.string.invalid_username_or_password), listener);
            } else {
                UiUtils.showErrorDialog(this, getString(R.string.error_message),
                        getString(R.string.unable_to_login) + " " + e.getMessage(), listener);
            }
        }
    }

    private void showLoginDialog() {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.in_down);
        progressBar.setVisibility(View.GONE);
        viewsContainer.setVisibility(View.VISIBLE);
        viewsContainer.startAnimation(anim);
    }

    private void handleUser() {
        mPrefs.putServerUrl(serverSpinner.getSelectedItem().toString());
        mPrefs.putUserName(usernameEditText.getText().toString());
        launchMainActivity();
    }

    public void launchMainActivity() {
        startActivity(new Intent(LoginActivity.this,
                ((Dhis2Application) getApplication()).getMainActivity()));
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
        System.exit(0);
        super.onBackPressed();
    }

    /*
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

        Log.e(CLASS_TAG, "Server URL selected is: " + (String) parent.getItemAtPosition(pos));


    }   */


     /*
    @Override
    protected void onStop() {
        super.onStop();
    } */

    //@Arthur : Nested Class for downlading latest Updates of APK files
    public class DownloadTask extends AsyncTask<String,Integer,String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private final String CLASS_TAG  = "DownloadTask";
        public Date sdate,newdevicedate;
        private boolean flagUpToDate;

        public DownloadTask(Context context) {
            this.context = context;
        }

        public boolean isFlagUpToDate() {
            return flagUpToDate;
        }

        public void setFlagUpToDate(boolean flagUpToDate) {
            this.flagUpToDate = flagUpToDate;
        }

        @Override
        protected String doInBackground(String... sUrl) {

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                //Code to check the version release date for APK currently on the device and for the APK currently on server
                long servertime = connection.getLastModified();
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
                ZipFile zf = new ZipFile(ai.sourceDir);
                ZipEntry ze = zf.getEntry("classes.dex");
                long installed = ze.getTime();
                //long installed = context.getPackageManager().getPackageInfo("org.hisp.dhis.android.trackercapture",0).firstInstallTime;
                newdevicedate = new Date(installed);
                sdate = new Date(servertime);
                Log.v("SERVER_VERSION_DATE", "Last Version Build Date for APK on SERVER is: " + sdate);
                Log.v("DEVICE_VER_DATE", "Version Build Date of APK currently installed on the device is : " + newdevicedate);
                //Compare APK version dates
                if( newdevicedate.after(sdate) || (newdevicedate.equals(sdate))) {
                    Log.v("APK_UP_TO_DATE", "The APK on the Device is already up to date!!!!");
                    //release memory on stack if there are issues - web resources - otherwise remove the next 3 lines
                    flagUpToDate = true;
                   setFlagUpToDate(true);
                    Log.v("FLAG_IN_DTASK", "The flag in the download class is: " + flagUpToDate);
                    // Toast.makeText(context, "Device is already upto date!!!", Toast.LENGTH_SHORT).show();
                    //force killing the asynctask thread
                    this.cancel(true);
                    input.close();
                    output.close();
                    connection.disconnect();


                    //this.cancel(true);
                    return null;
                }
                //zf.close();
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                //Delete the APK file if present in the device from previous installation, delete before downloading
                File apkfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/app-release.apk");
                if (!apkfile.exists()) {

                    apkfile.delete();
                    Log.v("APK_DELETED", "APK  has been deleted from the device after installation");
                }

                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                    output = new FileOutputStream((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/app-release.apk"));
                Log.v("FILE_ONDEVICE", "APK now on Android device");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1 &&(this.isCancelled()==false)) {
                    // allow canceling with back button
                    if (isCancelled()||flagUpToDate==true) {
                        input.close();

                        // return null;
                        break;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            }catch(Exception e){
                return e.toString();
            }finally{

                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();

                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }

                if (connection != null)
                    connection.disconnect();
            }
            Log.v("DOWNLOAD_SUCCESSFULL", "APK has been successfully Downloaded onto device");
            //install the APK on device after the download process
            performAPKInstall("app-release.apk");
            Log.v("INSTALL_SUCCESSFULL", "APK has been successfully installed onto device by the user");

            return null;
        }


        //@Arthur: Overrides of AsyncTask Methods
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            //onPreExecute(), invoked on the UI thread before the task is executed.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();
            // mProgressDialog.show();

            Log.e(CLASS_TAG, "POWER-ON button pressed to keep the device ON!");
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            super.onProgressUpdate(progress);
            mProgressDialog.show();
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("....Downloading NEW APP RELEASE.Please wait....");
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
            Log.v("APK_DOWNLOAD", "Latest APK is currently being downloaded from HISP server!");

        }

        @Override
        protected void onPostExecute(String result) {
            // invoked on the UI thread after the background computation finishes. The result of the background computation is passed to this step as a parameter.
            mWakeLock.release();
            mProgressDialog.dismiss();

            if (result != null) {
                //Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                result=null;
                Log.e("DOWNLOAD ERROR","Possible download error " + result);
            } else if (result == null) {
                Log.v("APK_UPDATED","The APK on device is already up to date");
            } else {
                Toast.makeText(context, "File download Complete!!!", Toast.LENGTH_SHORT).show();
                Log.e(CLASS_TAG, "onPostExecute method invoked on the UI thread");

                Toast.makeText(context, "App has been successfully upgraded!!!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mProgressDialog.dismiss();
            flagUpToDate=true;
             //  An Error has occured due to some unknown problem
            Log.v("ASYNC_TASK_CANCELLED","The download task has been interrupted and aborted inside background method");
            Toast toast = Toast.makeText(LoginActivity.this,
                   "This application is currently up to date", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 25, 180);
            toast.show();
        }
            /*
        @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
        void startMyTask(AsyncTask downloadTask) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                downloadTask.executeOnExecutor(Executors.newSingleThreadExecutor());
            else
                downloadTask.execute();
        }
            */

    }//end DownloadTask class





} //End LoginActivity class
