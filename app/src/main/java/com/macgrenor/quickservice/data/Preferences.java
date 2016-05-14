package com.macgrenor.quickservice.data;

import java.util.UUID;

import com.macgrenor.quickservice.QSApplication;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

	public String TAG;

	public boolean INITED;
	
	public String URL;
	public static String URL_URL = "http://quickservice.macgrenor.com/url.php";
	public static String FEEDBACK_URL = "http://quickservice.macgrenor.com/feedback.php";
	//private static String FIRST_URL = "https://script.google.com/macros/s/AKfycbxUQN8Yfn5NCsmdmKc6qPuXlchHUMUmqRUaMosXAh37D663GHmL/exec";
	private static String FIRST_URL = "https://quickservice-1275.appspot.com/qs";
	public static int FETCH_LIMIT = 30;

	public String GEN_UNIQUE_ID;
	
	public int lastUsageSync; //these are Ids
	public int lastSubUsageSync; //these are Ids
	public long lastServiceSync; //these are TimeStamps
	public int lastServerVersion;
	private Context context;

	public Preferences(Context context) {
		super();
		this.context = context;
	}

	public final String PREF_NAME = "QS_PREF";
	/**
	 * Load the value of the settings / preference variable.
	 *
	 * @return void
	 */
	public void loadPreferences() {
		
		final SharedPreferences settings = context.getSharedPreferences(
				PREF_NAME, 0);
		String UniqueId = null;
		int _lastServerVersion = 0;
		if (!settings.contains("GEN_UNIQUE_ID")) {
			UniqueId = UUID.randomUUID().toString();
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("GEN_UNIQUE_ID", UniqueId);
			editor.commit();
		}
		if (!settings.contains("lastServerVersion")) {
			_lastServerVersion = QSApplication.getAppVersion();
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("lastServerVersion", _lastServerVersion);
			editor.commit();
		}
		
		GEN_UNIQUE_ID = settings.getString("GEN_UNIQUE_ID", UniqueId);
		TAG = settings.getString("TAG", "NG");
		URL = settings.getString("URL", FIRST_URL);
		lastUsageSync = settings.getInt("lastUsageSync", 0);
		lastSubUsageSync = settings.getInt("lastSubUsageSync", 0);
		lastServiceSync = settings.getLong("lastServiceSync", 0);
		lastServerVersion = settings.getInt("lastServerVersion", _lastServerVersion);
		INITED = settings.getBoolean("INITED", false);
	}

	private SharedPreferences.Editor getEditor() {
		return context.getSharedPreferences(
				PREF_NAME, 0).edit();
	}

	public void setInited(boolean value) {

		getEditor().putBoolean("INITED", value).commit();
		INITED = value;
	}

	public void changeTag(String tag) {
		if (TAG.equalsIgnoreCase(tag)) return;
		getEditor().putString("TAG", tag).commit();
		getEditor().putInt("lastServiceSync", 0).commit();
		TAG = tag;
	}

	public void setURL(String url) {
		if (URL.equalsIgnoreCase(url)) return;
		getEditor().putString("URL", url).commit();
		URL = url;
	}
	
	public void setLastServiceSync(long stamp) {
		if (stamp < lastServiceSync) return;
		getEditor().putLong("lastServiceSync", stamp).commit();
		lastServiceSync = stamp;
	}
	
	public void setLastUsageSync(int id) {
		if (id < lastUsageSync) return;
		getEditor().putInt("lastUsageSync", id).commit();
		lastUsageSync = id;
	}
	
	public void setLastSubUsageSync(int id) {
		if (id < lastSubUsageSync) return;
		getEditor().putInt("lastSubUsageSync", id).commit();
		lastSubUsageSync = id;
	}
	
	public void setLastServerVersion(int version) {
		if (version < QSApplication.getAppVersion()) return;
		getEditor().putInt("lastServerVersion", version).commit();
		lastServerVersion = version;
	}
	
}
