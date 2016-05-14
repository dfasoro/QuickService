package com.macgrenor.quickservice.service;

import java.util.ArrayList;

import android.net.Uri;
import android.text.TextUtils;

public class Functions {
	public static String urlencode(String input) {
		return Uri.encode(input);
	}
	public static String urldecode(String input) {
		return Uri.decode(input);
	}
	public static String htmlspecialchars(String input) {
		return TextUtils.htmlEncode(input);
	}
	
	public static String ussd_encode(String input) {

	    String output = "";

	    for(char c : input.toCharArray()) {

	        if(c == '#')
	        	output += Uri.encode("#");
	        else if (!(c == '(' || c == ')' || c == '-' || c == ' ')) 
	        	output += c;
	    }

	    return output;
	}
	
	public static String filterText(String input, String function) {
		if (function == "urlencode") return urlencode(input);
		else if (function == "urldecode") return urldecode(input);
		else if (function == "ussd_encode") return ussd_encode(input);
		else if (function == "htmlspecialchars") return htmlspecialchars(input);
		else return (input);
	}
	
	public static String replaceText(String input, ArrayList<ArrayList<String>> replace_filter) {
		for (ArrayList<String> filter : replace_filter) {
			input = input.replace(filter.get(0), filter.get(1));
		}
		return input;
	}
	
	public static String filterFunctions(String input, ArrayList<String> replace_functions) {
		for (String function : replace_functions) {
			input = filterText(input, function);
		}
		return input;
	}
}
