package com.macgrenor.quickservice.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class ServiceListAdapter extends CursorAdapter {
	private final SparseBooleanArray bookmarks = new SparseBooleanArray(20);
	private static final SparseIntArray last_use = new SparseIntArray(20);
	
	public ServiceListAdapter(Context context, Cursor cursor, int flags) {
	      super(context, cursor, flags);
	      	  
	  }

	  // The newView method is used to inflate a new view and return it, 
	  // you don't bind any data to the view at this point. 
	  @Override
	  public View newView(Context context, Cursor cursor, ViewGroup parent) {
	      return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
	  }

	  // The bindView method is used to bind all data to a given view
	  // such as setting the text on a TextView. 
	  @Override
	  public void bindView(View view, Context context, Cursor cursor) {
	      // Find fields to populate in inflated template
	      TextView tvName = (TextView) view.findViewById(android.R.id.text1);
	      TextView tvTag = (TextView) view.findViewById(android.R.id.text2);
	      // Extract properties from cursor
	      String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
	      String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
	      // Populate fields with extracted properties
	      tvName.setText(name);
	      tvName.setTextAppearance(context, android.R.style.TextAppearance_Medium);
	      tvTag.setText(tags);
	      tvTag.setTextAppearance(context, android.R.style.TextAppearance_Small);
	      //if (TextUtils.isEmpty(tags)) tvTag.setVisibility(View.GONE);
	      
	      boolean promoted = cursor.getInt(cursor.getColumnIndexOrThrow("promoted")) == 0 ? false : true;
	      boolean bookmarked = isBookMarked(cursor);
	    		  
	      if (bookmarked) {
	    	  tvName.setTextColor(Color.rgb(0, 200, 0));
	      }
	      else if (promoted) {
	    	  tvName.setTextColor(Color.BLUE);
	      }
	  }
	  
	  public static ServiceListAdapter getAdapter(Context ctx, String category, String subcategory, String filter, String order,
			  String orderDirection, boolean favoriteMode) {		  
			return new ServiceListAdapter(ctx, DataAccess.getServiceCursor(ctx, category, subcategory, filter, order, orderDirection, favoriteMode), 0);
	  }

	public void toggleBookMark(int position) {
		Cursor cursor = (Cursor) getItem(position);

		int service_id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
		boolean bookmarkedToggle = !isBookMarked(cursor);

		DataAccess.bookmark(service_id, bookmarkedToggle);
		bookmarks.put(service_id, bookmarkedToggle);
	}

	public static void setLastUsage(int service_id, int last_use_id) {
		last_use.put(service_id, last_use_id);
	}

	public int getLastUsage(int position) {
		Cursor cursor = (Cursor) getItem(position);
		int service_id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
		return last_use.get(service_id,
				cursor.getInt(cursor.getColumnIndexOrThrow("last_use_id")));
	}

	public boolean isBookMarked(int position) {
		  Cursor cursor = (Cursor) getItem(position);
		  return isBookMarked(cursor);
	  }

	  private boolean isBookMarked(Cursor cursor) {
		  int service_id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
	      
		  return bookmarks.get(service_id,
    			  cursor.getInt(cursor.getColumnIndexOrThrow("bookmarked")) == 0 ? false : true);
	  }
	  
	  public void filterAdapter(Context ctx, String category, String subcategory, String filter, String order,
			  String orderDirection, boolean favoriteMode) {		  
			this.changeCursor(DataAccess.getServiceCursor(ctx, category, subcategory, filter, order, orderDirection, favoriteMode));
			bookmarks.clear();
		    last_use.clear();
	  }
}
