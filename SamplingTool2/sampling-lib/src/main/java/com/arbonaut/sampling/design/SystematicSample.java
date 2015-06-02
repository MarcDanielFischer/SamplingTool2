package com.arbonaut.sampling.design;

import java.util.ArrayList;

import com.arbonaut.sampling.SamplePlot;
import com.arbonaut.sampling.io.SampleWriter;
import com.arbonaut.sampling.design.plot.PlotGeometry;
import com.arbonaut.sampling.design.cluster.PlotCluster;
import com.arbonaut.sampling.util.PlotInPolygonChecker;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Location;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.geotools.geometry.jts.JTSFactoryFinder;


/**
 * Implements systematic sampling strategy.
 * 
 */
public class SystematicSample extends SamplingDesignBase {
	
    private double xDistance;
    private double yDistance;
    Coordinate startPoint = null;
    
    
    /**
     * Constructor for SystematicSample without a specified starting point.
     * 
     * @param xDistance grid point distance in x direction
     * @param yDistance grid point distance in y direction
     * @param plotGeometry Plot geometry factory defining the shape of each plot   
     * @param plotClustering Plot cluster instance or null not to generate clustered  
     */
    public SystematicSample(double xDistance, double yDistance, PlotGeometry plotGeometry, PlotCluster plotClustering) 
    {
        super(plotGeometry, plotClustering);
        this.xDistance = xDistance;
        this.yDistance = yDistance;
    }
    
    /**
     * Constructor for SystematicSample with a specified starting point.
     * 
     * @param xDistance grid point distance in x direction
     * @param yDistance grid point distance in y direction
     * @param startPoint start point for the construction of the grid
     * @param plotGeometry Plot geometry factory defining the shape of each plot   
     * @param plotClustering Plot cluster instance or null not to generate clustered  
     */
    public SystematicSample(double xDistance, double yDistance, Coordinate startPoint, PlotGeometry plotGeometry, PlotCluster plotClustering) 
    {
        this(xDistance, yDistance, plotGeometry, plotClustering);
        this.startPoint = startPoint;  
    }
    

    @Override
    protected void doGenerate(Geometry censusGeometry, 
                              CoordinateReferenceSystem censusCRS,
                              int sampleSize, String stratumName,
                              SampleWriter writer) throws Exception
    {
    	PlotInPolygonChecker plotChecker = new PlotInPolygonChecker(
  				(Polygonal)censusGeometry, this.getPlotGeometry());
    	Coordinate[] coords = createGridCoords(censusGeometry, plotChecker);
        PlotCluster cluster = this.getPlotClustering();
        Coordinate c = new Coordinate();
        
        // iterate over coords[], optionally create clusters and sample plots
        int numPlots = 0;
        for(int i = 0; i < coords.length; i++)
        {
            // Clustered plot
            if (cluster != null)
            {
                CoordinateSequence subplots = cluster.create(coords[i]);
                int subPlotNr = 0;
                for (int k = 0; k != subplots.size(); k++)
                {
                    subplots.getCoordinate(k, c);
                    boolean added = sampleSinglePoint(
                        c, censusCRS, subPlotNr, numPlots, stratumName, 
                        writer, plotChecker);
                         
                    if (added) {++subPlotNr;}                    
                }
                
                // Placed at least 1 plot in the cluster -> increase cluster number
                // increase numPlots here because one cluster counts as one single plot
                if (subPlotNr > 0) {++numPlots;}
            }
            // Single plot
            else
            {
                boolean added = sampleSinglePoint(
                	coords[i], censusCRS, numPlots, SamplePlot.NO_CLUSTER, stratumName, 
                    writer, plotChecker); 
                if (added) { numPlots ++;}  
            }
		}
      
    }
    
    private Coordinate[] createGridCoords(Geometry censusGeometry, PlotInPolygonChecker plotChecker ) throws Exception{
        // Bounding Box min/max values
      	Envelope envelope = censusGeometry.getEnvelopeInternal(); 
  		double minX = envelope.getMinX();
  		double maxX = envelope.getMaxX();
  		double minY = envelope.getMinY();
  		double maxY = envelope.getMaxY();    

  		if(this.startPoint == null){
  			this.startPoint = createStartPoint(envelope, plotChecker);
  		}
  		
  		// determine numPoints in all directions (seen from the starting point, how many points are there in each direction until reaching the edges of the boundingBox)
  		double xStart = this.startPoint.x;
  		double yStart = this.startPoint.y;
  		int numPointsLeft = (int)Math.floor((xStart - minX) / this.xDistance); 
  		int numPointsRight = (int)Math.floor((maxX - xStart) / this.xDistance); 
  		int numPointsUp = (int)Math.floor((maxY -yStart) / this.yDistance); 
  		int numPointsDown = (int)Math.floor((yStart - minY) / this.yDistance); 
  		
  		// calculate totals: numPointsHorizontal (number of points in one grid row covering the whole width of the bounding box) and numPointsVertical
  		// (number of points in one grid column covering the whole length of the bounding box)
  		int numPointsHorizontal = numPointsLeft + numPointsRight + 1; // numPoints to the right and left of the starting point + starting point itself
  		int numPointsVertical = numPointsUp + numPointsDown + 1; // numPoints up and down + starting point itself
  		// calculate total number of grid-plots 
  		int numPlots = numPointsHorizontal * numPointsVertical;
  		
  		// calculate grid`s upper left corner coord where sampling process is started
  		double x = xStart - (this.xDistance * numPointsLeft);
  		double y = yStart + (this.yDistance * numPointsUp);
      	
      	Coordinate[] coords = new Coordinate[numPlots];
      	int n = 0; // index for coords[]
  		
  		// create all grid coords using two nested for loops
      	// starting from upper left corner of the grid, from top to bottom (same order as when reading a page of text) 
  		for(int i = 0; i < numPointsVertical; i++ ){
  			 for(int k = 0; k < numPointsHorizontal; k++){
  				 coords[n] = new Coordinate( x, y );
  				 n++;
  				 x += this.xDistance;
  			 }
  			 // reset x to its lefternmost value ("carriage return")
  			 x = xStart - (this.xDistance * numPointsLeft);
  			 
  			 // shift y coord one row down
  			 y -= this.yDistance;
  		 }
  		return coords;
    }
    
    
    private Coordinate createStartPoint(Envelope envelope, PlotInPolygonChecker plotChecker)throws Exception{
    	double minX = envelope.getMinX();
    	double maxX = envelope.getMaxX();
    	double minY = envelope.getMinY();
    	double maxY = envelope.getMaxY();    
    	
    	while(true){
    		// generate random X
        	double x = (Math.random() * (maxX - minX)) + minX;
        	// generate random Y
        	double y = (Math.random() * (maxY - minY)) + minY;
    		Coordinate coord = new Coordinate(x, y);
    		int location = plotChecker.centreTest(coord);
    		if (location == Location.INTERIOR){
    			return coord;
    		}
    	}
    }
    
    
    
    private boolean sampleSinglePoint(Coordinate coord, CoordinateReferenceSystem crs,
                                      int plotNr, int clusterNr,
                                      String stratumName, SampleWriter writer,
                                      PlotInPolygonChecker plotChecker) throws Exception
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
        if (plotGeom == null)
            plotGeom = this.getPlotGeometry().create(coord);
        
        SamplePlot plot = 
            new SamplePlot(plotGeom, crs, stratumName, plotNr, clusterNr);  
        writer.write(plot);
        
        return true;
    }
	


    public SampleInfo getSampleInfo()
    {
    	AreaSampleInfo info = new AreaSampleInfo();

    	// Todo: fill this

    	return info;
    }    

}