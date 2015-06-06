package com.arbonaut.sampling.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.arbonaut.sampling.SamplePlot;
import com.arbonaut.sampling.design.SamplingDesign;
import com.arbonaut.sampling.design.WeightedRandomSample;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;


/**
 * Writes sample plots to a Shapefile.
 * 
 */
public class ShpSampleWriter implements SampleWriter {

	private File file;
    private SamplingDesign design;
    private SimpleFeatureType TYPE = null; 
    private ShapefileDataStore dataStore;
	// A list to collect features as we create them.
    private ArrayList<SimpleFeature> features;
    private SimpleFeatureBuilder featureBuilder;
    
    
     
    /**
     * CsvSampleWriter constructor.
     * 
     * @param file    Output file
     * @param design  Sample design under which the written plots are generated
     * @param writeXYColumns If true, the CSV file will have 'X' and 'Y' columns
     *                       with the plot coordinates. Otherwise 'Geom' column
     *                       will be written with full geometry description in 
     *                       WKT format.                                 
     */             
    public ShpSampleWriter(File file, SamplingDesign design) 
    		 throws IOException
    {
    	this.file = file;
    	this.design = design;
    	
    	// this.TYPE is not initialized in the constructor because it depends on the actual plot Geometry
    	// and is therefore called by the write() method
    	
    	// initialize dataStore
    	// we use DataStoreFactory with a parameter indicating we want a spatial index
    	ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
    	Map<String, Serializable> params = new HashMap<String, Serializable>();
    	params.put("url", this.file.toURI().toURL());
    	params.put("create spatial index", Boolean.TRUE);
    	this.dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
    	
    	this.features = new ArrayList<SimpleFeature>();
    }


    public void write(SamplePlot plot) throws Exception 
    {
    	// Problem: FeatureType must know the final plot Geometry type (POINT, POLYGON, MULTIPOLY...),
    	// but when constructor is called, this is not yet known 
    	// --> only when the plot is there, we really know what type ists Geometry is
    	// so it really only maked sense to construct the type when we do have a plot to write
    	// further complication: write plots 1 by 1 -> my example code writes all features at once
    	// -> means leaving the file open and not closing it (eg no commit)
    	// create FeatureType instead of csv header line
    	
    	// check if FeatureType has been defined already
    	if(this.TYPE == null){
    		this.TYPE = createFeatureType(plot, design);
    		// use createSchema( SimpleFeatureType ) method to set up the shapefile
        	this.dataStore.createSchema(TYPE); // TYPE is used as a template to describe the file contents
    	}
    	
    	// create a feature for each sample plot. Please note the following:
    	// Creation of features (SimpleFeature objects) using SimpleFeatureBuilder

    	/*
    	 *  If you have used previous versions of GeoTools you might be used 
    	 *  to creating a new FeatureCollection and using the add method 
    	 *  to accumulate features. This usage has now been deprecated and 
    	 *  we encourage you to treat FeatureCollections as immutable views or result sets.
    	 */
    	
    	if(this.featureBuilder == null){
    		this.featureBuilder = new SimpleFeatureBuilder(TYPE);
    	}
    	
    	Geometry geom = plot.getGeometry();
    	this.featureBuilder.add(geom);
    	this.featureBuilder.add(plot.getPlotNr());
    	if (design.getPlotClustering() != null) {
    		this.featureBuilder.add(plot.getClusterNr());
    	} 

    	if (this.design instanceof WeightedRandomSample) {
    		this.featureBuilder.add(plot.getWeight());
    	} 
    	this.featureBuilder.add(plot.getStratumName());	

    	SimpleFeature feature = this.featureBuilder.buildFeature(null);
    	this.features.add(feature);
    }

  

    private static SimpleFeatureType createFeatureType(SamplePlot plot, SamplingDesign design)throws Exception {

    	SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    	builder.setName("Plot");
    	
    	builder.setCRS(plot.getCRS());

    	// add attributes in order
        // Geom can be either Point or Polygon, depending on PlotGeometry and Filter
        builder.add("the_geom", plot.getGeometry().getClass());
        
        builder.add("Plot_nr",Integer.class);
        // conditionally add attributes "Cluster_nr" and "weight"
        if (design.getPlotClustering() != null) {
        	builder.add("Cluster_nr",Integer.class);
        } 
      
        if (design instanceof WeightedRandomSample) {
        	builder.add("weight",Double.class);
        } 
        	
        builder.add("Stratum",String.class);
        // build the type
        final SimpleFeatureType PLOT = builder.buildFeatureType();

        return PLOT;
    }
    
    
    public void close() throws IOException 
    {
    	// write all Features at once when the writing loop is over

    	/////////////////////////////////////////////////////////////////////////////////////////////////////////
    	//Write the feature data to the shapefile
    	// We check that we have read-write access by confirming our FeatureSource object implements the FeatureStore methods
    	// Take a moment to check how closely the shapefile was able to match 
    	//our template (the SimpleFeatureType TYPE). Compare this output to see how they are different.
    	// The SimpleFeatureStore that we use to do this expects a 
    	// FeatureCollection object, so we wrap our list of features in a ListFeatureCollection.
    	// The use of transaction commit() to safely write out the features in one go.
    	/*
    	 * Write the features to the shapefile
    	 */
    	Transaction transaction = new DefaultTransaction("create");

    	String typeName = this.dataStore.getTypeNames()[0];
    	SimpleFeatureSource featureSource = this.dataStore.getFeatureSource(typeName);
    	SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
    	/*
    	 * The Shapefile format has a couple limitations:
    	 * - "the_geom" is always first, and used for the geometry attribute name
    	 * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
    	 * - Attribute names are limited in length 
    	 * - Not all data types are supported (example Timestamp represented as Date)
    	 * 
    	 * Each data store has different limitations so check the resulting SimpleFeatureType.
    	 */
    	System.out.println("SHAPE:"+SHAPE_TYPE);

    	if (featureSource instanceof SimpleFeatureStore) {
    		SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
    		/*
    		 * SimpleFeatureStore has a method to add features from a
    		 * SimpleFeatureCollection object, so we use the ListFeatureCollection
    		 * class to wrap our list of features.
    		 */
    		SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
    		featureStore.setTransaction(transaction);
    		try {
    			featureStore.addFeatures(collection);
    			transaction.commit();
    		} catch (Exception problem) {
    			problem.printStackTrace();
    			transaction.rollback();
    		} finally {
    			transaction.close();
    		}
    	} else {
    		throw new IOException(typeName + " does not support read/write access");
    	}
    	/////////////////////////////////////////////////////////////////////////////////////////////////////////
    }	

}
