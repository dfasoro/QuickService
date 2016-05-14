package com.macgrenor.quickservice.service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.*;

import com.macgrenor.quickservice.QSApplication;
import com.macgrenor.quickservice.data.DataAccess;
import com.macgrenor.quickservice.engine.External;

import android.content.Context;
import android.text.TextUtils;

public class Service {
	private int id;
	private ServiceData sdata;
	private int usage_id;

	private HashMap<String, String> values;
	
	public String name; //check Glo Call Balance
	public String category; //Health/Edu/Unsubscribe/Banking/Telco/Balance/
	public String subcategory; // Gtb/Stanbic/Empty/etc...
	public String tags; //string delimited linda, konga, orisirisi
	
	public ArrayList<SelectOptions> types; //ussd => USSD, call, text, email, weblinks
	public String defType; //ussd

	public HashMap<String, String> pre_instruction; //if empty don't show
	public HashMap<String, String> post_instruction; //if empty show at end while prompting for a draft/replay.
	//public String reminder; //if not empty show after post instruction, show if there is a reminder, or else set one.
	public int service_version;
	
	private HashMap<String, String> formats;
	
	private ArrayList<ArrayList<ServiceField>> forms;
	private boolean readonly;
	
	public Service(int id) throws Exception {
		this(id, 0);
	}
	
	public Service(int id, int usage_id) throws Exception {
		this.id = id;
		this.sdata = new ServiceData(id);
		
		JSONObject config = new JSONObject(this.sdata.markup);
		
		this.name = config.getString("name");
		this.category = config.getString("category");
		this.subcategory = config.getString("subcategory");
		this.tags = config.getString("tags");
		this.service_version = config.getInt("service_version");
		
		ArrayList<String> type_keys = ArrayToArrayListOfString(config.getJSONArray("types"));
		ArrayList<String> type_titles = new ArrayList<String>(type_keys.size());
		
		HashMap<String, String> type_labels = SERVICE_TYPE.getServiceTypeLabels();
		
		for (int i = 0; i < type_keys.size(); i++) {
			type_titles.add(type_labels.get(type_keys.get(i)));
		}
		
		this.types = Service.ArraysToSelectOptions(type_keys, type_titles);
		
		this.defType = config.getString("defType");
		
		this.pre_instruction =  ObjectToStringMap(config.getJSONObject("pre_instruction"));
		this.post_instruction =  ObjectToStringMap(config.getJSONObject("post_instruction"));
		//this.reminder = config.getString("reminder");
		
		this.formats = ObjectToStringMap(config.getJSONObject("formats"));
		
		parseFormDescriptor(config.getJSONArray("forms"));

		this.values = new HashMap<String, String>(10);
		
		loadData(usage_id);
	}
	
	private void loadData(int usage_id) throws JSONException {
		
		if (usage_id != 0) {
			ServiceUsage current_usage = new ServiceUsage(usage_id);
			this.values = current_usage.data;
			this.defType = current_usage.channel;
			this.readonly = true;
			this.usage_id = usage_id;
			
			if (this.service_version != current_usage.service_version ||
					(QSApplication.getUnixTimeStamp() - current_usage.date_used > 600)) {
				this.removeReadonly();
			}
		}
		else {
			ServiceUsage sf = QSApplication.ServiceDBInstance.getServiceDraft(this.id);
			this.readonly = false;
			
			if (sf != null) {
				this.values = sf.data;
				this.defType = sf.channel;
				this.usage_id = 0;
			}
			else if (this.sdata.last_use_id != 0) {
				ServiceUsage last_usage = new ServiceUsage(this.sdata.last_use_id);
				
				for (int i = 0; i < this.forms.size(); i++) {
					ArrayList<ServiceField> form = this.forms.get(i);
					for (int j = 0; j < form.size(); j++) {
						ServiceField field = form.get(j);
						if (field.defaultLast && last_usage.data.containsKey(field.name)) {
							this.values.put(field.name, last_usage.data.get(field.name));
						}
					}
				}
				
				this.defType = last_usage.channel;
			}
		}
		
		
		for (int i = 0; i < this.forms.size(); i++) {
			ArrayList<ServiceField> form = this.forms.get(i);
			for (int j = 0; j < form.size(); j++) {
				ServiceField field = form.get(j);
				if (this.values.containsKey(field.name)) {
					field.setValue(this.values.get(field.name));
				}
			}
		}
		
	}
	
	public void removeReadonly() {
		if (this.readonly) {
			this.readonly = false;
			this.usage_id = 0;
		}
	}
	
	public boolean isReadonly() {
		return this.readonly;
	}
	
	public int _nextForm;
	public int _prevForm;
	public int _currentForm;
	
	public String _currentChannel;
	private ArrayList<ServiceField> _currentFormFields;
	
	public ArrayList<ServiceField> prepareForm(int form_id, ArrayList<String> form_values) {
		//form_id is 1 indexed.
		
		if (_currentForm != 0) _saveForm(form_values, false);
		
		_currentFormFields = new ArrayList<ServiceField>(5);
		if (_currentChannel == null) _currentChannel = defType;
		int _form_index = form_id - 1;
		
		_prevForm = 0;
		for (int i = _form_index - 1; i >= 0; i--) {
			ArrayList<ServiceField> formFieldsSet = this.forms.get(i);
			
			for (int j = 0; j < formFieldsSet.size(); j++) {
				ServiceField field = formFieldsSet.get(j);
				if ((field.inuse_in.contains(SERVICE_TYPE.all) || field.inuse_in.contains(_currentChannel)) 
						&& !INPUT_TYPE.hidden.equalsIgnoreCase(field.type) ) {
					_prevForm = i + 1;
					break;
				}
			}
			if (_prevForm != 0) break;
		}
		
		_nextForm = 0;
		for (int i = _form_index + 1; i < this.forms.size(); i++) {
			ArrayList<ServiceField> formFieldsSet = this.forms.get(i);
			
			for (int j = 0; j < formFieldsSet.size(); j++) {
				ServiceField field = formFieldsSet.get(j);
				if ((field.inuse_in.contains(SERVICE_TYPE.all) || field.inuse_in.contains(_currentChannel)) 
						&& !INPUT_TYPE.hidden.equalsIgnoreCase(field.type)) {
					_nextForm = i + 1;
					break;
				}
			}
			if (_nextForm != 0) break;
		}
		
		ArrayList<ServiceField> formFieldsSet = this.forms.get(_form_index);
		
		for (int j = 0; j < formFieldsSet.size(); j++) {
			ServiceField field = formFieldsSet.get(j);
			if ((field.inuse_in.contains(SERVICE_TYPE.all) || field.inuse_in.contains(_currentChannel))) {				
				_currentFormFields.add(field);
			}
		}
		
		_currentForm = form_id;

		return _currentFormFields;
	}
	
	public HashMap<String, String> saveForm(ArrayList<String> form_values) {
		return _saveForm(form_values, false);
	}
	
	private HashMap<String, String> _saveForm(ArrayList<String> form_values, boolean completed) {
		if (_currentFormFields == null) return null;
		
		ArrayList<HashMap<String, String>> valueMap = getExtractedValues(form_values, completed, null);
		HashMap<String, String> valuesToSave = valueMap.get(0);
		HashMap<String, String> valuesToFormat = valueMap.get(1);
		
		if (!readonly) {
			this.usage_id = DataAccess.saveUsage(usage_id, id, _currentChannel, completed, service_version, valuesToSave, this.sdata.use_count + 1);
			if (completed) {
				this.readonly = true;
				this.sdata.use_count++;
			}
		}
		
		return valuesToFormat;
	}
	
	private ArrayList<HashMap<String, String>> getExtractedValues(ArrayList<String> form_values, boolean completed, ArrayList<String> subActionValues) {
		HashMap<String, String> valuestoSave = new HashMap<String, String>(10);
		HashMap<String, String> valuestoFormat = new HashMap<String, String>(10);
		ArrayList<HashMap<String, String>> returnVal = new ArrayList<HashMap<String, String>>(2);
		
		if (!readonly) {
			for (int i = 0; i < _currentFormFields.size(); i++) {
				//call on form objects to submit their values. and call setvalue on field.
				ServiceField field = _currentFormFields.get(i);
				field.setValue(form_values.get(i));
			}
		}
		
		//save autocomplete
		for (int i = 0; i < this.forms.size(); i++) {
			ArrayList<ServiceField> form = this.forms.get(i);
			for (int j = 0; j < form.size(); j++) {
				ServiceField field = form.get(j);
				if (!(field.inuse_in.contains(SERVICE_TYPE.all) || field.inuse_in.contains(_currentChannel))) continue;
				
				if (!field.isReadOnly()) {
					if (field.autocomplete && (completed || (subActionValues != null && subActionValues.contains(field.name))) &&
							(INPUT_TYPE.number.equalsIgnoreCase(field.type) || INPUT_TYPE.phonenumber.equalsIgnoreCase(field.type) ||
									INPUT_TYPE.email.equalsIgnoreCase(field.type) || INPUT_TYPE.text.equalsIgnoreCase(field.type))) {
						DataAccess.saveAutoComplete(field.name, field.getValue());
					}
	
					valuestoSave.put(field.name, field.getValue());
				}
				
				valuestoFormat.put(field.name, field.getFormattedValue());
			}
		}

		for (int i = 0; i < this.forms.size(); i++) {
			ArrayList<ServiceField> form = this.forms.get(i);
			for (int j = 0; j < form.size(); j++) {
				ServiceField field = form.get(j);
				if (!(field.inuse_in.contains(SERVICE_TYPE.all) || field.inuse_in.contains(_currentChannel))) continue;

				if (field.live) {
					HashMap<String, String> _formats = new HashMap<String, String>(1);
					_formats.put("live", valuestoFormat.get(field.name));
					valuestoFormat.put(field.name, parseVariables("live", valuestoFormat, _formats));
				}
			}
		}

		returnVal.add(valuestoSave);
		returnVal.add(valuestoFormat);
		
		return returnVal;
	}
	
	//return errors
	public ArrayList<String> _validateForm(ArrayList<String> form_values) {
		if (_currentFormFields == null) return null;
		_saveForm(form_values, false);
		
		ArrayList<String> errors = new ArrayList<String>(2);
		
		if (readonly) return errors;
		
		for (int i = 0; i < _currentFormFields.size(); i++) {
			//call on form objects to submit their values. and call setvalue on field.
			ServiceField field = _currentFormFields.get(i);
			if (field.isReadOnly() || !field.required) continue;
			
			if (form_values.get(i).trim().length() == 0) {
				errors.add((field.label != "" ? field.label : field.name) + " is required");
			}
		}
		
		return errors;
	}
	
	public void sendOutput(Context c, ArrayList<String> values) {
		HashMap<String, String> valuestoFormat = _saveForm(values, true);
		sendOutput(c, this._currentChannel, valuestoFormat, this.formats);
	}
	
	private HashMap<String, Boolean> _subActionDone = new HashMap<String, Boolean>(10);
	public void sendSubActionOutput(Context c, String field_name, ArrayList<String> values, String channel, HashMap<String, String> _formats) throws Exception {
		ArrayList<String> keywords = getKeywords(_formats);
		ArrayList<HashMap<String, String>> valueMap = getExtractedValues(values, false, keywords);
		HashMap<String, String> valuesToSave = valueMap.get(0);
		HashMap<String, String> valuesToSaveSubAction = new HashMap<String, String>(5);
		HashMap<String, String> valuesToFormat = valueMap.get(1);

		for (Map.Entry<String, String> value : valuesToSave.entrySet()) {
			if (keywords.contains(value.getKey())) valuesToSaveSubAction.put(value.getKey(), value.getValue());
		}

		_validateSubForm(valuesToSaveSubAction);

		sendOutput(c, channel, valuesToFormat, _formats);


		DataAccess.saveSubUsage(id, channel, field_name, _subActionDone.containsKey(field_name), valuesToSaveSubAction);
		_subActionDone.put(field_name, true);
	}

	private ArrayList<String> getKeywords(HashMap<String, String> _formats) {
		ArrayList<String> keywords = new ArrayList<String>(5);
		String allApplicableFormats = TextUtils.join("\n", _formats.values());
		String patternString = "\\[([a-zA-Z0-9_]+)\\]";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(allApplicableFormats);

		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			keywords.add(matcher.group(1));
		}

		return keywords;
	}


	private void _validateSubForm(HashMap<String, String> form_values) throws Exception {
		ArrayList<String> errors = new ArrayList<String>(2);

		for (int i = 0; i < _currentFormFields.size(); i++) {
			ServiceField field = _currentFormFields.get(i);
			if (field.isReadOnly() || !field.required) continue;

			if (form_values.containsKey(field.name) && form_values.get(field.name).trim().length() == 0) {
				throw new Exception ((field.label != "" ? field.label : field.name) + " is required");
			}
		}

	}

	public void sendOutput(Context c, String channel, HashMap<String, String> valuestoFormat, HashMap<String, String> _formats) {
		if (SERVICE_TYPE.ussd.equals(channel)) {
			External.dial(c, parseVariables(FORMAT_TYPE.ussd, valuestoFormat, _formats));
		}
		else if (SERVICE_TYPE.call.equals(channel)) {
			External.dial(c, parseVariables(FORMAT_TYPE.call, valuestoFormat, _formats));
		}
		else if (SERVICE_TYPE.text.equals(channel)) {
			External.sendText(c, parseVariables(FORMAT_TYPE.text_to, valuestoFormat, _formats), parseVariables(FORMAT_TYPE.text, valuestoFormat, _formats));
		}
		else if (SERVICE_TYPE.email.equals(channel)) {
			External.sendEmail(c, parseVariables(FORMAT_TYPE.email_to, valuestoFormat, _formats), parseVariables(FORMAT_TYPE.email_subject, valuestoFormat, _formats), 
					parseVariables(FORMAT_TYPE.email, valuestoFormat, _formats));
		}
		else if (SERVICE_TYPE.weblinks.equals(channel)) {
			External.openBrowser(c, parseVariables(FORMAT_TYPE.weblinks, valuestoFormat, _formats));
		}
		else if (SERVICE_TYPE.info.equals(channel)) {
			External.copyText(c, parseVariables(FORMAT_TYPE.info, valuestoFormat, _formats));
		}
		
	}
	
	public boolean emptyFormat() {
		String format = null;
		if (SERVICE_TYPE.ussd.equals((this._currentChannel))) format = FORMAT_TYPE.ussd;
		else if (SERVICE_TYPE.call.equals((this._currentChannel))) format = FORMAT_TYPE.call;
		else if (SERVICE_TYPE.text.equals((this._currentChannel))) format = FORMAT_TYPE.text;
		else if (SERVICE_TYPE.email.equals((this._currentChannel))) format = FORMAT_TYPE.email;
		else if (SERVICE_TYPE.weblinks.equals((this._currentChannel))) format = FORMAT_TYPE.weblinks;
		else if (SERVICE_TYPE.info.equals((this._currentChannel))) format = FORMAT_TYPE.info;
		
		return TextUtils.isEmpty(this.formats.get(format));
	}
	
	private String parseVariables(String format, HashMap<String, String> valuestoUse, HashMap<String, String> _formats) {

		// Create pattern of the format "%(cat|beverage)%"
		String patternString = "\\[(" + TextUtils.join("|", valuestoUse.keySet()) + ")\\]";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(_formats.get(format));

		StringBuffer sb = new StringBuffer();
		while(matcher.find()) {
		    matcher.appendReplacement(sb, valuestoUse.get(matcher.group(1)));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	
	public void saveFieldValue(String fieldname, String fieldvalue) {
		this.values.put(fieldname, fieldvalue);
	}
	
	private void parseFormDescriptor(JSONArray _forms) throws JSONException {
		this.forms = new ArrayList<ArrayList<ServiceField>>(_forms.length());
		
		for (int i = 0; i < _forms.length(); i++) {
			JSONArray fields = _forms.getJSONArray(i);
			
			ArrayList<ServiceField> formFields = new ArrayList<ServiceField>(fields.length());
			
			for (int j = 0; j < fields.length(); j++) {
				formFields.add(new ServiceField(fields.getJSONObject(j), this));
			}
			
			this.forms.add(formFields);
			
		}
	}


	public static HashMap<String, String> ObjectToStringMap(JSONObject obj) throws JSONException {
		if (obj == null) return null;
		@SuppressWarnings("unchecked")
		Iterator<String> keys = obj.keys();
		HashMap<String, String> o = new HashMap<String, String>(obj.length());
		
		while (keys.hasNext()) {
			String key = keys.next();
			o.put(key, obj.getString(key));
		}
		
		return o;
	}
	public static ArrayList<String> ArrayToArrayListOfString(JSONArray arr) throws JSONException {
		if (arr == null) return null;
		ArrayList<String> a = new ArrayList<String>(arr.length());
		
		for (int i = 0 ; i < arr.length(); i++) {
			a.add(arr.getString(i));
		}
		
		return a;
	}
	public static ArrayList<ArrayList<String>> ArrayToArrayListOfArrayString(JSONArray arr) throws JSONException {
		if (arr == null) return null;
		ArrayList<ArrayList<String>> a = new ArrayList<ArrayList<String>>(arr.length());
		
		for (int i = 0 ; i < arr.length(); i++) {
			a.add(ArrayToArrayListOfString(arr.getJSONArray(i)));
		}
		
		return a;
	}

	public static ArrayList<SelectOptions> ArraysToSelectOptions(JSONArray keys, JSONArray values) throws JSONException {
		return ArraysToSelectOptions(ArrayToArrayListOfString(keys), ArrayToArrayListOfString(values));
		
	}

	public static ArrayList<SelectOptions> ArraysToSelectOptions(ArrayList<String> keys, ArrayList<String> values) throws JSONException {
		if (keys == null || values == null) return null;
		if (keys.size() != values.size()) throw new JSONException("Array size for select options do not match");
		
		ArrayList<SelectOptions> a = new ArrayList<SelectOptions>(keys.size());
		
		for (int i = 0 ; i < keys.size(); i++) {
			a.add(new SelectOptions(keys.get(i), values.get(i)));
		}
		
		return a;
	}
	
	

	
	
}