package com.arbonaut.sampling.design;

import java.io.File;
import java.util.ArrayList;

import com.arbonaut.sampling.SamplePlot;
import com.arbonaut.sampling.io.SampleWriter;
import com.arbonaut.sampling.design.plot.PlotGeometry;
import com.arbonaut.sampling.design.cluster.PlotCluster;
import com.arbonaut.sampling.util.PlotInPolygonChecker;
import com.arbonaut.sampling.util.RasterProcessing;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Location;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;

/**
 * Implements weighted random sampling strategy.
 * 
 */
public class WeightedRandomSample extends SamplingDesignBase {
	
    GridCoverage2D raster;
    
    
    /**
     * Constructor.
     * 
     * @param pgeom   Plot geometry factory defining the shape of each plot     
     * @param cluster Plot cluster instance or null not to generate clustered  
     *                sample                   
     */         
    public WeightedRandomSample(GridCoverage2D raster, PlotGeometry plotGeometry, PlotCluster plotClustering) 
    {
        super(plotGeometry, plotClustering);
        this.raster = raster;
    }
    
    /**
     * Constructor.
     * 
     * @param geoTiff input raster file (geoTiff format)
     * @param pgeom   Plot geometry factory defining the shape of each plot     
     * @param cluster Plot cluster instance or null not to generate clustered  
     *                sample                   
     */         
    public WeightedRandomSample(File geoTiff, PlotGeometry plotGeometry, PlotCluster plotClustering) 
    {
        super(plotGeometry, plotClustering);
        try{
        	this.raster = RasterProcessing.readGeoTiff(geoTiff);
        }catch(Exception e){
        	System.out.println("Could not read GeoTiff file: " + e.toString());        
            e.printStackTrace();
        }
        
    }
    
    
    @Override
    protected void doGenerate(Geometry censusGeometry, 
                              CoordinateReferenceSystem censusCRS,
                              int sampleSize, String stratumName,
                              SampleWriter writer) throws Exception
    {
    	// check if CRS are different
    	CoordinateReferenceSystem crsRaster = this.raster.getCoordinateReferenceSystem();
    	boolean needsReproject = !CRS.equalsIgnoreMetadata(censusCRS, crsRaster);
    	// we need to maintain our original Geometry in a metric CRS so it can be used with plotChecker
    	Geometry clipGeom = censusGeometry; 
    	// use raster CRS
    	if (needsReproject) {
    		MathTransform transform = CRS.findMathTransform(censusCRS, crsRaster, true);
    		// reprojects censusGeometry towards raster CRS -->  don´t know if that is a good idea here
    		clipGeom  = JTS.transform(censusGeometry, transform);
    	}
    	GridCoverage2D clippedCoverage = RasterProcessing.getClippedCoverage(clipGeom, this.raster);
    	double maxValue = RasterProcessing.getCoverageMaxValue(clippedCoverage, 0);
    	double noDataValue = RasterProcessing.getNoDataValue(this.raster, 0);
    	
    	/////////////////////////////////////////////////////////////////////////
    	
    	// variables used in loop
        PlotCluster cluster = this.getPlotClustering();
        PlotInPolygonChecker plotChecker = new PlotInPolygonChecker(
	            (Polygonal)censusGeometry, this.getPlotGeometry());
        
        ////////////////////////////////////////////////////////////////////////
        int numPlots = 0;
    	while (numPlots < sampleSize){
    		 // get random point in geom
    		Coordinate c = createRandCoordInGeom(censusGeometry);
    		 
    		// get raster value at coord position 
    		double plotValue = RasterProcessing.getValueAtPosition(this.raster, c, censusCRS, (double[]) null)[0];
    		// throw exception if plot has nodata value
    		if(plotValue == noDataValue){
    			throw new Exception("Plot does not have a corresponding raster pixel value.\n"
    					+ " Make sure the weight raster completely covers the stratum area.");
    		}
    		// get plot weight 
    		double weight = plotValue / maxValue;
    		// rejection testing: call  simpleRandomSampling() one at a time until desired number of plots is reached
    		boolean keepPlot = RasterProcessing.rejectionTesting(weight);

    		if(keepPlot == false){ //if plot is rejected, sample a new one
    			continue;
    		}else{
    			// keep coord and make plot out of it

    			// Clustered plot
    			if (cluster != null)
    			{
    				CoordinateSequence subplots = cluster.create(c);
    				int subPlotNr = 0;
    				for (int i = 0; i != subplots.size(); i++)
    				{
    					Coordinate c2 = new Coordinate();
    					subplots.getCoordinate(i, c2);
    					
    					// check if sub-plot is still inside census Geometry
    					if(pointInPoly(c2, plotChecker) == true ){
        			    	Geometry plotGeom = this.getPlotGeometry().create(c2);
        			    	SamplePlot plot = 
        			    			new SamplePlot(plotGeom, censusCRS, stratumName, subPlotNr, numPlots, weight);  
        			    	writer.write(plot);
        			    	subPlotNr++;
    					}
    									
    				}
    				// Placed at least 1 plot in the cluster -> increase cluster number
    				// increase numPlots here because one cluster counts as one single plot
    				if (subPlotNr > 0) {numPlots++;}    	
    				
    			}
    			// Single plot
    			else
    			{
    				Geometry plotGeom = this.getPlotGeometry().create(c);
			    	SamplePlot plot = 
			    			new SamplePlot(plotGeom, censusCRS, stratumName, numPlots, weight);  
			    	writer.write(plot);
			    	numPlots++;
    			}

    		}

    	}
    	////////////////////////////////////////////////////////////////////


    	

    }
    
    private Coordinate createRandCoordInGeom(Geometry censusGeometry){
    	Envelope envelope = censusGeometry.getEnvelopeInternal(); 
		double minX = envelope.getMinX();
		double maxX = envelope.getMaxX();
		double minY = envelope.getMinY();
		double maxY = envelope.getMaxY();   
		
		PlotInPolygonChecker plotChecker = new PlotInPolygonChecker(
	            (Polygonal)censusGeometry, this.getPlotGeometry());
		while(true){
    		// generate random X
        	double x = (Math.random() * (maxX - minX)) + minX;
        	// generate random Y
        	double y = (Math.random() * (maxY - minY)) + minY;
    		Coordinate coord = new Coordinate(x, y);
    		int location = plotChecker.centreTest(coord);
    		if (pointInPoly(coord, plotChecker)){
    			return coord;
    		}
    	}
    }
    
    
    public SampleInfo getSampleInfo()
    {
        AreaSampleInfo info = new AreaSampleInfo();
        
        // Todo: fill this
        
        return info;
    }    
    
    
    
    private boolean pointInPoly(Coordinate coord, 
    		PlotInPolygonChecker plotChecker) 
    {
        // Quickly check if this point will do just based on the centre so
        // we could possibly skip creation of the geometry completely 
        int location = plotChecker.centreTest(coord);
        
        // Absolutely outside
        if (location == Location.EXTERIOR) return false;
        
        // Need geometry test
        Geometry plotGeom = null;
        if (location == Location.BOUNDARY)
        {
            plotGeom = this.getPlotGeometry().create(coord);
            // Outside
            if (!plotChecker.contains(plotGeom)) return false;
        }   
        
        // Got so far so we're inside                
        return true;
    }
}
