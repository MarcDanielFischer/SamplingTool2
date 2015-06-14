
package com.arbonaut.sampling;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import com.vividsolutions.jts.geom.Geometry;


/**
 * Represents a single sample plot. 
 *  
 * Sample plot stores geometry of the plot and a number of attributes such as 
 * the unique plot or cluster numbers, stratum information etc. 
 *
 * The class is immutable and cannot be modified after created. 
 */
public class SamplePlot {
	
    public static final int NO_CLUSTER = -1;
    public static final double NO_WEIGHT = -1;
    
	// properties
    private Geometry geometry;
    private CoordinateReferenceSystem CRS;
    
	private int plotNr = 0;
	private String stratumName = null;
	private int clusterNr = NO_CLUSTER;
	private double weight = NO_WEIGHT;
	

	
	/**
	 * Constructs a SamplePlot object without clusterNr or plotNr.
	 * @param geom        the plot Geometry
	 * @param crs         the CoordinateReferenceSystem associated with the plot Geometry
	 * @param stratumName name of the Stratum the plot lies in
	 */
	public SamplePlot(Geometry geom, CoordinateReferenceSystem crs, String stratumName) {
		this.geometry = geom;
        this.CRS = crs;
		this.stratumName = stratumName;
	}
	
	/**
	 * Constructs a SamplePlot object with a plotNr, but without a clusterNr (for NO_CLUSTER option)
	 * @param geom        the plot Geometry
	 * @param crs         the CoordinateReferenceSystem associated with the plot Geometry
	 * @param stratumName name of the Stratum the plot lies in
	 * @param plotNr      plot number  
	 */
	public SamplePlot(Geometry geom, CoordinateReferenceSystem crs, String stratumName, int plotNr) {
		this(geom, crs, stratumName);
        this.plotNr = plotNr;
	}
	
	/**
	 * Constructs a SamplePlot object with plotNr and clusterNr
	 * @param geom        the plot Geometry
	 * @param crs         the CoordinateReferenceSystem associated with the plot Geometry
	 * @param stratumName name of the Stratum the plot lies in
	 * @param plotNr      plot number  
	 * @param clusterNr   cluster number
	 */
	public SamplePlot(Geometry geom, CoordinateReferenceSystem crs, String stratumName, int plotNr, int clusterNr) {
		this(geom, crs, stratumName, plotNr);
		this.clusterNr = clusterNr;
	}
	
	/**
	 * Constructs a SamplePlot object with plotNr, clusterNr and a weight value 
	 * (for weighted sampling).
	 * @param geom        the plot Geometry
	 * @param crs         the CoordinateReferenceSystem associated with the plot Geometry
	 * @param stratumName name of the Stratum the plot lies in
	 * @param plotNr      plot number  
	 * @param clusterNr   cluster number
	 * @param weight      plot weight
	 */
	public SamplePlot(Geometry geom, CoordinateReferenceSystem crs, 
			String stratumName, int plotNr, int clusterNr, double weight) {
		this(geom, crs, stratumName, plotNr, clusterNr);
		this.weight = weight;
	}
	
	/**
	 * Constructs a SamplePlot object with plotNr and a weight value 
	 * (for weighted sampling without plot clusters).
	 * @param geom        the plot Geometry
	 * @param crs         the CoordinateReferenceSystem associated with the plot Geometry
	 * @param stratumName name of the Stratum the plot lies in
	 * @param plotNr      plot number  
	 * @param weight      plot weight
	 */
	public SamplePlot(Geometry geom, CoordinateReferenceSystem crs, 
			String stratumName, int plotNr, double weight) {
		this(geom, crs, stratumName, plotNr);
		this.weight = weight;
	}
    
    
    /**
	 * Constructs new SamplePlot from existing instance but with geometry replaced.
	 * @param plot     existing plot
	 * @param newgeom  new Geometry to be associated with the plot
	 * @param newcrs   crs to be associated with the new plot Geometry
	 */
	public SamplePlot(SamplePlot plot, Geometry newgeom, CoordinateReferenceSystem newcrs) {
       this.geometry = newgeom;
       this.CRS = newcrs;
	   this.clusterNr = plot.clusterNr;
	   this.plotNr = plot.plotNr;
	   this.stratumName = plot.stratumName;
	   this.weight = plot.weight;
	}
    

	// Getters 
	public Geometry getGeometry() {
		return geometry;
	}

	public CoordinateReferenceSystem getCRS() {
		return CRS;
	}

	public int getClusterNr() {
		return clusterNr;
	}

	public int getPlotNr() {
		return plotNr;
	}

	public String getStratumName() {
		return stratumName;
	}

	public double getWeight() {
		return weight;
	}

}
