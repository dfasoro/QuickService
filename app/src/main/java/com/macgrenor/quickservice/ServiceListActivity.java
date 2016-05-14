package com.macgrenor.quickservice;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.macgrenor.quickservice.data.DataAccess;
import com.macgrenor.quickservice.data.Preferences;
import com.macgrenor.quickservice.data.ServiceListAdapter;
import com.macgrenor.quickservice.engine.External;
import com.macgrenor.quickservice.network.UpAndDanNotifier;

public class ServiceListActivity extends Activity implements UpAndDanNotifier {
	private Variables variables = new Variables();
	private ServiceListAdapter adapter;
	private final ArrayList<String> sortLabels = new ArrayList<String>(10);
	private final ArrayList<String> sortLabelsDefDirections = new ArrayList<String>(10);
	private final int BOOKMARK_MENU = 100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		Intent intent = getIntent();
		variables.favoriteMode = intent.getBooleanExtra("favoriteMode", false);
		
		sortLabels.add("Alphabetical"); sortLabelsDefDirections.add("Ascending");
		sortLabels.add("Recently Used"); sortLabelsDefDirections.add("Descending");
		sortLabels.add("Most Used"); sortLabelsDefDirections.add("Descending");
		sortLabels.add("Recently Added"); sortLabelsDefDirections.add("Descending");
		
		setContentView(R.layout.service_list);
		setMyTitle();

		if (variables.favoriteMode) {
			findViewById(R.id.favoriteInstruction).setVisibility(View.VISIBLE);
		}

		//modify order before here.
		adapter = ServiceListAdapter.getAdapter(this, variables.category, variables.subcategory, variables.filter, variables.order, 
				variables.orderDirection, variables.favoriteMode);
		
		// Find ListView to populate
		final ListView lvItems = (ListView) findViewById(R.id.serviceView);
		lvItems.setEmptyView(findViewById(R.id.empty_list_item));
		
		lvItems.setAdapter(adapter);
		lvItems.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Cursor cursor = (Cursor)lvItems.getAdapter().getItem(pos);
				FormActivity.launchMe(ServiceListActivity.this, cursor.getInt(cursor.getColumnIndexOrThrow("id")),
						adapter.getLastUsage(pos));
			}
		});

	    registerForContextMenu(lvItems);

		EditText myFilter = (EditText) findViewById(R.id.filterText);
		myFilter.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				variables.filter = s.toString().trim();
				if (variables.filter.length() == 0 || variables.filter.length() >= 2) reloadList();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
		});
		
		DataAccess.loadCategories();
		

		QSApplication.UpAndDanThread.setNotifier(this);

		if (!QSApplication.Preferences.INITED) {
			Toast.makeText(this, "Loading Services...", Toast.LENGTH_SHORT).show();
		}
		else {
			if (!QSApplication.isValidVersion()) {
				Toast.makeText(this, "You need to upgrade this app in the App Store", Toast.LENGTH_LONG).show();
			}
			else {
				long timeDiff = QSApplication.syncMonthsDifference();
				if (timeDiff > 0) {
					Toast.makeText(this, timeDiff + " month(s) have elapsed since this app synced up with the server.\nTurn on your network now!", Toast.LENGTH_LONG).show();
				}
			}
		}
	}


	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("category", variables.category);
		savedInstanceState.putString("subcategory", variables.subcategory);
		savedInstanceState.putString("order", variables.order);
		savedInstanceState.putString("orderDirection", variables.orderDirection);
		savedInstanceState.putString("filter", variables.filter);
		savedInstanceState.putBoolean("favoriteMode", variables.favoriteMode);
	    
	    // Always call the superclass so it can save the view hierarchy state
	    super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState) {
		variables.category = savedInstanceState.getString("category");
		variables.subcategory = savedInstanceState.getString("subcategory");
		variables.order = savedInstanceState.getString("order");
		variables.orderDirection = savedInstanceState.getString("orderDirection");
		variables.filter = savedInstanceState.getString("filter");
		variables.favoriteMode = savedInstanceState.getBoolean("favoriteMode");
	    
	    // Always call the superclass so it can save the view hierarchy state
	    super.onRestoreInstanceState(savedInstanceState);
	}
	
	
	private void setMyTitle() {
		String serviceTitle = null;
		if (variables.favoriteMode) {
			serviceTitle = "Favorite";
		}
		else {
			if (!TextUtils.isEmpty(variables.subcategory)) serviceTitle = variables.subcategory;
			else if (!TextUtils.isEmpty(variables.category)) serviceTitle = variables.category;
			else serviceTitle = "All";
		}
		serviceTitle += " Services" + (variables.favoriteMode ? "" : " (" + variables.order + ")");
		setTitle(serviceTitle);
	}
	
	private void reloadList() {
		adapter.filterAdapter(ServiceListActivity.this, variables.category, variables.subcategory, variables.filter, variables.order, 
				variables.orderDirection, variables.favoriteMode);
		setMyTitle();
	}
	
	@SuppressLint("InflateParams")
	public void showFilterDialog() {
		final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("Filter");
		final View dialogView = getLayoutInflater().inflate(R.layout.filter, null);
		
		builderSingle.setView(dialogView);
		
		final Spinner categoryChooser = (Spinner)dialogView.findViewById(R.id.categoryChooser);
		final Spinner subCategoryChooser = (Spinner)dialogView.findViewById(R.id.subCategoryChooser);
		setupSpinner(this, categoryChooser, QSApplication.ServiceDBInstance.getCategories(), variables.category, true);
		setupSpinner(this, subCategoryChooser, 
				(TextUtils.isEmpty(variables.category) ? new ArrayList<String>() : QSApplication.ServiceDBInstance.getSubCategories(variables.category)), 
				variables.subcategory, true);
		
		categoryChooser.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String _category = pos == 0 ? "" : (String)categoryChooser.getSelectedItem();
				setupSpinner(ServiceListActivity.this, subCategoryChooser, 
						(TextUtils.isEmpty(_category) ? new ArrayList<String>() : QSApplication.ServiceDBInstance.getSubCategories(_category)), 
						variables.subcategory, true);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});
		
		builderSingle.setNegativeButton(
		        "Cancel",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
		            }
		        });
	
		builderSingle.setPositiveButton(
		        "Ok",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	int cpos = categoryChooser.getSelectedItemPosition(), scpos = subCategoryChooser.getSelectedItemPosition();
		            	variables.category = cpos == 0 ? "" : (String)categoryChooser.getSelectedItem();
		            	variables.subcategory = scpos == 0 ? "" : (String)subCategoryChooser.getSelectedItem();
		            	dialog.dismiss();
		            	reloadList();
		            }
		        });
	
		builderSingle.show();
	}


	@SuppressLint("InflateParams")
	public void showSortDialog() {
		final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("Sort");
		final View dialogView = getLayoutInflater().inflate(R.layout.sort, null);
		final ArrayList<String> orderDirections = new ArrayList<String>(2);
		orderDirections.add("Ascending"); orderDirections.add("Descending"); 
		
		builderSingle.setView(dialogView);
		
		final Spinner orderByChooser = (Spinner)dialogView.findViewById(R.id.orderByChooser);
		final Spinner orderDirectionChooser = (Spinner)dialogView.findViewById(R.id.orderDirectionChooser);
		setupSpinner(this, orderByChooser, sortLabels, variables.order, false);
		setupSpinner(this, orderDirectionChooser, orderDirections, variables.orderDirection, false);
		
		orderByChooser.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String _defDirection = sortLabelsDefDirections.get(pos);
				setupSpinner(ServiceListActivity.this, orderDirectionChooser, orderDirections, _defDirection, false);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});
		
		builderSingle.setNegativeButton(
		        "Cancel",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
		            }
		        });
	
		builderSingle.setPositiveButton(
		        "Ok",
		        new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	variables.order = (String)orderByChooser.getSelectedItem();
		            	variables.orderDirection = (String)orderDirectionChooser.getSelectedItem();
		            	dialog.dismiss();
		            	reloadList();
		            }
		        });
	
		builderSingle.show();
	}

	private void setupSpinner(Context ctx, Spinner sview, ArrayList<String> _options, String value, boolean empty) {
		ArrayList<String> options = new ArrayList<String>(_options.size() + 1);
		if (empty) options.add("[Please Select]");
		options.addAll(_options);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx,
	            android.R.layout.simple_spinner_item, options);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sview.setAdapter(adapter);
		
		int spinnerPosition = options.indexOf(value);
	    if (spinnerPosition >= 0) sview.setSelection(spinnerPosition, false);

	}
	
	private void loadFavoriteActivity() {
		Intent myIntent = new Intent(this, ServiceListActivity.class);
		myIntent.putExtra("favoriteMode", true);
		startActivity(myIntent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!variables.favoriteMode) {
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.list, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!variables.favoriteMode) {
			// Handle action bar item clicks here. The action bar will
			// automatically handle clicks on the Home/Up button, so long
			// as you specify a parent activity in AndroidManifest.xml.
			int id = item.getItemId();
			if (id == R.id.action_filter) {
				showFilterDialog();
				return true;
			}
			else if (id == R.id.action_sort) {
				showSortDialog();
				return true;
			}
			else if (id == R.id.action_refresh) {
				reloadList();
				return true;
			}
			else if (id == R.id.action_filter_favorites) {
				loadFavoriteActivity();
				return true;
			}
			else if (id == R.id.action_pull) {
				QSApplication.UpAndDanThread.forceReStart();
				return true;
			}
			else if (id == R.id.action_feedback) {
				External.openBrowser(this, Preferences.FEEDBACK_URL);
				return true;
			}
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	  if (v.getId() == R.id.serviceView) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    boolean bookmarked = adapter.isBookMarked(info.position);
	    menu.add(Menu.NONE, BOOKMARK_MENU, 0, bookmarked ? "Unfavorite" : "Favorite");
	  }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {	  
	  if (item.getItemId() == BOOKMARK_MENU) {
		  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		  adapter.toggleBookMark(info.position);
		  ((ListView) findViewById(R.id.serviceView)).invalidateViews();
		  return true;
	  }
	  return super.onContextItemSelected(item);
	}
	


	@Override
	public void initReady() {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				reloadList();
				Toast.makeText(ServiceListActivity.this, "Services now loaded", Toast.LENGTH_SHORT).show();
			}
		});
		
	}

	@Override
	public void newServices(final int count) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				Toast.makeText(ServiceListActivity.this, count + " new service(s) now available. Refresh list to see it.", Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void updatedServices(final int count) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				Toast.makeText(ServiceListActivity.this, (count == 0 ? "No new/updated services." : count + " services updated."), Toast.LENGTH_SHORT).show();
			}
		});
	}

	/*
	@Override
	protected void onPause() {
		super.onPause();
		QSApplication.UpAndDanThread.setNotifier(null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		QSApplication.UpAndDanThread.setNotifier(this);
	}*/

	@Override
	protected void onDestroy() {
		super.onDestroy();
		QSApplication.UpAndDanThread.setNotifier(null);
	}
	
	private class Variables {
		public String category = "";
		public String subcategory = "";
		public String order = "Alphabetical";
		public String orderDirection = "Ascending";
		public String filter = "";
		public boolean favoriteMode;
		
	}
}
