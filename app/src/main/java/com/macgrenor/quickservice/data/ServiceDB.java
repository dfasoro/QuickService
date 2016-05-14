package com.macgrenor.quickservice.data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.util.SparseArray;

import com.macgrenor.quickservice.service.ServiceUsage;


public class ServiceDB {
	
	private final HashMap<String, ArrayList<String>> autoCompleteMap = new HashMap<String, ArrayList<String>>(20);;

	private final HashMap<String, ArrayList<String>> categoryMap = new HashMap<String, ArrayList<String>>(20);;

	private final SparseArray<ServiceUsage> usageDrafts = new SparseArray<ServiceUsage>(20);
	
	public synchronized ArrayList<String> getAutoCompleteValues(String name) {
		if (!autoCompleteMap.containsKey(name)) {
			autoCompleteMap.put(name, DataAccess.getAutoComplete(name));
			Collections.sort(autoCompleteMap.get(name));
		}
		return autoCompleteMap.get(name);
	}
	
	public synchronized void addAutoCompleteValues(String name, String value) {
		ArrayList<String> autoMap = getAutoCompleteValues(name);
		if (!autoMap.contains(value)) {
			autoMap.add(value);
			Collections.sort(autoMap);
		}
	}

	public synchronized ArrayList<String> getCategories() {
		ArrayList<String> categories = new ArrayList<String>(categoryMap.keySet());
		Collections.sort(categories);
		return categories;
	}

	public synchronized ArrayList<String> getSubCategories(String category) {
		if (!categoryMap.containsKey(category)) {
			categoryMap.put(category, new ArrayList<String>(10));
		}
		return categoryMap.get(category);
	}

	
	public synchronized void addCategory(String category, String subcategory) {
		ArrayList<String> subcategories = getSubCategories(category);
		if (!subcategories.contains(subcategory)) {
			subcategories.add(subcategory);
			Collections.sort(subcategories);
		}
	}

	public ServiceUsage getServiceDraft(int service_id) {
		return usageDrafts.get(service_id);
	}
	
	public void addServiceDraft(int service_id, ServiceUsage value) {
		usageDrafts.put(service_id, value);
	}
	
	public void removeServiceDraft(int service_id) {
		usageDrafts.remove(service_id);
	}

}
