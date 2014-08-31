/*
 * Classname: ZoneFlowList
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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Array<List> of ZoneFlow (2 instances: OAs, WZs)
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
 */
public class ZoneFlowList {
	
	/**
	 * List of flows to read the CSV file into
	 */
	public ArrayList<ZoneFlow> flows = new ArrayList<ZoneFlow>();
	
	/**
	 * Flow count for (currently) selected distance for each zone
	 */
	private int[] selectedDistanceCount;
	
//-----------------------------------------------------------------------------

	/**
	 * (Constructor) Parse a OA/WZ CSV file to create a list. Each record
	 * (element of the list) will hold data of interest in an appropriate
	 * variable type.
	 * 
	 * @param csvFileName	File name of CSV file to parse
	 * @param codeCsvField	Field index along CSV line for ONS geography code
	 * @throws FileNotFoundException	Unable to open specified input CSV files
	 * @throws IOException				Error reading specified input CSV files
	 */
	public ZoneFlowList(String csvFileName,
			int codeCsvField) throws FileNotFoundException, IOException {

		System.out.println("Parsing file: " + csvFileName);

		// CSV File Readers (openCSV library)
		CSVReader csvReader = null;

		// Read in CSV file of WZ or OA commuting totals (by distance)
		try {
			csvReader = new CSVReader(new FileReader(csvFileName));
		} catch (FileNotFoundException e) {
			
			// Major failure in reading file: bail out
			throw e;
		}

		try {

			// Read header line of CSV file + sanity checks
			String [] nextLine = csvReader.readNext();
			assert (nextLine != null) : "CSV file is empty";
			System.out.println("Fieldlist headings from CSV file:");
			for (int i = 0; i < nextLine.length; i++) {
				System.out.printf("    %2d: %s%n", i, nextLine[i]);
			}
			System.out.printf("Check expected ZoneCode (field %d): \"%s\"%n",
					codeCsvField, nextLine[codeCsvField]);
			System.out.printf(
					"Check expected PWC XY (fields %d,%d): (\"%s\",\"%s\")%n",
					codeCsvField + 1, codeCsvField + 2,
					nextLine[codeCsvField + 1], nextLine[codeCsvField + 2]);
			System.out.printf("Check expected \"< 2km\" (field %d): \"%s\"%n%n",
					codeCsvField + 4, nextLine[codeCsvField + 4]);

			// Parse rest of file one line at a time
			while ((nextLine = csvReader.readNext()) != null) {
				flows.add(new ZoneFlow(nextLine, codeCsvField));
			}
		} catch (IOException e) {
			
			// Major failure in reading file: bail out
			throw e;
		}

		// Close CSV file
		try {
			csvReader.close();
		} catch (IOException e) {

			// Print warning, but stumble on anyway
			e.printStackTrace();
		}
	}

//-----------------------------------------------------------------------------

	/**
	 * Select distance interval flow counts to use from now on.
	 * (Simplify things up by extracting these into a private array)
	 * 
	 * @param distanceSelect	Required distance interval (e.g. 0: 0-2km)
	 * @return					Total flow counts for this distance range
	 */
	public int setDistance(int distanceSelect){

		int totalFlows = 0;
		int countFlows = flows.size();

		// Create array of selected distance counts
		selectedDistanceCount = new int[countFlows];
		
		// For each zone, extract the required count
		for (int i = 0; i < countFlows; i++) {
			selectedDistanceCount[i] = flows.get(i).distCounts[distanceSelect];
			totalFlows += selectedDistanceCount[i];
			
			// If running low on memory, could prompt for garbage collection to
			// free up rest of distCounts array at this point
		}

		return totalFlows;
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Get flow count for currently selected distance interval for a single
	 * zone (OA or WZ)
	 * 
	 * @param zoneIndex		Which OA (or WZ)
	 * @return				Flow count
	 */
	public int getCount(int zoneIndex) {
		return selectedDistanceCount[zoneIndex];
	}

}
