package org.opennms.features.topology.plugins.topo.asset.filter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.opennms.features.topology.api.support.breadcrumbs.BreadcrumbStrategy;
import org.opennms.features.topology.plugins.topo.asset.GeneratorConfig;
import org.opennms.features.topology.plugins.topo.asset.GeneratorConfigBuilder;
import org.opennms.features.topology.plugins.topo.asset.UriParameters;
import org.opennms.features.topology.plugins.topo.asset.layers.NodeParamLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ConfigURITest{
	private static final Logger LOG = LoggerFactory.getLogger(ConfigURITest.class);
	
	@Test
	public void test() {

		String uriString="fred";
		boolean expectedException=false;
		try {
			GeneratorConfig config = new GeneratorConfigBuilder()
			.withUriQueryString(uriString)
			.build();
			
		} catch ( IllegalArgumentException e){
			expectedException=true;
			LOG.debug("    expected IllegalArgumentException thrown="+e.getMessage());
		}
		assertEquals(true,expectedException);
	}
	
	@Test
	public void test2() {

		String uriString="label=Asset Topology Provider&fred=blogs";
		boolean expectedException=false;
		try {
			GeneratorConfig config = new GeneratorConfigBuilder()
			.withUriQueryString(uriString)
			.build();
		} catch ( IllegalArgumentException e){
			expectedException=true;
			LOG.debug("    expected IllegalArgumentException thrown="+e.getMessage());
		}
		assertEquals(true,expectedException);
	}
	
	@Test
	public void test3() {
		
	   String label = "Asset Topology Provider";
	   String breadcrumbStrategy = BreadcrumbStrategy.SHORTEST_PATH_TO_ROOT.name();
	   String providerId = "asset1";
	   String preferredLayout = "Grid Layout";
	   String layers = NodeParamLabels.ASSET_COUNTRY+","+NodeParamLabels.ASSET_CITY+","+NodeParamLabels.ASSET_BUILDING;
	   String filter=NodeParamLabels.ASSET_DISPLAYCATEGORY+"=!testDisplayCategory"
				+ ";"+NodeParamLabels.NODE_FOREIGNSOURCE+"=testForeignSource1,testForeignSource2";
	   
	   String uriString=UriParameters.LABEL+"="+label
			   +"&"+UriParameters.BREADCRUMB_STRATEGY+"="+breadcrumbStrategy
			   +"&"+UriParameters.PROVIDER_ID+"="+providerId
			   +"&"+UriParameters.PREFFERED_LAYOUT+"="+preferredLayout
			   +"&"+UriParameters.ASSET_LAYERS+"="+layers
			   +"&"+UriParameters.FILTER+"="+filter;
	   
	   LOG.debug("    uriString="+uriString);
	   
		boolean expectedException=false;
		try {
			
			//check config builds from uri string
			GeneratorConfig config = new GeneratorConfigBuilder()
			.withUriQueryString(uriString)
			.build();
			
			//check values
			assertEquals(label,config.getLabel());
			assertEquals(breadcrumbStrategy,config.getBreadcrumbStrategy());
			assertEquals(providerId,config.getProviderId());
			assertEquals(preferredLayout,config.getPreferredLayout());
			
			List<String> layerHeirarchies = Arrays.asList(NodeParamLabels.ASSET_COUNTRY,NodeParamLabels.ASSET_CITY,NodeParamLabels.ASSET_BUILDING);
			assertEquals(config.getLayerHierarchies(),layerHeirarchies );
			
			List<String> filters =  Arrays.asList(NodeParamLabels.ASSET_DISPLAYCATEGORY+"=!testDisplayCategory",
					NodeParamLabels.NODE_FOREIGNSOURCE+"=testForeignSource1,testForeignSource2");
					
			assertEquals(config.getFilters(),filters);
			
			// generate new uri string from config
			String uriOut=GeneratorConfigBuilder.toUriString(config);
			LOG.debug("    uriOut="+uriOut);
			
			GeneratorConfig config2 = new GeneratorConfigBuilder()
			.withUriQueryString(uriOut)
			.build();
			
			assertTrue(config.equals(config2));
			
		} catch ( IllegalArgumentException e){
			expectedException=true;
			LOG.debug("    IllegalArgumentException thrown="+e.getMessage());
		}
		assertEquals(false,expectedException);
	}
	


}
