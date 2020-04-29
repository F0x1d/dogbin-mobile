package com.f0x1d.dogbin.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.f0x1d.dmsdk.BinService;
import com.f0x1d.dogbin.App;
import com.f0x1d.dogbin.R;
import com.f0x1d.dogbin.network.DogBinService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.BaseDexClassLoader;

public class BinServiceUtils {

    public static final String START_TAG = "dogbin.";

    private static BinService sInstance;
    private static MutableLiveData<List<ApplicationInfo>> sInstalledServicesData = new MutableLiveData<>();

    public static BinService getCurrentActiveService() {
        synchronized (BinServiceUtils.class) {
            if (sInstance == null) {
                List<ApplicationInfo> installedServices = getInstalledServices();

                String selectedService = App.getPrefsUtil().getSelectedService();
                if (selectedService == null)
                    return sInstance = DogBinService.getInstance();

                try {
                    for (ApplicationInfo installedService : installedServices) {
                        if (installedService.packageName.equals(selectedService)) {
                            return sInstance = loadServiceFromApp(installedService.packageName);
                        }
                    }
                } catch (Exception e) {
                    Utils.runOnUiThread(() ->
                            Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.error, e.getLocalizedMessage()), Toast.LENGTH_SHORT).show());
                    App.getPrefsUtil().setSelectedService(null);
                    return sInstance = DogBinService.getInstance();
                }
            }
            return sInstance;
        }
    }

    public static BinService getBinServiceForPackageName(String packageName) {
        synchronized (BinServiceUtils.class) {
            try {
                sInstance = loadServiceFromApp(packageName);
                App.getPrefsUtil().setSelectedService(packageName);
                return sInstance;
            } catch (Exception e) {
                return DogBinService.getInstance();
            }
        }
    }

    private static BinService loadServiceFromApp(String packageName) throws Exception {
        ApplicationInfo applicationInfo = App.getInstance().getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        if (applicationInfo.metaData == null || !App.getInstance().getPackageManager().getApplicationLabel(applicationInfo).toString().startsWith(START_TAG)) {
            App.getPrefsUtil().setSelectedService(null);
            return DogBinService.getInstance();
        }

        File outDir = new File("/data/data/" + App.getInstance().getPackageName() + "/files/" + applicationInfo.packageName);
        if (!outDir.exists())
            outDir.mkdirs();

        BaseDexClassLoader baseDexClassLoader = new BaseDexClassLoader(applicationInfo.sourceDir, outDir, null, App.getInstance().getClassLoader());
        BinService binService = (BinService) baseDexClassLoader.loadClass(applicationInfo.metaData.getString("service")).getConstructor().newInstance();
        binService.init(App.getInstance().createPackageContext(packageName, 0),
                App.getInstance().getSharedPreferences(packageName + "_module", Context.MODE_PRIVATE));
        return binService;
    }

    private static List<ApplicationInfo> getInstalledServices() {
        List<ApplicationInfo> installedPlugins = new ArrayList<>();
        for (ApplicationInfo installedApplication : App.getInstance().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA)) {
            if ((installedApplication.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                continue;

            if (App.getInstance().getPackageManager().getApplicationLabel(installedApplication).toString().startsWith(START_TAG))
                installedPlugins.add(installedApplication);
        }

        sInstalledServicesData.postValue(installedPlugins);

        return installedPlugins;
    }

    public static void refreshInstalledServices() {
        Utils.getExecutor().execute(BinServiceUtils::getInstalledServices);
    }

    public static void refreshCurrentService() {
        sInstance = null;
    }

    public static LiveData<List<ApplicationInfo>> getInstalledServicesData() {
        return sInstalledServicesData;
    }
}
