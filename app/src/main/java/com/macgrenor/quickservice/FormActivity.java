package com.macgrenor.quickservice;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.macgrenor.quickservice.engine.ViewEngine;
import com.macgrenor.quickservice.service.SERVICE_TYPE;
import com.macgrenor.quickservice.service.SelectOptions;
import com.macgrenor.quickservice.service.Service;
import com.macgrenor.quickservice.service.ServiceField;

public class FormActivity extends Activity {
	private ProgressDialog loginProgressDialog;
	private final int DIALOG_PROGRESS_SPINNER = 62;
	private Service service;
	private ViewEngine vwe;
	private final int PICK_CONTACT = 10;
	private View numberPicker;
	private int usage_id, service_id;
	private static Service savedService = null;
	
	public static void launchMe(Context ctx, int service_id, int usage_id) {
		if (!QSApplication.isValidVersion()) {
			Toast.makeText(ctx, "You need to upgrade this app in the App Store\nThis Version no longer works.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent myIntent = new Intent(ctx, FormActivity.class);
		myIntent.putExtra("service_id", service_id);
		myIntent.putExtra("usage_id", usage_id);
		ctx.startActivity(myIntent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		if (savedInstanceState != null && savedInstanceState.containsKey("service_name")) {
			service = savedService;
		}
		savedService = null;
		
		
		Intent intent = getIntent();
		service_id = intent.getIntExtra("service_id", 0);
		usage_id = intent.getIntExtra("usage_id", 0);
		int startForm = 1;
		
		setContentView(R.layout.template);
		setTitle("Form");
		
		try {
			if (service == null) {
				service = new Service(service_id);
			}
			else {
				startForm = service._currentForm;
				service._currentForm = 0;
			}
			
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			this.finish();
			//this should be a major system error that bypassed inside testing
			//apart from displaying a service error and exiting.
			//it should also be logged to the server for analysis with as much data as possible.
			e.printStackTrace();
			return;
		}
		
		setTitle(service.name);
		prepServiceChooser();
		

		vwe = new ViewEngine();
		//vwe.generateViews(this);
		if (!prepareForm(startForm)) return;
		
		


		Button prevButton = (Button)findViewById(R.id.prevButton);
		Button goButton = (Button)findViewById(R.id.goButton);
		Button nextButton = (Button)findViewById(R.id.nextButton);
		prevButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				moveBack();
			}
		});
		nextButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				moveFront();
			}
		});
		
		goButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				go();
			}
		});
	}

	private void moveBack() {
		prepareForm(service._prevForm);
	}

	private void moveFront() {
		if (validateForm()) prepareForm(service._nextForm);
	}

	private void go() {
		if (validateForm()) {
			//push to necessary service.
			service.sendOutput(FormActivity.this, vwe.getFieldValues());
			if (service.isReadonly()) setFieldsReadonly(true);
			exitActivity();
		}
	}

	private void reloadLast() {
		try {
			service = new Service(service_id, usage_id);
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			this.finish();
			//this should be a major system error that bypassed inside testing
			//apart from displaying a service error and exiting.
			//it should also be logged to the server for analysis with as much data as possible.
			e.printStackTrace();
			return;
		}
		prepServiceChooser();
		prepareForm(1);
	}

	private void editForm() {
		setFieldsReadonly(false);
		service.removeReadonly();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    // Save the user's current game state
		service.saveForm(vwe.getFieldValues()); //this is basically to just save state 
		savedService = service;
		savedInstanceState.putString("service_name", service.name); //we need to put at least a key here for onRestore to be called.
	    
	    // Always call the superclass so it can save the view hierarchy state
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	private void setFieldsReadonly(boolean value) {
		findViewById(R.id.typeChooser).setEnabled(!value);
		for (int i = 0; i < vwe.valueViews.size(); i++) {
			vwe.valueViews.get(i).setEnabled(!value);
		}
		invalidateOptionsMenu();
	}
	
	private boolean prepareForm(int form_id) {
		service._currentChannel = ((SelectOptions)((Spinner)findViewById(R.id.typeChooser)).getSelectedItem()).getKey();

		if (service._prevForm == 0) {
			TextView preInstruction = (TextView) findViewById(R.id.preInstruction);
			if (service.pre_instruction.containsKey(service._currentChannel) && service.pre_instruction.get(service._currentChannel).length() != 0) {
				preInstruction.setText(service.pre_instruction.get(service._currentChannel));
				preInstruction.setVisibility(View.VISIBLE);
			} else if (service.pre_instruction.containsKey(SERVICE_TYPE.all) && service.pre_instruction.get(SERVICE_TYPE.all).length() != 0) {
				preInstruction.setText(service.pre_instruction.get(SERVICE_TYPE.all));
				preInstruction.setVisibility(View.VISIBLE);
			} else preInstruction.setVisibility(View.GONE);
		}

		if (service._nextForm == 0) {
			TextView postInstruction = (TextView) findViewById(R.id.postInstruction);
			if (service.post_instruction.containsKey(service._currentChannel) && service.post_instruction.get(service._currentChannel).length() != 0) {
				postInstruction.setText(service.post_instruction.get(service._currentChannel));
				postInstruction.setVisibility(View.VISIBLE);
			} else if (service.post_instruction.containsKey(SERVICE_TYPE.all) && service.post_instruction.get(SERVICE_TYPE.all).length() != 0) {
				postInstruction.setText(service.post_instruction.get(SERVICE_TYPE.all));
				postInstruction.setVisibility(View.VISIBLE);
			} else postInstruction.setVisibility(View.GONE);
		}
				
		LinearLayout container = (LinearLayout)findViewById(R.id.formContainer);
		container.removeAllViews();
		
		final ArrayList<ServiceField> fields = service.prepareForm(form_id, vwe.getFieldValues());
		try {
			vwe.generateViews(this, fields, new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					int position = (Integer) v.getTag();
					ServiceField field = fields.get(position);
					try {
						service.sendSubActionOutput(FormActivity.this, field.name, vwe.getFieldValues(), field.action, field.action_payload);
					} catch (Exception e) {
						Toast.makeText(FormActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
			}, new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					int position = (Integer) v.getTag();
					numberPicker = vwe.valueViews.get(position);
					Intent intent = new Intent(Intent.ACTION_PICK);
		            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		            startActivityForResult(intent, PICK_CONTACT);
				}
			});
		}
		catch (Exception e) {
			//this should be a major system error that bypassed inside testing
			//apart from displaying a service error and exiting.
			//it should also be logged to the server for analysis with as much data as possible.
			e.printStackTrace();
			return false;
		}
		
		if (service.isReadonly()) setFieldsReadonly(true);
		
		for (int i = 0; i < vwe.displayViews.size(); i++) {
			container.addView(vwe.displayViews.get(i));
		}

		Button prevButton = (Button)findViewById(R.id.prevButton);
		Button goButton = (Button)findViewById(R.id.goButton);
		Button nextButton = (Button)findViewById(R.id.nextButton);

		prevButton.setVisibility(service._prevForm == 0 ? View.GONE : View.VISIBLE);
		goButton.setVisibility(!(service._nextForm == 0) || service.emptyFormat() ? View.GONE : View.VISIBLE);
		nextButton.setVisibility(service._nextForm == 0 ? View.GONE : View.VISIBLE);

		goButton.setText(SERVICE_TYPE.getServiceActionLabels().get(service._currentChannel));

		invalidateOptionsMenu();
	    return true;
	}
	
	private boolean validateForm() {
		ArrayList<String> errors = service._validateForm(vwe.getFieldValues());
		if (errors.size() > 0) {
			Toast.makeText(this, errors.get(0), Toast.LENGTH_LONG).show();
		}
		
		return errors.size() == 0;
	}
	
	private void prepServiceChooser() {
		Spinner sTypeChooser = (Spinner)findViewById(R.id.typeChooser);
		TextView sTypeText = (TextView)findViewById(R.id.typeChooserText);
		
		ArrayAdapter<SelectOptions> adapter = new ArrayAdapter<SelectOptions>(this,
	            android.R.layout.simple_spinner_item, service.types);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sTypeChooser.setAdapter(adapter);
		
		int spinnerPosition = service.types.indexOf(new SelectOptions(service.defType, ""));
	    if (spinnerPosition >= 0) sTypeChooser.setSelection(spinnerPosition, false);
	    
	    sTypeChooser.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {
				prepareForm(1); //start from form 1 with new value;
			}
			@Override
		    public void onNothingSelected(AdapterView<?> parent) {
		    }
	    	
		});

		if (service.types.size() < 2) {
			sTypeChooser.setVisibility(View.GONE);
			sTypeText.setVisibility(View.GONE);
		}
	}

	 //code 
	  @Override
	 public void onActivityResult(int reqCode, int resultCode, Intent data) {
		 super.onActivityResult(reqCode, resultCode, data);
	
		 switch (reqCode) {
		 case (PICK_CONTACT) :
		   if (resultCode == Activity.RESULT_OK) {
			   Cursor cursor = null;
	            String phoneNumber = "", displayName = "";

	            ArrayList<String> allNumbers = new ArrayList<String>();
	            int phoneColumnID = 0, nameColumnID = 0;
	            try {
	                Uri result = data.getData();
	                String id = result.getLastPathSegment();
	                cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { id }, null);
	                cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
	                phoneColumnID = cursor.getColumnIndex(Phone.DATA);
	                nameColumnID = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

	                if (cursor.moveToFirst()) {
	                    while (cursor.isAfterLast() == false) {
	                        //idContactBook = cursor.getString(contactIdColumnId);
	                        displayName = cursor.getString(nameColumnID);
	                        phoneNumber = cursor.getString(phoneColumnID);

	                        if (phoneNumber.length() == 0) continue;

	                        //int type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
	                        
	                        allNumbers.add(phoneNumber);
	                        cursor.moveToNext();
	                    }
	                } else {
	                    // no results actions
	                }
	                
	                if (allNumbers.size() > 0) {
	                	if (allNumbers.size() == 1) {
	                		((TextView)numberPicker).setText(allNumbers.get(0));
	                	}
	                	else displayPhoneNumberDialog(allNumbers, displayName);
	                }
	            } 
	            catch (Exception e) {
	                // error actions
	            	e.printStackTrace();
	            } 
	            finally {
	                if (cursor != null) {
	                    cursor.close();
	                }
            	}
		     
		   }
		   break;
		   
		   
		 }
	 }
	
	
	
	private void displayPhoneNumberDialog(ArrayList<String> numbers, String name) {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("Select Number for: " + name);
	
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
		        this,
		        android.R.layout.select_dialog_singlechoice, numbers);
		
		builderSingle.setNegativeButton(
		        "Cancel",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
		            }
		        });
	
		builderSingle.setAdapter(
		        arrayAdapter,
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                String strPhone = arrayAdapter.getItem(which);
		                ((TextView)numberPicker).setText(strPhone);
		            }
		        });
		builderSingle.show();
	}
	
	private void exitActivity() {
		finish();
	}
	


	private void displayPostActivityDialog(String postInstruction) {
		/* AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("Info");
	
		builderSingle.setPositiveButton(
		        "OK",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
		                exitActivity();
		            }
		        });
	
		builderSingle.setNegativeButton(
		        "Retry",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
		            }
		        });
	
		builderSingle.setMessage(postInstruction);
		
		builderSingle.show();
	*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		if (service == null) return true;
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.form, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (service == null) return true;

		menu.findItem(R.id.action_previous).setVisible(service._prevForm != 0);
		menu.findItem(R.id.action_next).setVisible(service._nextForm != 0);
		menu.findItem(R.id.action_go).setVisible(service._nextForm == 0 && !service.emptyFormat())
									.setTitle(SERVICE_TYPE.getServiceActionLabels().get(service._currentChannel));
		menu.findItem(R.id.action_edit).setVisible(service.isReadonly());
		menu.findItem(R.id.action_reload).setVisible(usage_id != 0);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_previous) {
			moveBack();
			return true;
		}
		else if (id == R.id.action_next) {
			moveFront();
			return true;
		}
		else if (id == R.id.action_go) {
			go();
			return true;
		}
		else if (id == R.id.action_edit) {
			editForm();
			return true;
		}
		else if (id == R.id.action_reload) {
			reloadLast();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0 && service._prevForm != 0) {
	    	prepareForm(service._prevForm);
	        return true; 
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onBackPressed() {
	   if (service._prevForm == 0) {
		   super.onBackPressed();
	   }
	   else {
		   prepareForm(service._prevForm);
	   }
	}
}
