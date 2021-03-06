package com.f0x1d.dogbin.network.retrofit.foxbin;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class FoxBinApi {

    private static FoxBinApi sInstance;
    private FoxBinApiService mService;

    public static FoxBinApi getInstance() {
        synchronized (FoxBinApi.class) {
            return sInstance == null ? sInstance = new FoxBinApi() : sInstance;
        }
    }

    public FoxBinApiService getService() {
        if (mService == null)
            setupService();

        return mService;
    }

    private void setupService() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://f0x1d.com/foxbin/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        mService = retrofit.create(FoxBinApiService.class);
    }
}
