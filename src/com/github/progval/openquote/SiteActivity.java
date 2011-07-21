/**
 * 
 */
package com.github.progval.openquote;

// Project specific
import com.github.progval.openquote.SiteItem;

// User interface
import android.text.ClipboardManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

// Utils
import java.io.IOException;
import java.util.ArrayList;
import java.lang.Void;

// Android
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 * Abstract class for a source site.
 * 
 * @author ProgVal
 *
 */

public abstract class SiteActivity extends ListActivity implements OnClickListener {
	/* *******************************
	 *  Site-specific data
	 ********************************/
	public abstract String getName();
	public abstract int getLowestPageNumber();  // 1 for most of the sites, but 0 for VDM.

	/* ************************************
	 *  State
	 *************************************/
	public enum Mode {
	    LATEST, TOP, RANDOM
	}
	protected Mode mode;
	protected int page;

	/* ************************************
	 *  Storage
	 *************************************/
	private ArrayList<SiteItem> listItemsMetadata = new ArrayList<SiteItem>();
	private ArrayList<String> listItems = new ArrayList<String>();
	ArrayAdapter<String> adapter;



	/* ************************************
	 *  User interface building and handling
	 *************************************/
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.page = this.getLowestPageNumber();
		this.setAdapter();
		this.bindButtons();
		this.initializeContextMenu();
		this.onClick(findViewById(R.id.buttonLatest));
	}
	private void setAdapter() {
		setContentView(R.layout.siteactivity);
		adapter=new ArrayAdapter<String>(this,
				R.layout.siteitem,
				listItems);
		setListAdapter(adapter);
	}
	/** Set onClickListener for the buttons */
	private void bindButtons() {
		findViewById(R.id.buttonLatest).setOnClickListener(this);
		findViewById(R.id.buttonTop).setOnClickListener(this);
		findViewById(R.id.buttonRandom).setOnClickListener(this);
		findViewById(R.id.buttonPrevious).setOnClickListener(this);
		findViewById(R.id.buttonNext).setOnClickListener(this);
	}
	/** Initialize the context menu. */
	private void initializeContextMenu() {
		ListView listView = getListView();
		registerForContextMenu(listView);
	}
	/** Called when any button is clicked. */
	public void onClick(View v) {
		enablePageChange(true);
		switch (v.getId()) {
			case R.id.buttonLatest: // Display latest quotes
				this.mode = Mode.LATEST;
				this.page = this.getLowestPageNumber();
				this.refresh();
				break;
			case R.id.buttonTop: // Display top quotes
				this.mode = Mode.TOP;
				this.page = this.getLowestPageNumber();
				this.refresh();
				break;
			case R.id.buttonRandom: // Display random quotes
				this.mode = Mode.RANDOM;
				this.page = this.getLowestPageNumber();
				enablePageChange(false);
				this.refresh();
				break;
			case R.id.buttonPrevious: // Open previous page
				if (this.page > this.getLowestPageNumber()) {
					this.page--;
				}
				this.refresh();
				break;
			case R.id.buttonNext: // Open next page
				this.page++;
				this.refresh();
				break;
		}
	}
	public void enablePageChange(boolean mode) {
		findViewById(R.id.buttonPrevious).setEnabled(mode);
		findViewById(R.id.buttonNext).setEnabled(mode);
	}
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.quote_context_menu, menu);
	}
	public boolean onContextItemSelected(MenuItem item) {
		int clickedQuote = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
		switch (item.getItemId()) {
			case R.id.siteactivity_context_copy:
				ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
				clipboard.setText(listItemsMetadata.get(clickedQuote).getContent());
				return true;
		}
		return false;
	}

	/* ************************************
	 *  Error display
	 *************************************/
	/** Same as showErrorDialog() with a generic error message for network issues */
	public void showIOExceptionDialog() {
		this.showErrorDialog(getResources().getString(R.string.siteactivity_network_error_message));
	}
	/** Display an error dialog */
	public void showErrorDialog(String message) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle(getResources().getString(R.string.siteactivity_error_title));
		adb.setMessage(message);
		adb.setPositiveButton(getResources().getString(R.string.siteactivity_error_button), null);
		adb.show();
	}

	/* ************************************
	 *  Fetch quotes
	 *************************************/
	private SiteItem[] getQuotes() throws IOException {
		switch(this.mode) {
			case LATEST:
				return this.getLatest(this.page);
			case TOP:
				return this.getTop(this.page);
			case RANDOM:
				return this.getRandom(this.page);
		}
		return new SiteItem[0];
	}
	/** Populate the activity interface with latest quotes */
	public SiteItem[] getLatest() throws IOException {
		return this.getLatest(this.getLowestPageNumber());
	}
	/** Populate the activity interface with the n-th page of latest quotes */
	public abstract SiteItem[] getLatest(int page) throws IOException;
	/** Populate the activity interface with top quotes */
	public SiteItem[] getTop() throws IOException {
		return this.getTop(this.getLowestPageNumber());
	}
	/** Populate the activity interface with the n-th page of top quotes */
	public abstract SiteItem[] getTop(int page) throws IOException;
	/** Populate the activity interface with random quotes */
	public SiteItem[] getRandom() throws IOException {
		return this.getRandom(this.getLowestPageNumber());
	}
	/** Populate the activity interface with the n-th page of random quotes */
	public abstract SiteItem[] getRandom(int page) throws IOException;

	/* ************************************
	 *  Display quotes
	 *************************************/
	private class AsyncQuotesFetcher extends AsyncTask<Void, Void, Void> {
		private SiteItem[] items;
		private String errorLog;
		ProgressDialog dialog;

		protected void onPreExecute() {
			dialog = ProgressDialog.show(SiteActivity.this, "", getResources().getString(R.string.siteactivity_loading_quotes), true);
		}

		protected Void doInBackground(Void... foo) {
			try {
				items = SiteActivity.this.getQuotes();
			}
			catch (Exception e) {
				if (e instanceof IOException) {
					items = new SiteItem[0];
				}
				else {
					errorLog = e.toString();
				}
			}
			return null;
		}
		protected void onPostExecute(Void foo) {
			dialog.dismiss();
			if (errorLog != null) {
				SiteActivity.this.showErrorDialog(String.format(getResources().getString(R.string.siteactivity_unknown_error), errorLog));
			}
			else if (items == null) {
				SiteActivity.this.showErrorDialog(getResources().getString(R.string.siteactivity_no_errors_no_results));
			}
			else if (items.length > 0) {
				SiteActivity.this.clearList();
				for (SiteItem item : items) {
					SiteActivity.this.addItem(item, false);
				}
				adapter.notifyDataSetChanged();
			}
			else {
				SiteActivity.this.showIOExceptionDialog();
			}
		}
	}
	/** Load the quotes, and add them to the ListView */
	public void refresh() {
		this.updateTitle();
		new AsyncQuotesFetcher().execute();
	}
	/** Add an item to the list */
	private void addItem(SiteItem item, boolean top) {
		if (top) {
			listItemsMetadata.add(0, item);
			listItems.add(0, item.toString());
		}
		else {
			listItemsMetadata.add( item);
			listItems.add(item.toString());
		}
		adapter.notifyDataSetChanged();
	}
	/** Prepend an item to the list */
	public void addItem(SiteItem item) {
		addItem(item, true);
	}
	/** Clear the list */
	public void clearList() {
		listItems.clear();
		adapter.notifyDataSetChanged();
	}

	/* ************************************
	 *  Other user interface handling
	 *************************************/
	/** Format and set the title according to the activity state. */
	public void updateTitle() {
		int humanReadablePage = page - this.getLowestPageNumber() + 1;
		setTitle(String.format(getResources().getString(R.string.siteactivity_title), this.getName(), this.getModeString(), humanReadablePage));
	}
	/** Returns the mode in the current locale */
	public String getModeString() {
		switch (this.mode) {
			case LATEST:
				return getResources().getString(R.string.siteactivity_mode_latest);
			case TOP:
				return getResources().getString(R.string.siteactivity_mode_top);
			case RANDOM:
				return getResources().getString(R.string.siteactivity_mode_random);
			default:
				return getResources().getString(R.string.siteactivity_mode_unknown);
		}
	}
}
