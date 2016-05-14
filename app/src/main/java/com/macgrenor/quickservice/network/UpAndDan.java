package com.macgrenor.quickservice.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;

import android.database.Cursor;
import android.text.TextUtils;

import com.macgrenor.quickservice.QSApplication;
import com.macgrenor.quickservice.data.DataAccess;
import com.macgrenor.quickservice.data.Preferences;
import com.macgrenor.quickservice.service.RegisterService;

public class UpAndDan extends Thread {
	private UpAndDanNotifier notifier;
	private HashMap<String, String> defConnVars;
	private boolean restartMode, shutdownMode, userInitiated;
	private long checkedVersionTodayAt = 0;
	private int serviceLoops = 0;
	
	public UpAndDan() {
		super();
		defConnVars = new HashMap<String, String>(10);

		defConnVars.put("TAG", QSApplication.Preferences.TAG);
		defConnVars.put("GEN_UNIQUE_ID", QSApplication.Preferences.GEN_UNIQUE_ID);
		defConnVars.put("build_version", String.valueOf(android.os.Build.VERSION.SDK_INT));
		defConnVars.put("build_manufacturer", android.os.Build.MANUFACTURER);
		defConnVars.put("build_productname", android.os.Build.PRODUCT);
		defConnVars.put("build_brand", android.os.Build.BRAND);
		defConnVars.put("build_model", android.os.Build.MODEL);
		defConnVars.put("app_version", String.valueOf(QSApplication.getAppVersion()));
	}
	
	public void setNotifier(UpAndDanNotifier notifier) {
		this.notifier = notifier;
	}
	
	public UpAndDanNotifier getNotifier() {
		return notifier;
	}
	
	private void notifyInited() {
		try { notifier.initReady(); } catch (Exception e) { }
	}

	private void notifyNewServices(int count) {
		try { notifier.newServices(count); } catch (Exception e) { }
	}

	private void notifyUpdateServices(int count) {
		try { notifier.updatedServices(count); } catch (Exception e) { }
	}
	
	@Override
	public void run() {

		//read DB for unprocessed threads ...
		synchronized(this) {
			try {
				wait(1 * 1000); //wait 5 secs
			} catch (InterruptedException e) { }
		}
		
		if (shutdownMode) return;
		
		if (!QSApplication.Preferences.INITED) {
			try {
				RegisterService.wipeAll();
				long maxTimeStamp = RegisterService.initServicesFromResource();
				QSApplication.Preferences.setLastServiceSync(maxTimeStamp);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

			QSApplication.Preferences.setInited(true);
			notifyInited();
		}

		while (true) {
			restartMode = false;
			if (shutdownMode) return;


			if (serviceLoops == 0) {
				try {
					URL whatismyurl = new URL(Preferences.URL_URL);
					//HttpURLConnection xx = (HttpURLConnection) whatismyurl.openConnection();
					//xx.
					BufferedReader in = new BufferedReader(new InputStreamReader(
							whatismyurl.openStream()));

					String new_url = in.readLine(); //you get the IP as a String
					if (new_url != null && (new_url.startsWith("http://") || new_url.startsWith("https://"))) {
						QSApplication.Preferences.setURL(new_url);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			long currentUnixTime = QSApplication.getUnixTimeStamp();
			if ( (currentUnixTime - checkedVersionTodayAt) > (2 * 60 * 60) ) { //do every 2 hours and also log other data.
				ClientHttpRequest conn = null;
				try {
					conn = new ClientHttpRequest(QSApplication.Preferences.URL, "POST", 15 * 1000);
					conn.setParameters(defConnVars);

					conn.setParameter("action", "check_version");
					
					String resp = conn.postAndRetrieve(); //Push to Server
					if (!TextUtils.isEmpty(resp)) resp = resp.trim();
					if (!TextUtils.isEmpty(resp)) {
						String[] respSplit = TextUtils.split(resp, "\n");
						if ("ok".equalsIgnoreCase(respSplit[0].trim()) || "notok".equalsIgnoreCase(respSplit[0].trim())) {
							checkedVersionTodayAt = currentUnixTime;

							if ("notok".equalsIgnoreCase(respSplit[0].trim())) {
								int serverVersion = Integer.valueOf(respSplit[1].trim());
								QSApplication.Preferences.setLastServerVersion(serverVersion);
							}
						}
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				finally {
					if (conn != null) conn.closeAll();
				}
			}
			
			if (shutdownMode) return;
			
			
			
			defConnVars.put("TAG", QSApplication.Preferences.TAG);
			
			int newDownloadedServicesAll = 0, lastDownloadedServicesAll = 0, lastDownloadedServices = 0;
			boolean connError = false;
			
			//dan
			if (serviceLoops == 0) {
				while (true) {
					if (shutdownMode) return;

					ClientHttpRequest conn = null;
					lastDownloadedServices = 0;
					try {

						conn = new ClientHttpRequest(QSApplication.Preferences.URL, "POST", 15 * 1000);
						conn.setParameters(defConnVars);

						conn.setParameter("lastServiceSync", String.valueOf(QSApplication.Preferences.lastServiceSync));
						conn.setParameter("FETCH_LIMIT", String.valueOf(Preferences.FETCH_LIMIT));
						conn.setParameter("action", "fetch_services");

						String resp = conn.postAndRetrieve(); //Push to Server

						if (!QSApplication.Preferences.TAG.equals(defConnVars.get("TAG"))) continue;
						if (!TextUtils.isEmpty(resp)) {
							long maxTimeStamp = RegisterService.initServicesFromServer(resp);
							QSApplication.Preferences.setLastServiceSync(maxTimeStamp);
							lastDownloadedServices = RegisterService.lastServiceCount;
							newDownloadedServicesAll += RegisterService.lastNewServiceCount;
							lastDownloadedServicesAll += RegisterService.lastServiceCount;
							resp = null;

						} else {
							connError = true;
						}

					} catch (NumberFormatException e1) {
						e1.printStackTrace();
					} catch (JSONException e1) {
						e1.printStackTrace();
					} catch (Exception e1) {
						connError = true;
						e1.printStackTrace();
					} finally {
						if (conn != null) conn.closeAll();
					}


					if (lastDownloadedServices < Preferences.FETCH_LIMIT) {
						//don fetch wetin e fit fetch
						break;
					} else {
						// we got exactly fetch_limit, let's do this again.
					}
				}

				if (newDownloadedServicesAll > 0) notifyNewServices(newDownloadedServicesAll);
				else if (userInitiated || lastDownloadedServicesAll > 0) notifyUpdateServices(lastDownloadedServicesAll);

				userInitiated = false;
			}
			

			if (shutdownMode) return;
			if (restartMode) continue;


			if (!connError) serviceLoops++;
			if (serviceLoops == 4) serviceLoops = 0;

			//up 
			//if unsynced service usage/subusage. sync, update last-id.
			StringBuilder usageBody = new StringBuilder(15000);
			int maxUsageId = 0;
			int maxSubUsageId = 0;
			
			{
				Cursor mainUsage = DataAccess.getUnsyncedUsage();
				
				while (mainUsage.moveToNext()) {
					if (usageBody.length() != 0) usageBody.append("\n");
					usageBody.append("usage,");
					maxUsageId = Math.max(maxUsageId, mainUsage.getInt(mainUsage.getColumnIndexOrThrow("id")));
	
					usageBody.append(mainUsage.getInt(mainUsage.getColumnIndexOrThrow("id")) + ",");
					usageBody.append(mainUsage.getInt(mainUsage.getColumnIndexOrThrow("service_id")) + ",");
					usageBody.append(mainUsage.getString(mainUsage.getColumnIndexOrThrow("channel")) + ",");
					usageBody.append(mainUsage.getLong(mainUsage.getColumnIndexOrThrow("date_used")) + ",");
					usageBody.append(mainUsage.getInt(mainUsage.getColumnIndexOrThrow("service_version")) + ",");
					usageBody.append(mainUsage.getInt(mainUsage.getColumnIndexOrThrow("bookmarked")));
				}
				mainUsage.close();
			}

			if (shutdownMode) return;
			if (restartMode) continue;

			{
				Cursor subUsage = DataAccess.getUnsyncedSubUsage();
				
				while (subUsage.moveToNext()) {
					if (usageBody.length() != 0) usageBody.append("\n");
					usageBody.append("subusage,");
					maxSubUsageId = Math.max(maxSubUsageId, subUsage.getInt(subUsage.getColumnIndexOrThrow("id")));
	
					usageBody.append(subUsage.getInt(subUsage.getColumnIndexOrThrow("id")) + ",");
					usageBody.append(subUsage.getInt(subUsage.getColumnIndexOrThrow("service_id")) + ",");
					usageBody.append(subUsage.getString(subUsage.getColumnIndexOrThrow("channel")) + ",");
					usageBody.append(subUsage.getLong(subUsage.getColumnIndexOrThrow("date_used")) + ",");
					usageBody.append(subUsage.getString(subUsage.getColumnIndexOrThrow("service_sub_id")) + ",");
					usageBody.append(subUsage.getInt(subUsage.getColumnIndexOrThrow("bookmarked")));
				}
				subUsage.close();
			}
			
			if (shutdownMode) return;
			if (restartMode) continue;
			
			if (usageBody.length() > 0){
				ClientHttpRequest conn = null;
				try {
					conn = new ClientHttpRequest(QSApplication.Preferences.URL, "POST", 15 * 1000);
					conn.setParameters(defConnVars);

					conn.setParameter("action", "sync_services");
					conn.setParameter("data", usageBody.toString());
					
					String resp = conn.postAndRetrieve(); //Push to Server
					
					if ("ok".equalsIgnoreCase(resp)) {
						QSApplication.Preferences.setLastUsageSync(maxUsageId);
						QSApplication.Preferences.setLastSubUsageSync(maxSubUsageId);
					}
					else {
						connError = true;
					}
					
				} catch (Exception e1) {
					connError = true;
					e1.printStackTrace();
				}
				finally {
					if (conn != null) conn.closeAll();
				}
			}
			
			if (shutdownMode) return;
			if (restartMode) continue;
			
			synchronized(this) {
				try {
					if (connError) {
						wait(15 * 60 * 1000); //retry after 15 minutes on connection failure.
					}
					else {
						wait(1 * 60 * 60 * 1000); //retry after 1 hours if no connection failure.
					}
				} catch (InterruptedException e) { }
			}
		}
	}

	
	public synchronized void forceReStart() {
		checkedVersionTodayAt = 0;
		serviceLoops = 0;
		restartMode = true;
		userInitiated = true;
		notify();
	}
	
	public synchronized void forceShutdown() {
		shutdownMode = true;
		notify();
	}
	
	public static String convertMilliSecondsToDate(long milliSeconds)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+1")); 
		
		return sdf.format(new Date(milliSeconds));
	}
 
}
