/*
 * Classname: ZoneFlow
 * 
 * Copyright (c) 2014 Richard Thomas and University of Leeds
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Artistic License 2.0 as published by the
 * Open Source Initiative (http://opensource.org/licenses/Artistic-2.0)
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package genSynthFlow;

/**
 * OA or WZ flow as extracted from 2011 Census-derived CSV Files
 * For each WZ or OA read in from CSV:
 * 	zoneCode
 * 	eastingPWC
 * 	northingPWC
 * 	distCounts[DIST_INTERVALS]
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
 */
public class ZoneFlow {
	
	// Note: keeping most of these variables public to avoid having to
	// write lots of trivial getVar() accessor functions
	
	/**
	 * Number of "Distance to Work" range intervals used
	 */
	private static final int DIST_INTERVALS = 8;
	
	/**
	 * OA or WZ ONS label (e.g. E33047444)
	 */
	public String zoneCode;
	
	/**
	 * British National Grid "Easting" for Population Weighted Centroid
	 */
	public int eastingPWC;
	
	/**
	 * British National Grid "Northing" for Population Weighted Centroid
	 */
	public int northingPWC;
	
	/**
	 * Count of commuters for each distance range, indexed as:
     *   0: Less than 2km
     *   1: 2km to less than 5km
     *   2: 5km to less than 10km
     *   3: 10km to less than 20km
     *   4: 20km to less than 30km
     *   5: 30km to less than 40km
     *   6: 40km to less than 60km
     *   7: 60km and over
	 */
	public int[] distCounts = new int[DIST_INTERVALS];
	
//-----------------------------------------------------------------------------
	
	/**
	 * Extract + convert fields of interest from each CSV line (Constructor)
	 * 
	 * @param csvTextFields	Raw string arrays of CSV fields
	 * @param codeCsvField	Field index along CSV line for ONS geography code
	 */
	public ZoneFlow(String[] csvTextFields, int codeCsvField) {
		
		// Sanity check that CSV file length is OK
		assert (csvTextFields.length >= codeCsvField + 4 + DIST_INTERVALS) :
			"Not enough fields in CSV";
		
		try {  
			zoneCode = csvTextFields[codeCsvField];
			eastingPWC = Math.round(Float.parseFloat(
					csvTextFields[codeCsvField + 1])); 
			northingPWC = Math.round(Float.parseFloat(
					csvTextFields[codeCsvField + 2]));
			for (int i = 0; i < DIST_INTERVALS; i++) {
				distCounts[i] = Integer.parseInt(
						csvTextFields[codeCsvField + 4 + i]);
			}
		}  
		catch(NumberFormatException nfe) {
			nfe.printStackTrace();
			assert false : "Unexpected CSV text field format";
		}
	}

}
