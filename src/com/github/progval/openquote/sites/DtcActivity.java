/**
 * 
 */
package com.github.progval.openquote.sites;
// Project specific
import com.github.progval.openquote.sites.DtcItem;
import com.github.progval.openquote.SiteActivity;

//Networking
import java.io.IOException;

//Parsing HTML
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;

/**
 * @author ProgVal
 *
 */
public class DtcActivity extends SiteActivity {
	public String getName() { return "DTC"; }
	public int getLowestPageNumber() { return 1; }

	public DtcItem[] getLatest(AsyncQuotesFetcher task, int page) throws IOException {
		return this.parsePage(task, "/latest/" + String.valueOf(page) +".html");
	}
	public DtcItem[] getTop(AsyncQuotesFetcher task, int page) throws IOException {
		if (page != this.getLowestPageNumber()) {
			this.showNonSupportedFeatureDialog();
			this.page = this.getLowestPageNumber();
			return new DtcItem[0];
		}
		else {
			return this.parsePage(task, "/top50.html");
		}
	}
	public DtcItem[] getRandom(AsyncQuotesFetcher task) throws IOException {
		return this.parsePage(task, "/random.html");
	}
	public DtcItem[] parsePage(AsyncQuotesFetcher task, String uri) throws IOException {
		int foundItems = 0;
		Document document = Jsoup.connect("http://danstonchat.com" + uri).get();
		Elements elements = document.select("div.item");
		DtcItem[] items = new DtcItem[elements.size()];
		for (Element element : elements) {
			if (task.isCancelled()) {
				return new DtcItem[0];
			}
			items[foundItems] = new DtcItem(element);
			foundItems++;
		}
		
		return items;
	}
}
