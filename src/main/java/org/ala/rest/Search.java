package org.ala.rest;

import java.util.ArrayList;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import java.io.File;
import org.apache.lucene.util.Version;
import java.util.List;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Fieldable;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import java.io.IOException;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import java.util.Map;
import java.util.HashMap;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.util.Arrays;

import java.util.logging.Logger;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/***
 *
 * @author angus
 */
@XStreamAlias("search")
public class Search {

	private static final Logger logger = Logging.getLogger("org.ala.rest.Search");
	@XStreamAlias("results")
	ArrayList<SearchResultItem> results;
	@XStreamAlias("xmlns:xlink")
	@XStreamAsAttribute
	String xlink = "http://www.w3.org/1999/xlink";

	/***
	 *
	 * @return a HashMap representation of the resource - which will be serialized into xml/json
	 */
	public Map getMap() {
		HashMap<String, ArrayList<SearchResultItem>> resultsMap = new HashMap<String, ArrayList<SearchResultItem>>();
		resultsMap.put("results", this.results);
		return resultsMap;
	}

	public ArrayList<SearchResultItem> getResults() {
		return this.results;
	}

	/**
	 * If no type is specified, assume that it is a name based search
	 * @param searchTerms
	 * @param layers
	 */
	public Search(String searchTerms, String[] layers) {
		this(searchTerms, layers, "name");
	}

	/**
	 * Searches for a feature within a layer based on name.
	 * @param searchTerms
	 * @param layers
	 * @param type
	 */
	public Search(String searchTerms, String[] layers, String type) {
		results = new ArrayList<SearchResultItem>();
		GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);
		String layerSearch = "";

		if (layers.length > 0) {
			layerSearch = "layerName:(";
			int count = 0;
			for (String layerName : layers) {
				layerSearch += layerName;
				count++;
				if (count < layers.length) {
					layerSearch += " OR ";
				}
			}
			layerSearch += ")";
		}
		logger.finer("layerSearch is " + layerSearch);

		try {
			//Get the geoserver data directory from the geoserver instance
			File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
			IndexSearcher is = new IndexSearcher(FSDirectory.open(file));
			String[] searchFields = {"name", "layerName"};

			if (type.compareTo("id") == 0) {
				searchFields = new String[]{"id", "layerName"};
				logger.finer("searching based on id");
			}

			searchTerms.replace("_", " ");

			MultiFieldQueryParser qp = new MultiFieldQueryParser(Version.LUCENE_CURRENT, searchFields, new StandardAnalyzer(Version.LUCENE_CURRENT));
			qp.setDefaultOperator(qp.AND_OPERATOR);
			Query nameQuery = qp.parse(searchTerms.toLowerCase() + " AND " + layerSearch);


			//TODO: instead of 20 - should be variable and paging?
			TopDocs topDocs = is.search(nameQuery, 20);




			HashMap scoreDocs = new HashMap();
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = is.doc(scoreDoc.doc);
				if (doc.get("name").toLowerCase().startsWith(searchTerms.toLowerCase().replace("*","")))
					scoreDoc.score += 3;
				scoreDocs.put(scoreDoc.score, scoreDoc);
			}

			Object[] keys = scoreDocs.keySet().toArray();
			Arrays.sort(keys);
			for (int i =keys.length -1; i >= 0;i--) {
				ScoreDoc scoreDoc = (ScoreDoc)scoreDocs.get(keys[i]);
				Document doc = is.doc(scoreDoc.doc);
				List<Fieldable> fields = doc.getFields();
				float score = scoreDoc.score;
				results.add(new SearchResultItem(fields, true, score));
			}
			is.close();
		} catch (IOException e1) {
			//FIXME: Log error - return http error code?
			logger.severe("An error occurred in search.");
			logger.severe(ExceptionUtils.getFullStackTrace(e1));
		} catch (ParseException e3) {
			logger.severe("An error occurred parsing search terms.");
			logger.severe(ExceptionUtils.getFullStackTrace(e3));
		}
	}
}
