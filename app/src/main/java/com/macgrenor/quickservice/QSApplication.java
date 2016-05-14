package com.macgrenor.quickservice;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.macgrenor.quickservice.data.DataProvider;
import com.macgrenor.quickservice.data.Preferences;
import com.macgrenor.quickservice.data.ServiceDB;
import com.macgrenor.quickservice.network.UpAndDan;

public class QSApplication extends Application {
	public static final String TAG = "QSApplication";

    public static Application app = null;
    public static ServiceDB ServiceDBInstance = null;
    public static Preferences Preferences = null;
    public static UpAndDan UpAndDanThread;

    @Override
    public void onCreate() {
        super.onCreate();
        // Open database connection when the application starts.
        app = this;
        DataProvider.initDataProvider(this.getApplicationContext());
        
        Preferences = new Preferences(this.getApplicationContext());
        Preferences.loadPreferences();
        
        ServiceDBInstance = new ServiceDB();
        
        UpAndDanThread = new UpAndDan();
        UpAndDanThread.start();
    }

    @Override
    public void onTerminate() {
        // Close the database when the application terminates.
    	try { UpAndDanThread.forceShutdown(); } catch (Exception e) { }
    	try { DataProvider.getDataProvider().close(); } catch (Exception e) { }
        super.onTerminate();
    }
    
    public static int getAppVersion() {
    	PackageManager manager = app.getPackageManager();
    	PackageInfo info = null;
    	try { info = manager.getPackageInfo(app.getPackageName(), 0); } catch (Exception e) { }
    	return info.versionCode;
    }
    
    public static long getAppInstallOrUpgradeTime() {
    	PackageManager manager = app.getPackageManager();
    	PackageInfo info = null;
    	try { info = manager.getPackageInfo(app.getPackageName(), 0); } catch (Exception e) { }
    	return (long) Math.floor(info.lastUpdateTime / 1000l);
    }
    
    public static boolean isValidVersion() {
    	return getAppVersion() >= Preferences.lastServerVersion; 
    }

    public static long getUnixTimeStamp() {
        return (long) Math.floor(System.currentTimeMillis() / 1000l);
    }
    
    public static long syncMonthsDifference() {
    	long compareTime = Preferences.lastServiceSync;
    	if (compareTime == 0) compareTime = getAppInstallOrUpgradeTime();
    	long currentTime = QSApplication.getUnixTimeStamp();
    	long divisor = 30l * 24l * 60l * 60l;
    	double diff = (currentTime - compareTime);
    	double _divisor = divisor; 
    	diff = diff / _divisor;
    	return (long) Math.floor(diff);
    }
}
