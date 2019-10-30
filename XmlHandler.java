/* JSoloSim
Copyright 2019, Ron Mignery
=================================================================================================================================================================
jsoup License
The jsoup code-base (including source and compiled packages) are distributed under the open source MIT license as described below.

The MIT License
Copyright © 2009 - 2017 Jonathan Hedley (jonathan@hedley.net)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
=================================================================================================================================================================
*/
package jSoloSim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.jsoup.Jsoup;

public class XmlHandler {
	org.jsoup.nodes.Document doc = null;
	public boolean dirty = false;
	ArrayList<String> xmlCategories = new ArrayList<String>();
	ArrayList<String> xmlCategoriesText = new ArrayList<String>();
	ArrayList<String> xmlUsedCategories = new ArrayList<String>();
	ArrayList<String> xmlUsedCategoriesText = new ArrayList<String>();
	File input = new File("local.xml");

	XmlHandler() {
		try {
			doc = Jsoup.parse(input, "UTF-8");
			get_categories();
		} catch (IOException e) {
			doc = Jsoup.parse("<html><head></head><categories></categories><body></body></html>");
			dirty = true;
		}
	}
	
	void get_categories() {
		try {
			org.jsoup.select.Elements cats = doc.getElementsByTag("cat");
			xmlCategories.clear();
			for (org.jsoup.nodes.Element cat : cats) {
				xmlCategories.add(cat.attr("category"));
				xmlCategoriesText.add(Jsoup.parse(cat.attr("category")).text());
			} // endfor links
		} catch (Exception e) {
		}
	}
	
	// Return {category, comment, (clue, response)n}
	public ArrayList<String> get_random_category() {
		ArrayList<String> ret = new ArrayList<String>();
		if (xmlCategories.size() > 0) {
			int i = JSoloSim.rand.nextInt(xmlCategories.size());
			ret.addAll(get_clues_by_category(xmlCategories.get(i)));
			xmlUsedCategories.add(xmlCategories.get(i));
			xmlUsedCategoriesText.add(Jsoup.parse(xmlCategories.get(i)).text());
			xmlCategories.remove(i);
			xmlCategoriesText.remove(i);
		}
		return ret;
	}
	
	public void restore_categories() {
		for (int i = 0; i < xmlUsedCategories.size(); i++) {
			if (!xmlCategories.contains(xmlUsedCategories.get(i))) {
				xmlCategories.add(xmlUsedCategories.get(i));
				xmlCategoriesText.add(Jsoup.parse(xmlUsedCategories.get(i)).text());
				xmlUsedCategories.remove(i);
				xmlUsedCategoriesText.remove(i);
				i = -1;
			}
		}
	}
	
	// Input {category, comment, (clue, response)n}
	public void put_category(ArrayList<String> categoryList) {
		
		// If category not found, add category and clues
		if (!xmlCategoriesText.contains(Jsoup.parse(categoryList.get(0)).text())) {
			xmlCategories.add(categoryList.get(0));
			xmlCategoriesText.add(Jsoup.parse(categoryList.get(0)).text());
			
			org.jsoup.select.Elements els = doc.getElementsByTag("categories");
		    for (org.jsoup.nodes.Element el : els) { // just one
		    	org.jsoup.nodes.Element cat = el.appendElement("cat");
		    	cat.attr("category", categoryList.get(0));
		    	cat.attr("comment", categoryList.get(1));
				for (int i = 2; i < categoryList.size(); i += 2) {
					org.jsoup.nodes.Element clue = cat.appendElement("clue");
					clue.text(categoryList.get(i));
					clue.attr("resp", categoryList.get(i+1));
					cat.appendChild(clue);
				}
				el.appendChild(cat);
				dirty = true;
			}
		}
	
		// Else category already exists, add clues to it
		else {
			org.jsoup.select.Elements cats = doc.getElementsByAttributeValue("category", categoryList.get(0));
		    for (org.jsoup.nodes.Element cat : cats) { // just one
		    	
		    	// Create Jsoup.parse list of categoryList
		    	ArrayList<String> categoryListText = new ArrayList<String>();
		    	for (String s: categoryList) {
		    		categoryListText.add(Jsoup.parse(s).text());
		    	}
		    	
		    	// Check if already copied
		    	org.jsoup.select.Elements clues = cat.getElementsByTag("clue");
			    for (org.jsoup.nodes.Element clue : clues) {
			    	int i = categoryListText.indexOf(Jsoup.parse(clue.text()).text());
		    		if (i > 0) {
		    			
		    			// Search plain text for plain text
		    			int j = categoryListText.indexOf(Jsoup.parse(clue.attr("resp")).text());
		    			if (j == (i+1)) {
		    				categoryList.remove(j);
		    				categoryListText.remove(j);
		    				categoryList.remove(i);
		    				categoryListText.remove(i);
		    			}
		    		}
			    	
			    }
		    	
		    	// For all uncopied clues
				for (int i = 2; i < categoryList.size(); i += 2) {
					org.jsoup.nodes.Element clue = cat.appendElement("clue");
					clue.text(categoryList.get(i));
					clue.attr("resp", categoryList.get(i+1));
					cat.appendChild(clue);
					dirty=true;
				}
		    }
		}
		// Add to doc TBD
	}
	
	public void delete_clues(ArrayList<String> deleteList) {
	    
	    // For the matching category
		org.jsoup.select.Elements cats = doc.getElementsByAttributeValue("category", deleteList.get(0));
	    for (org.jsoup.nodes.Element cat : cats) { // just one
	    	
	    	// Create html2text list of deleteList
	    	ArrayList<String> deleteListText = new ArrayList<String>();
	    	for (String s: deleteList) {
	    		deleteListText.add(Jsoup.parse(s).text());
	    	}
	    	
	    	// Get clues
	    	boolean go = true;
	    	while (go) {
	    		go = false;
		    	org.jsoup.select.Elements clues = cat.getElementsByTag("clue");
			    for (org.jsoup.nodes.Element clue : clues) {
			    	
			    	// If clue found, delete it (plain text in plain text)
			    	int i = deleteListText.indexOf(Jsoup.parse(clue.text()).text());
		    		if (i > 0) {
		    			
		    			// Search plain text for plain text
		    			int j = deleteListText.indexOf(Jsoup.parse(clue.attr("resp")).text());
		    			if (j == (i+1)) {
		    				clue.remove();
		    				dirty = true;
		    				go = true;
		    				break;
		    			}
		    		}
		    	} // endfor
		    } // endwhile
	    } // endfor
	    
	    // For the matching category
	    for (org.jsoup.nodes.Element cat : cats) { // just one
	    	
	    	// If no clues in category, delete category
	    	org.jsoup.select.Elements clues = cat.getElementsByTag("clue");
	    	if (clues.size() == 0) {
	    		cat.remove();
	    		xmlUsedCategories.remove(cat.attr("category"));
	    		dirty=true;
	    	}
	    }  // endfor
	}
	
	private ArrayList<String> get_clues_by_category(String category) {
		ArrayList<String> ret = new ArrayList<String>();
		
		try {
			ret.add(category);
			org.jsoup.select.Elements cats = doc.getElementsByAttributeValue("category", category);
			for (org.jsoup.nodes.Element cat : cats) {
				ret.add(cat.attr("comment"));
				org.jsoup.select.Elements clues = cat.getElementsByTag("clue");
				for (org.jsoup.nodes.Element clue : clues) {
					ret.add(clue.text());
					ret.add(clue.attr("resp"));
				}
			} // endfor links
		} catch (Exception e) {
		}
		return ret;
	}
	
	public void save_xml() {
		if (dirty) {
	        try {
	        	BufferedWriter writer = new BufferedWriter(new FileWriter("local.xml"));
				writer.write(doc.outerHtml());
		        writer.close();
			} catch (IOException e) {
			}
	        
		}
	}
	
}
