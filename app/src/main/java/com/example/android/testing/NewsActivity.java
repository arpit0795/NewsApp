package com.example.android.testing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class NewsActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener,  SharedPreferences.OnSharedPreferenceChangeListener {

    private SwipeRefreshLayout mSwipe ;
    private TextView mTv ;
    private NewsAdapter mAdapter;
    private ArrayList<News> mNewsList;
    private ListView mListView ;
    private static final String URL = "https://content.guardianapis.com/search?";  //https://content.guardianapis.com/search?q=science&api-key=test&show-fields=thumbnail&page-size=100&order-by=newest

    //Shared Preferences values
    private String mNewsType;
    private String mOrderBy;
    private String mNewsLimit;

    //Volley
    private RequestQueue mQueue;
    private ProgressBar mPb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        mPb = findViewById(R.id.news_pb);

        mQueue = Volley.newRequestQueue(this);
        mNewsList = new ArrayList<>();
        mAdapter = new NewsAdapter(this, mNewsList);

        mListView = findViewById(R.id.list) ;
        mListView.setAdapter(mAdapter);

        mTv=findViewById(R.id.emptyView);
        mListView.setEmptyView(mTv);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                News currentNews = mAdapter.getItem(position);
                Uri newsUri = Uri.parse(currentNews.getUrl());
                Intent webIntent = new Intent(Intent.ACTION_VIEW, newsUri);
                startActivity(webIntent);
            }
        });

        if(NetworkUtils.isNetworkAvailable(this)){
            mPb.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            jsonParse();
        }
        else{
            MakeDialog();
        }

        mSwipe = findViewById(R.id.swipe_layout) ;
        mSwipe.setOnRefreshListener(this);
    }

    private void MakeDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Internet Connection");
        builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!NetworkUtils.isNetworkAvailable(NewsActivity.this)){
                    mPb.setVisibility(View.VISIBLE);
                    mListView.setVisibility(View.GONE);
                    MakeDialog();
                }else {
                    dialog.dismiss();
                    jsonParse();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void jsonParse() {
        getSharedPreferences();
        String url = URL + "q=" + mNewsType + "&api-key=test&show-fields=thumbnail&page-size=" + mNewsLimit + "&order-by=" + mOrderBy;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {


                        mListView.setVisibility(View.GONE);

                        try {
                            JSONObject  res = response.getJSONObject("response");
                            JSONArray newsArray = res.getJSONArray("results");

                            for (int i = 0; i < newsArray.length(); i++) {

                                JSONObject currentNews = newsArray.getJSONObject(i);

                                String sectionName = currentNews.getString("sectionName");

                                String newsTitle = currentNews.getString("webTitle");
                                String newsPublish = currentNews.getString("webPublicationDate");
                                String newsURL;
                                if(currentNews.has("webUrl")){
                                    newsURL = currentNews.getString("webUrl");
                                }
                                else{
                                    newsURL = null;
                                }

                                String thumbnail;
                                if(currentNews.has("fields")){
                                    JSONObject fields = currentNews.getJSONObject("fields");
                                    thumbnail = fields.getString("thumbnail");
                                }
                                else{
                                    thumbnail = null;
                                }


                                mSwipe.setRefreshing(false);

                                mPb.setVisibility(View.GONE);
                                mListView.setVisibility(View.VISIBLE);
                                mTv.setVisibility(View.GONE);

                                News news = new News("newsName", sectionName, newsTitle, newsURL, thumbnail, newsPublish);
                                mNewsList.add(news);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            mTv.setVisibility(View.VISIBLE);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                mTv.setVisibility(View.VISIBLE);
            }
        });
        mQueue.add(request);
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.e("NewsActivity", "OnResume");
        mNewsList.clear();
        mAdapter.notifyDataSetChanged();

        mPb.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
        jsonParse();
    }

    @Override
    public void onRefresh() {
        Log.d("MAIN", "REFRESHING...");
        mNewsList.clear();
        mAdapter.notifyDataSetChanged();
        jsonParse();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings) {
            startActivity(new Intent(NewsActivity.this, SettingsActivity.class));
        }
        return true;
    }

    private void getSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mNewsType = sharedPreferences.getString(getString(R.string.title_news_type_preference_key), getString(R.string.default_news_type_value));
        mOrderBy = sharedPreferences.getString(getString(R.string.title_order_by_preference_key), getString(R.string.default_order_by_value));
        mNewsLimit = sharedPreferences.getString(getString(R.string.title_news_limit_preference_key), getString(R.string.default_news_limit_value));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        getSharedPreferences();
    }
}