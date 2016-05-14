package com.macgrenor.quickservice.service;

public class SelectOptions {
	private String key;
	private String text;
	
	public SelectOptions(String key, String text) {
		this.key = key;
		this.text = text;
	}

	public String getKey() {
		return key;
	}
	
	public String getText() {
		return text;
	}
	
	@Override
	public String toString() {
		return text;
	}
	
	@Override
	public boolean equals(Object o){
	    return o == null || !(o instanceof SelectOptions) ? false : this.key.equals(((SelectOptions)o).getKey());
	}
	
	@Override
	public int hashCode(){
	    return key.hashCode();
	}
}
