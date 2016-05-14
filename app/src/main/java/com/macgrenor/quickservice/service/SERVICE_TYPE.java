package com.macgrenor.quickservice.service;

import java.util.HashMap;

public class SERVICE_TYPE {
	//once this is added to, then prepareOutput in Service must be edited.
	public static final String ussd = "ussd";
	public static final String call = "call";
	public static final String text = "text";
	public static final String email = "email";
	public static final String weblinks = "weblinks";
	public static final String info = "info";
	public static final String all = "all";

	public static HashMap<String, String> getServiceTypeLabels() {
		HashMap<String, String> ret = new HashMap<String, String>(10);
		ret.put(ussd, "USSD");
		ret.put(call, "Phone Call");
		ret.put(text, "SMS Text");
		ret.put(email, "Email");
		ret.put(weblinks, "Web Links");
		ret.put(info, "Information");
		return ret;
	}
	public static HashMap<String, String> getServiceActionLabels() {
		HashMap<String, String> ret = new HashMap<String, String>(10);
		ret.put(ussd, "Dial");
		ret.put(call, "Dial");
		ret.put(text, "Send");
		ret.put(email, "Send");
		ret.put(weblinks, "Open");
		ret.put(info, "Copy");
		return ret;
	}
}
