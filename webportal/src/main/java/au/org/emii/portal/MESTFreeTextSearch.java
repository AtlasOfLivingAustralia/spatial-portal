/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal;

/**
 *
 * @author Brendon
 */
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.StringCharacterIterator;
import java.util.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

public class MESTFreeTextSearch {

	private String SearchURL;
	private String SearchParameter;
	private Document SearchResult;
	private Document SearchFilter;
	protected int MaxRecords;
	private String SearchKVP;
	// private List<Theme> ThemeList = new ArrayList<Theme>();
	private List<WMSLayer> WMSList = new ArrayList<WMSLayer>();
	// private List<Service> ServiceList = new ArrayList<Service>();
	private List<FeatureType> FeatureTypeList = new ArrayList<FeatureType>();
	private List<DatasetDescription> DatasetDescriptionList = new ArrayList<DatasetDescription>();
	private HttpClient client = new HttpClient();
	private int resultCount = 0;
	private List<Dataset> datasets = new ArrayList<Dataset>();
	private Logger logger = Logger.getLogger(this.getClass());
	private Double east;
	private Double west;
	private boolean useDate = false;
	private boolean useBBOX = false;
	private List<String> UuidList = new ArrayList<String>();

	public List<String> getUuidList() {
		return UuidList;
	}

	public void setUuidList(List<String> uuidList) {
		UuidList = uuidList;
	}

	public boolean isUseBBOX() {
		return useBBOX;
	}

	public void setUseBBOX(boolean useBBOX) {
		this.useBBOX = useBBOX;
	}

	public boolean isUseDate() {
		return useDate;
	}

	public void setUseDate(boolean useDate) {
		this.useDate = useDate;
	}

	public Double getEast() {
		return east;
	}

	public void setEast(Double east) {
		this.east = east;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Double getNorth() {
		return north;
	}

	public void setNorth(Double north) {
		this.north = north;
	}

	public Double getSouth() {
		return south;
	}

	public void setSouth(Double south) {
		this.south = south;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Double getWest() {
		return west;
	}

	public void setWest(Double west) {
		this.west = west;
	}

	private Double north;
	private Double south;
	private Date startDate;
	private Date endDate;

	public List<Dataset> getDatasets() {
		return datasets;
	}

	public void setDatasets(List<Dataset> datasets) {
		this.datasets = datasets;
	}

	public int getResultCount() {
		return resultCount;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	public List<WMSLayer> getWMSList() {
		return WMSList;
	}

	public List<FeatureType> getFeatureTypeList() {
		return FeatureTypeList;
	}

	public List<DatasetDescription> getDatasetDescriptionList() {
		return DatasetDescriptionList;
	}

	public void setWMSList(List<WMSLayer> wmsLayers) {
		WMSList = wmsLayers;
	}

	public String getSearchURL() {
		return SearchURL;
	}

	public void setSearchURL(String searchURL) {
		SearchURL = searchURL;
	}

	public String getSearchParameter() {
		return SearchParameter;
	}

	public void setSearchParameter(String searchParameter) {
		SearchParameter = searchParameter;
	}

	/**
	 * Get the value of SearchFilter
	 * 
	 * @return the value of SearchFilter
	 */
	public Document getSearchFilter() {
		return SearchFilter;
	}

	/**
	 * Set the value of SearchFilter
	 * 
	 * @param SearchFilter
	 *            new value of SearchFilter
	 */
	public void setSearchFilter(Document SearchFilter) {
		this.SearchFilter = SearchFilter;
	}

	public int getMaxRecords() {
		return MaxRecords;
	}

	public Document getSearchResult() {
		return SearchResult;
	}

	public void setSearchResult(Document searchResult) {
		SearchResult = searchResult;
	}

	public String getSearchKVP() {
		return SearchKVP;
	}

	public void setSearchKVP(String searchKVP) {
		SearchKVP = searchKVP;
	}

	public boolean RunSearch() {

		boolean resultSuccess = false;
		// LoadFilter();
		// check what type of search it is

		try {
			// if its free text only load the filter and use csw
			// it its complex use the kvp stuff
			String s = "xml.search?any=";
			DateFormat sdf = Validate.getShortIsoDateFormatter();
			
			//SearchParameter = forHTML(SearchParameter);
			SearchParameter = URLEncoder.encode(SearchParameter, "UTF-8");

			if (useBBOX) {
				if (useDate) {
					s = s	+ SearchParameter
							+ "&attrset=geo&northBL="
							+ north.toString()
							+ "&eastBL="
							+ east.toString()
							+ "&southBL="
							+ south.toString()
							+ "&westBL="
							+ west.toString();
					
							if (startDate != null) {
								s = s + "&dateFrom=" + sdf.format(startDate);
							}
					
							if (endDate != null) {
								s = s + "&dateTo=" + sdf.format(endDate);
							}
							s = s + "&relation=overlaps&region=userdefined&sortBy=relevance&output=text&hitsPerPage=25";

				} else {
					s = s   + SearchParameter
							+ "&attrset=geo&northBL="
							+ north.toString()
							+ "&eastBL="
							+ east.toString()
							+ "&southBL="
							+ south.toString()
							+ "&westBL="
							+ west.toString()
							+ "&relation=overlaps&region=userdefined&sortBy=relevance&output=text&hitsPerPage=25";

				}
			} else {
				if (useDate) {
					s = s + SearchParameter;
					
					if (startDate != null) {
						s = s + "&dateFrom=" + sdf.format(startDate);
					}
					
					if (endDate != null) {
						s = s + "&dateTo=" + sdf.format(endDate);
					}
					
					s = s + "&sortBy=relevance&output=text";
				} else {
					s = s + SearchParameter;
				}
			}

			String feed = SearchURL + "/" + s;
			SearchKVP = feed;   
			SearchResult = getDocument(SearchKVP, false);

		} catch (Exception e) {
			resultSuccess = false;
		}

		return resultSuccess;
	}

	@SuppressWarnings("unchecked")
	public boolean ParseResult() {

		boolean bResult = true;
		Document doc = getSearchResult();

		NodeList nCount = doc.getElementsByTagName("summary");
		Node ns = nCount.item(0);

		NamedNodeMap attributes = ns.getAttributes();

		Attr attr = (Attr) attributes.item(0);

		resultCount = Integer.parseInt(attr.getValue());

		UuidList.clear();

		try {

			NodeList nu = doc.getElementsByTagName("uuid");

			// now for each record get the uuid

			for (int i = 0; i < nu.getLength(); i++) {
				Node n = nu.item(i);
				UuidList.add(n.getTextContent());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}

		if (UuidList.size() > 10) {
			// just get the first ten
			GetResultFromTo(1, 10);
		} else {
			if (UuidList.size() < 1) {
				// dont get any
			} else {
				GetResultFromTo(1, UuidList.size());
			}
		}

		return bResult;
	}

	public boolean GetResultFromTo(int from, int to) {

		boolean bCheck = false;
		String getString = SearchURL + "/xml.metadata.get?uuid=";
		String s = "";
		Document d = null;
		datasets.clear();

		if (to > resultCount) {
			to = resultCount;
		}

		for (int i = from - 1; i < to; i++) {
			String sn = UuidList.get(i);
			s = getString + sn;
                	d = getDocument(s, false);
			parseRecord(d, sn);

		}

		return bCheck;

	}

	public void parseRecord(Document d, String uuid) {
		try {
			Dataset nd = new Dataset();
                        Node nl = null;
                        NodeList nodes = null;


			// get the title abstract and set the uuid

                        try {
			nl = d.getElementsByTagName("gmd:title").item(0);
		        nodes = nl.getChildNodes();
                        } catch (NullPointerException npe) {
                            //just ignore it
                        }

			nd.setMestrecord("/metadata.show?uuid=" + uuid);


                        try {
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().equalsIgnoreCase(
						"gco:CharacterString")) {
					nd.setTitle(nodes.item(i).getTextContent());
				}
			}


                        } catch (NullPointerException npx) {
                            //just ignore it
                        }

                         try {

			// get the title abstract and set the uuid
			Node na = d.getElementsByTagName("gmd:abstract").item(0);
			NodeList nodesA = na.getChildNodes();

			for (int i = 0; i < nodesA.getLength(); i++) {
				if (nodesA.item(i).getNodeName().equalsIgnoreCase(
						"gco:CharacterString")) {
					nd.setAbstract(nodesA.item(i).getTextContent());
				}
			}

                        } catch (NullPointerException npx) {
                            //just ignore it
                        }


			nd.setIdentifier(uuid);

			datasets.add(nd);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.error(e1 + uuid);
		}

	}

	public Document getDocument(String feed, boolean RemoveFluff) {
		Document d = null;
		HttpURLConnection connection = null;
		OutputStreamWriter wr = null;
		BufferedReader rd = null;
		StringBuilder sb = null;
		String line = null;

		URL serverAddress = null;
		try {

			serverAddress = new URL(feed);

			// set up out communications stuff
			connection = null;

			// Set up the initial connection
			connection = (HttpURLConnection) serverAddress.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(60000);

			connection.connect();

			rd = new BufferedReader(new InputStreamReader(connection
					.getInputStream()));
			sb = new StringBuilder();

			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}

			// want to get rid of all the superflous stuff

			String document = sb.toString();

			if (RemoveFluff) {
				int istart = document.indexOf("<response");
				int iend = document.indexOf("</response>");
				document = document.substring(istart, iend + 11);
			}

			// now turn it into a document

			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory
					.newInstance();
			javax.xml.parsers.DocumentBuilder db = factory.newDocumentBuilder();
			org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();

			inStream.setCharacterStream(new java.io.StringReader(document));
			d = db.parse(inStream);

			connection.disconnect();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}

		return d;
	}

	public static String forHTML(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(
				aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>') {
				result.append("&gt;");
			} else if (character == '&') {
				result.append("&amp;");
			} else if (character == '\"') {
				result.append("&quot;");
			} else if (character == '\t') {
				addCharEntity(9, result);
			} else if (character == '!') {
				addCharEntity(33, result);
			} else if (character == '#') {
				addCharEntity(35, result);
			} else if (character == '$') {
				addCharEntity(36, result);
			} else if (character == '%') {
				addCharEntity(37, result);
			} else if (character == '\'') {
				addCharEntity(39, result);
			} else if (character == '(') {
				addCharEntity(40, result);
			} else if (character == ')') {
				addCharEntity(41, result);
			} else if (character == '*') {
				addCharEntity(42, result);
			} else if (character == '+') {
				addCharEntity(43, result);
			} else if (character == ',') {
				addCharEntity(44, result);
			} else if (character == '-') {
				addCharEntity(45, result);
			} else if (character == '.') {
				addCharEntity(46, result);
			} else if (character == '/') {
				addCharEntity(47, result);
			} else if (character == ':') {
				addCharEntity(58, result);
			} else if (character == ';') {
				addCharEntity(59, result);
			} else if (character == '=') {
				addCharEntity(61, result);
			} else if (character == '?') {
				addCharEntity(63, result);
			} else if (character == '@') {
				addCharEntity(64, result);
			} else if (character == '[') {
				addCharEntity(91, result);
			} else if (character == '\\') {
				addCharEntity(92, result);
			} else if (character == ']') {
				addCharEntity(93, result);
			} else if (character == '^') {
				addCharEntity(94, result);
			} else if (character == '_') {
				addCharEntity(95, result);
			} else if (character == '`') {
				addCharEntity(96, result);
			} else if (character == '{') {
				addCharEntity(123, result);
			} else if (character == '|') {
				addCharEntity(124, result);
			} else if (character == '}') {
				addCharEntity(125, result);
			} else if (character == '~') {
				addCharEntity(126, result);
			} else if (character == ' ') {
				addCharEntity(20, result);
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

	private static void addCharEntity(Integer aIdx, StringBuilder aBuilder) {
		String padding = "";
		if (aIdx <= 9) {
			padding = "00";
		} else if (aIdx <= 99) {
			padding = "0";
		} else {
			// no prefix
		}
		String number = padding + aIdx.toString();
		aBuilder.append("&#" + number + ";");
	}

}
