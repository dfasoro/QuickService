package com.macgrenor.quickservice.service;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class ServiceField {
		public String name; // name must be unique in a service but can be shared across services, use service prefix to enforce nosharing.
		public String label; // ... (might be empty for label types)
		public String instruction; //noshow if empty
		public String type; //number/phonenumber/email/select/multiselect/link/label/text/hidden/textarea
		public boolean required; //true / false
		public ArrayList<SelectOptions> selectoptions;
		public boolean live; // use this to rerun some pattern replacements
		public boolean autocomplete; // use this to determine whether to save values and whether to use an autocomplete field
		public boolean defaultLast; //read last value
		public String defaultValue; //use this value, might be null or empty.
		public ArrayList<String> inuse_in; //[ussd, call, text, email, weblinks, info or ALL]
		public ArrayList<ArrayList<String>> replace_filter; // [[search, replace], [search, replace]]
		public ArrayList<String> function_filter; //: [urlencode, ussd_encode] etc.

		public String action;
		public HashMap<String, String> action_payload;
		
		private String currentValue;
		private Service _service;
		
		public ServiceField(JSONObject field, Service service) throws JSONException {
			this.name = field.getString("name");
			this.label = field.getString("label");
			this.instruction = field.getString("instruction"); //small instruction text or placeholder?
			this.type = field.getString("type"); //fallsback to text if unknown
			this.required = field.getBoolean("required");

			this.selectoptions = Service.ArraysToSelectOptions(field.getJSONArray("selectoptions_key"), field.getJSONArray("selectoptions_title"));
			
			this.autocomplete = field.getBoolean("autocomplete");
			this.live = field.optBoolean("live", false);
			this.action = field.getString("action");
			this.action_payload = Service.ObjectToStringMap(field.optJSONObject("action_payload"));

			this.defaultLast = field.getBoolean("defaultLast");
			this.defaultValue = field.getString("defaultValue");
			this.currentValue = this.defaultValue;
			this.inuse_in = Service.ArrayToArrayListOfString(field.getJSONArray("inuse_in"));
			this.replace_filter = Service.ArrayToArrayListOfArrayString(field.getJSONArray("replace_filter"));
			this.function_filter = Service.ArrayToArrayListOfString(field.getJSONArray("function_filter"));
			
			this._service = service;
		}
		
		public void setValue(String value) {
			if (!this.isReadOnly()) currentValue = value;
			this._service.saveFieldValue(this.name, currentValue);
		}
		
		public String getValue() {
			return currentValue == null ? "" : currentValue;
		}
		
		public String getFormattedValue() {
			String value = getValue();
			
			value = Functions.replaceText(value, replace_filter);
			value = Functions.filterFunctions(value, function_filter);
			
			return value;
		}
		
		public boolean isReadOnly() {
			return (INPUT_TYPE.label.equals(this.type) || INPUT_TYPE.hidden.equals(this.type));
		}
	}