package com.macgrenor.quickservice.service;

public class INPUT_TYPE {
	public static final String number = "number";
	public static final String phonenumber = "phonenumber";
	public static final String email = "email";
	public static final String select = "select";
	public static final String label = "label";
	public static final String text = "text";
	public static final String textarea = "textarea";
	public static final String hidden = "hidden";
	

	//we need to ensure that whenever we add a new input type, we 
	//1) modify viewengine.generateView to accomodate the new entry
	//2) Servive._saveForm to perform the appropriate casting to save.
}
