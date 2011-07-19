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
	public DtcItem[] getLatest() {
		try {
			return this.parsePage("/latest.html");
		}
		catch (IOException e) {
	        return new DtcItem[0];
		}
	}
	public DtcItem[] parsePage(String uri) throws IOException {
		int foundItems = 0;
        Document document = Jsoup.connect("http://danstonchat.com" + uri).get();
		Elements elements = document.select("p.item-content a");
		DtcItem[] items = new DtcItem[elements.size()];
        for (Element element : elements) {
        	items[foundItems] = new DtcItem(element);
        	foundItems++;
        }
		
		return items;
	}
}
