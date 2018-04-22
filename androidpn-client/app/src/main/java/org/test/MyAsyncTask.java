package org.test;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 * Created by pcyfox on 2018/1/5.
 */

public class MyAsyncTask extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TestAsyncTask task=new TestAsyncTask();
        task.execute("123");

    }

    private  class TestAsyncTask extends AsyncTask<String ,String,String>{


        @Override
        protected String doInBackground(String... strings) {
            return null;
        }
    }


}
