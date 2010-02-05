package au.org.ala.portal;

/*
 
 */

/**
 * @author brendon
 *Example of the JSON return for a result
 	"score": 12.76156, 
	"scientificNameId": 2118810, 
	"scientificName": "Squalus", 
	"scientificNameUrl": "/species/2118810", 
	"author": "", 
	"rank": "genus", 
	"family": "Squalidae", 
	"kingdom": "Animalia", 
	"occurrenceCount": 2022, 
	"occurrenceCoordinateCount": 2005  
 */
public class TaxaScientificSearchResult {
	
	private Double score;
	private int scientificNameId;
	private String scientificName;
	private String scientificNameUrl;
	private String author;
	private String rank;
	private String family;
	private String kingdom;
	private int occurrenceCount;
	private int occurrenceCoordinateCount;
	
	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
	public int getScientificNameId() {
		return scientificNameId;
	}
	public void setScientificNameId(int scientificNameId) {
		this.scientificNameId = scientificNameId;
	}
	public String getScientificName() {
		return scientificName;
	}
	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}
	public String getScientificNameUrl() {
		return scientificNameUrl;
	}
	public void setScientificNameUrl(String scientificNameUrl) {
		this.scientificNameUrl = scientificNameUrl;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
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
