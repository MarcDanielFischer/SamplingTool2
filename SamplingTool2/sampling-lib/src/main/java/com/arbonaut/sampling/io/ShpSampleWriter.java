package com.arbonaut.sampling.io;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.arbonaut.sampling.SamplePlot;
import com.arbonaut.sampling.design.SamplingDesign;
import com.arbonaut.sampling.design.WeightedRandomSample;
import com.vividsolutions.jts.geom.Geometry;


/**
 * Writes sample plots to a Shapefile.
 */
public class ShpSampleWriter implements SampleWriter {

	private File file;
    private SamplingDesign design;
    private SimpleFeatureType TYPE = null; 
   
    // Attributes we need to construct a Shapefile:
    private ShapefileDataStore dataStore;
    private ArrayList<SimpleFeature> features;
    private SimpleFeatureBuilder featureBuilder;
    
    
     
    /**
     * ShpSampleWriter constructor.
     * @param file    output file
     * @param design  sample design under which the written plots are generated
     */             
    public ShpSampleWriter(File file, SamplingDesign design) 
    		 throws IOException
    {
    	this.file = file;
    	this.design = design;
    	
    	// this.TYPE is not initialized in the constructor because it depends on 
    	// the actual plot Geometry to be written
    	// and is therefore only initialized by the write() method
    	
    	// initialize dataStore
    	// we use DataStoreFactory with a parameter indicating we want a spatial index
    	ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
    	Map<String, Serializable> params = new HashMap<String, Serializable>();
    	params.put("url", this.file.toURI().toURL());
    	params.put("create spatial index", Boolean.TRUE);
    	this.dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
    	
    	this.features = new ArrayList<SimpleFeature>();
    }


    /**
     * Stores a plot in the class´s feature ArrayList property.
     * The actual writing to output file operation is performed
     * by the close() method. This allows for all plots to
     * be written at the same time, so it is not necessary to establish
     * writing access to the output file every time this method is called
     * and transactions can be used without severe performance losses.
     * @param plot
     */
    public void write(SamplePlot plot) throws Exception 
    {
    	/*
    	 * Instead of writing a csv header line, we first
    	 * define a FeatureType for the output Shapefile here. 
    	 * In order to establish this class´s FeatureType property,
    	 * the actual plot Geometry type must be known (POINT, POLYGON,
    	 * or MULTIPOLYGON) which may not be case at the time the
    	 * constructor is called (-> only when there is a plot to write,
    	 * we really know what type its Geometry is).
    	 * This is why this.FeatureType is initialized by the first call
    	 * to this method. 
    	 */
    	
    	// check if FeatureType has been defined already
    	if(this.TYPE == null){
    		this.TYPE = createFeatureType(plot, design);
    		// use createSchema() method to set up the shapefile
        	this.dataStore.createSchema(TYPE); // TYPE is used as a template to describe the file contents
    	}
    	
    	/*
    	 *  Create a feature for each sample plot. 
    	 *  Please note the following:
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
    
    
    /** 
     * Closes this SampleWriter. 
     * This method writes all Features that have been stored in 
     * the class´s feature ArrayList 
     * property during the writing loop to an output Shapefile.
     */
    public void close() throws IOException 
    {
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

    	// Check that we have read-write access by confirming our FeatureSource 
    	// object implements the FeatureStore methods.
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
    }	
}
