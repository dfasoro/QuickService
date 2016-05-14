package com.macgrenor.quickservice.engine;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.macgrenor.quickservice.QSApplication;
import com.macgrenor.quickservice.service.INPUT_TYPE;
import com.macgrenor.quickservice.service.SelectOptions;
import com.macgrenor.quickservice.service.ServiceField;

public class ViewEngine {
	public ArrayList<ServiceField> fields;
	public ArrayList<String> values;
	public ArrayList<View> valueViews;
	public ArrayList<View> displayViews;
	
	public void generateViews(Context ctx, ArrayList<ServiceField> fields, OnClickListener actionListener, OnClickListener contactListener) throws Exception {
		valueViews = new ArrayList<View>(fields.size());
		displayViews = new ArrayList<View>(fields.size());
		values = new ArrayList<String>(fields.size());
		this.fields = fields;
		

		for (int i = 0; i < fields.size(); i++) {
			ServiceField field = fields.get(i);
			generateView(ctx, i, field, actionListener, contactListener);
		}
	}
	
	private View generateView(final Context ctx, int position, ServiceField field, OnClickListener actionListener, OnClickListener contactListener) throws Exception {
		
		String name = field.name;
		String type = field.type;
		String value = field.getValue();
		ArrayList<SelectOptions> options = field.selectoptions;
		String label = field.label;
		String instruction = field.instruction;
		final String action = field.action;
		boolean autocomplete = field.autocomplete;
		boolean required = field.required;
		
		LinearLayout ll = new LinearLayout(ctx);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		ll.setOrientation(LinearLayout.VERTICAL);
		//AutoCompleteTextView , Spinner, TextView(big or small for label)
		//select/label/hidden
		
		if (label != null && label.length() > 0) {
			TextView tview = new TextView(ctx);
			tview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			tview.setTextAppearance(ctx, android.R.style.TextAppearance_Medium);
			tview.setText(label + (required ? " *" : ""));
			ll.addView(tview);
		}
		
		if (INPUT_TYPE.number.equalsIgnoreCase(type) || INPUT_TYPE.phonenumber.equalsIgnoreCase(type) || 
				INPUT_TYPE.email.equalsIgnoreCase(type) || INPUT_TYPE.text.equalsIgnoreCase(type)) {
			final AutoCompleteTextView atview = new AutoCompleteTextView(ctx);
			atview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			atview.setEms(10);
			atview.setText(value);
			
			if (INPUT_TYPE.number.equalsIgnoreCase(type)) atview.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
			if (INPUT_TYPE.phonenumber.equalsIgnoreCase(type)) atview.setInputType(InputType.TYPE_CLASS_PHONE);
			if (INPUT_TYPE.email.equalsIgnoreCase(type)) atview.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			if (INPUT_TYPE.text.equalsIgnoreCase(type)) atview.setInputType(InputType.TYPE_CLASS_TEXT);
			
			if (autocomplete) {
				ArrayList<String> autoComplete = QSApplication.ServiceDBInstance.getAutoCompleteValues(name);
				
				if (autoComplete != null) atview.setAdapter(new ArrayAdapter<String>(ctx,
		                 android.R.layout.simple_dropdown_item_1line, autoComplete));
			}
			if (!INPUT_TYPE.phonenumber.equalsIgnoreCase(type)) {
				ll.addView(atview);
			}
			else {
				LinearLayout ll_inner = new LinearLayout(ctx);
				ll_inner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				ll_inner.setOrientation(LinearLayout.HORIZONTAL);
				atview.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
				ll_inner.addView(atview);
				
				Button contact_picker = new Button(ctx);
				contact_picker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				contact_picker.setTag(position);
				contact_picker.setText("...");
				contact_picker.setTextColor(Color.BLUE);
				contact_picker.setOnClickListener(contactListener);
				ll_inner.addView(contact_picker);
				
				ll.addView(ll_inner);
				
			}
			
			valueViews.add(atview); 
			atview.setTag(position);
		}
		else if (INPUT_TYPE.textarea.equalsIgnoreCase(type)) {
			EditText mtview = new EditText(ctx);
			mtview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			mtview.setEms(10);
			mtview.setText(value);
			
			mtview.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			mtview.setMinLines(3);
			
			ll.addView(mtview);
			
			valueViews.add(mtview); 
			mtview.setTag(position);
		}
		else if (INPUT_TYPE.label.equalsIgnoreCase(type) || INPUT_TYPE.hidden.equalsIgnoreCase(type)) {
			TextView tview = new TextView(ctx);
			tview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			tview.setTextAppearance(ctx, android.R.style.TextAppearance_Medium);
			tview.setText(value);
			ll.addView(tview);
			
			if (action.length() > 0) {
				tview.setTextColor(Color.BLUE);
				tview.setOnClickListener(actionListener);
			}
			
			if (INPUT_TYPE.hidden.equalsIgnoreCase(type)) tview.setVisibility(View.GONE);

			valueViews.add(tview);
			tview.setTag(position);
		}
		else if (INPUT_TYPE.select.equalsIgnoreCase(type)) {
			Spinner sview = new Spinner(ctx);
			sview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			ArrayAdapter<SelectOptions> adapter = new ArrayAdapter<SelectOptions>(ctx,
		            android.R.layout.simple_spinner_item, options);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sview.setAdapter(adapter);
			
			int spinnerPosition = options.indexOf(new SelectOptions(value, ""));
		    if (spinnerPosition >= 0) sview.setSelection(spinnerPosition, false);

			ll.addView(sview);

			valueViews.add(sview);
			sview.setTag(position);
		}
		
		else throw new Exception("Could not find type definition");
		
		
		if (instruction != null && instruction.length() > 0) {
			TextView tview = new TextView(ctx);
			tview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			tview.setTextAppearance(ctx, android.R.style.TextAppearance_Small);
			tview.setText(instruction);
			ll.addView(tview);
		}
		
		//put some space between
		TextView tview = new TextView(ctx);
		tview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		tview.setTextAppearance(ctx, android.R.style.TextAppearance_Small);
		tview.setText(" ");
		ll.addView(tview);
		
		
		displayViews.add(ll);
		return ll;
	}
	
	public ArrayList<String> getFieldValues() {
		if (fields == null) return new ArrayList<String>();
		
		values = new ArrayList<String>(fields.size());
		

		for (int i = 0; i < fields.size(); i++) {
			//call on form objects to submit their values. and call setvalue on field.
			ServiceField field = fields.get(i);
			View view = valueViews.get(i);
			String type = field.type;
			
			String value = "";
			
			if (INPUT_TYPE.number.equalsIgnoreCase(type) || INPUT_TYPE.phonenumber.equalsIgnoreCase(type) || 
					INPUT_TYPE.email.equalsIgnoreCase(type) || INPUT_TYPE.text.equalsIgnoreCase(type) || 
					INPUT_TYPE.textarea.equalsIgnoreCase(type) || INPUT_TYPE.label.equalsIgnoreCase(type) || 
					INPUT_TYPE.hidden.equalsIgnoreCase(type)) {
				value = ((TextView)view).getText().toString();
			}
			else if (INPUT_TYPE.select.equalsIgnoreCase(type)) {
				value = ((SelectOptions)((Spinner)view).getSelectedItem()).getKey();
			}
			else {
				//continue;
				//throw new Exception("Invalid Implementation for input type.");
			}
			
			values.add(value);
		}
		
		return values;
	}
	
}
