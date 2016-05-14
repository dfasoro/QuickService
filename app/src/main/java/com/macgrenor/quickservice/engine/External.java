package com.macgrenor.quickservice.engine;

import com.macgrenor.quickservice.service.Functions;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class External {

	public static void dial(Context context, String number) {
		Intent intent = new Intent(Intent.ACTION_DIAL);
	    intent.setData(callableUri(number));
	    context.startActivity(intent);
	}
	
	public static void sendText(Context context, String number, String message) {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		
		intent.setType("text/plain");
	    intent.setData(Uri.parse("smsto:" + number));  // This ensures only SMS apps respond
	    intent.putExtra("sms_body", message);

	    context.startActivity(intent);
	}
	
	public static void sendEmail(Context context, String to, String subject, String body) {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		
		intent.setType("text/plain");
	    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
	    
	    intent.putExtra(Intent.EXTRA_EMAIL, to.split(","));
	    intent.putExtra(Intent.EXTRA_SUBJECT, subject);
	    intent.putExtra(Intent.EXTRA_TEXT, body);
	    
	    context.startActivity(intent);
	}
	
	@SuppressLint("DefaultLocale")
	public static void openBrowser(Context context, String url) {
		if (!url.toLowerCase().startsWith("https://") && !url.toLowerCase().startsWith("http://")){
		    url = "http://" + url;
		}
		Intent openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		context.startActivity(openUrlIntent);
	}
	
	public static void copyText(Context context, String text) {
		ClipboardManager clipboard = (ClipboardManager)
				context.getSystemService(Context.CLIPBOARD_SERVICE);
				
		ClipData clip = ClipData.newPlainText("Info", text);

		clipboard.setPrimaryClip(clip);
		
		Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
	}
	
	private static Uri callableUri(String number) {

	    String uriString = "";

	    if(!number.startsWith("tel:"))
	        uriString += "tel:";
	    uriString += Functions.ussd_encode(number);

	    return Uri.parse(uriString);
	}
	
	
}
