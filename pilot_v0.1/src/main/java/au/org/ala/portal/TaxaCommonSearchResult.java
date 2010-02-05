package au.org.ala.portal;

public class TaxaCommonSearchResult {
	
	private Double score;
	private String commonName;
	private String commonNameUrl;
	private String scientificName;
	private String rank;
	private String family;
	private String kingdom;
	private int occurrenceCount;
	private int occurrenceCoordinateCount;
	private String result;
	

	public String getCommonNameUrl() {
		return commonNameUrl;
	}
	public void setCommonNameUrl(String commonNameUrl) {
		this.commonNameUrl = commonNameUrl;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
	public String getCommonName() {
		return commonName;
	}
	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}
	public String getScientificName() {
		return scientificName;
	}
	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}
	public String getRank() {
		return rank;
	}
	public void setRank(String rank) {
		this.rank = rank;
	}
	public String getFamily() {
		return family;
	}
	public void setFamily(String family) {
		this.family = family;
	}
	public String getKingdom() {
		return kingdom;
	}
	public void setKingdom(String kingdom) {
		this.kingdom = kingdom;
	}
	public int getOccurrenceCount() {
		return occurrenceCount;
	}
	public void setOccurrenceCount(int occurrenceCount) {
		this.occurrenceCount = occurrenceCount;
	}
	public int getOccurrenceCoordinateCount() {
		return occurrenceCoordinateCount;
	}
	public void setOccurrenceCoordinateCount(int occurrenceCoordinateCount) {
		this.occurrenceCoordinateCount = occurrenceCoordinateCount;
	}

}
