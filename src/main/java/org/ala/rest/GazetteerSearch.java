package org.ala.rest;

import java.util.ArrayList;


public class GazetteerSearch {

   ArrayList<SearchResultItem> results;

   public GazetteerSearch( String message ) {
       results = new ArrayList<SearchResultItem>();
       for(int i=0;i<10;i++)
       {
	   results.add(new SearchResultItem(message,i));
       }
   }

 //  public String getName() {
 //     return name;
 //  }
}
