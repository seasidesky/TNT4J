/*
 * Copyright 2014-2015 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.tnt4j.source;

import java.util.Map;
import java.util.StringTokenizer;

import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.config.Configurable;
import com.jkoolcloud.tnt4j.locator.DefaultGeoLocator;
import com.jkoolcloud.tnt4j.locator.GeoLocator;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * This class provides default implementation of <code>SourceFactory</code> interface.
 * New sources are created off the root source which is defined by <code>RootFQN</code>
 * configuration property. This factory provides the following configuration attributes:
 * 
 * <pre>
 * {@code
 *  source.factory: com.jkoolcloud.tnt4j.source.SourceFactoryImpl
 *  source.factory.GEOADDR: New York
 *  source.factory.DATACENTER: MyDC
 *  source.factory.DEVICE: HPPRO
 *  source.factory.PROCESS: $java.process
 *  source.factory.RootFQN: PROCESS=?#RUNTIME=?#SERVER=?#NETADDR=?#DATACENTER=?#GEOADDR=?	
 * }
 * </pre>
 * 
 * @version $Revision: 1 $
 * 
 */
public class SourceFactoryImpl implements SourceFactory, Configurable {
	public static final String UNKNOWN_SOURCE = "UNKNOWN";
	public static final String DEFAULT_SOURCE_ROOT_SSN = System.getProperty("tnt4j.source.root.ssn", "tnt4j");
	public static final String DEFAULT_SOURCE_ROOT_FQN = System.getProperty("tnt4j.source.root.fqname", "RUNTIME=?#SERVER=?#NETADDR=?#DATACENTER=?#GEOADDR=?");
	
	private static final String TNT4J_SOURCE_PFIX = "tnt4j.source.";	
	private static final String USER_NAME_KEY = "user.name";	
	private static final String [] DEFAULT_SOURCES;
	
	static {
		int i = 0;
		DEFAULT_SOURCES = new String[SourceType.length()];
		String location = DefaultGeoLocator.getInstance().getCurrentCoords();
		if (location != null) {
			DEFAULT_SOURCES[SourceType.GEOADDR.ordinal()] = location;			
		}
		for (SourceType type: SourceType.values()) {
			String typeValue = UNKNOWN_SOURCE;
			String typeString = type.toString().toUpperCase();
			typeValue = System.getProperty(TNT4J_SOURCE_PFIX + typeString);

			if (typeValue == null) {
				if (typeString.equalsIgnoreCase(SourceType.SERVER.name())) {
					typeValue = Utils.getLocalHostName();
				} else if (typeString.equalsIgnoreCase(SourceType.RUNTIME.name())) {
					typeValue = Utils.getVMName();				
				} else if (typeString.equalsIgnoreCase(SourceType.NETADDR.name())) {
					typeValue = Utils.getLocalHostAddress();				
				} else if (typeString.equalsIgnoreCase(SourceType.USER.name())) {
					typeValue = System.getProperty(USER_NAME_KEY);				
				} else {
					typeValue = UNKNOWN_SOURCE;
				}
			} 
			if (typeValue.startsWith("$")) {
				// points to another environment variable
				typeValue = System.getProperty(typeValue.substring(1), UNKNOWN_SOURCE);
			}
			DEFAULT_SOURCES[i++] = typeValue;
		}
	}

	private String rootFqn = DEFAULT_SOURCE_ROOT_FQN;
	private String rootSSN = DEFAULT_SOURCE_ROOT_SSN;
	private String [] defaultSources = DEFAULT_SOURCES.clone();

	private Map<String, Object> config;
	private Source rootSource;
	private GeoLocator geoLocator = DefaultGeoLocator.getInstance();
	
	public SourceFactoryImpl() {
		rootSource = newFromFQN(rootFqn);	
		geoLocator = DefaultGeoLocator.getInstance();
	}
	
	@Override
	public String getSSN() {
		return rootSSN;
	}

	@Override
    public Source fromFQN(String fqn) {
	    return fromFQN(fqn, getRootSource());
    }

	@Override
    public Source fromFQN(String fqn, Source parent) {
	    return createFromFQN(fqn, parent);
    }	

	@Override
    public Source newFromFQN(String fqn) {
		return createFromFQN(fqn, null);
    }

	@Override
    public Source newSource(String name) {
	    return newSource(name, SourceType.APPL, getRootSource());
    }

	@Override
    public Source newSource(String name, SourceType tp) {
	    return newSource(name, tp, getRootSource());
    }

	@Override
    public Source newSource(String name, SourceType tp, Source parent) {
	    return newSource(name, tp, parent, getNameFromType("?", SourceType.USER));
    }

	@Override
    public Source newSource(String name, SourceType tp, Source parent, String user) {
		DefaultSource src =  new DefaultSource(this, getNameFromType(name, tp), tp, parent, user);
		src.setSSN(getSSN());
		return src;
    }

	@Override
	public Source getRootSource() {
		return rootSource;
	}

	@Override
	public GeoLocator getGeoLocator() {
		return geoLocator;
	}

	@Override
    public Map<String, Object> getConfiguration() {
	    return config;
    }

	@Override
    public void setConfiguration(Map<String, Object> settings) throws ConfigException {
		config = settings;
		
		GeoLocator locator = (GeoLocator) Utils.createConfigurableObject("GeoLocator", "GeoLocator.", config);
		geoLocator = locator != null? locator: geoLocator;
		
		// initialize default geo location to the current location
		String location = geoLocator.getCurrentCoords();
		if (location != null) {
			defaultSources[SourceType.GEOADDR.ordinal()] = geoLocator.getCurrentCoords();
		}

		// initialize source types for this factory
		for (SourceType type: SourceType.values()) {
			String typeString = type.toString().toUpperCase();
			Object typeValue = config.get(typeString);
			if (typeValue != null) {
				defaultSources[type.ordinal()] = getNameFromType(String.valueOf(typeValue), type);
			}
		}
		rootFqn = Utils.getString("RootFQN", settings, DEFAULT_SOURCE_ROOT_FQN);
		rootSource = newFromFQN(rootFqn);			
		rootSSN = Utils.getString("RootSSN", settings, DEFAULT_SOURCE_ROOT_SSN);
   }
	
	/**
	 * <p>
	 * Returns current GEO location, format is implementation specific.
	 * Developers should override this method for specific platforms and GEO
	 * implementations.
	 * </p>
	 * 
	 * @return current GEO location
	 */
	public String getCurrentGeoAddr() {
		return defaultSources[SourceType.GEOADDR.ordinal()];
	}
	
	/**
	 * <p>
	 * Returns current data center name, format is implementation specific.
	 * Developers should override this method for specific platforms.
	 * </p>
	 * 
	 * @return current data center name
	 */
	public String getCurrentDatacenter() {
		return defaultSources[SourceType.DATACENTER.ordinal()];
	}
	
	/**
	 * <p>
	 * Returns current source name for specific source type
	 * </p>
	 * @param type source type
	 * @return source name associated with a given type
	 */
	public String getCurrentSource(SourceType type) {
		return defaultSources[type.ordinal()];
	}
	
	/**
	 * <p>
	 * Obtains default name based on a given name/type pair ? name is converted into a default runtime binding.
	 * $property converts property to java property binding.
	 * Example: SERVER=? or PROCESS=$java.process, where java property must be set to java.process=value. 
	 * </p>
	 * 
	 * @param name source name
	 * @param type source type
	 * 
	 * @return source name based on given name and type
	 */
	protected String getNameFromType(String name, SourceType type) {
		if (name == null || name.equals("?")) {
			if (type == SourceType.GEOADDR) {
				String location = geoLocator.getCurrentCoords();
				if (location != null) return location;
			}
			return defaultSources[type.ordinal()];
		}
		return Utils.resolve(name, UNKNOWN_SOURCE);
	}
 
	
	private Source createFromFQN(String fqn, Source parent) {
		StringTokenizer tk = new StringTokenizer(fqn, "#");
		DefaultSource child = null, root = null;
		while (tk.hasMoreTokens()) {
			String sName = tk.nextToken();
			String[] pair = sName.split("=");
			SourceType type = SourceType.valueOf(pair[0]);
			DefaultSource source = new DefaultSource(this, 
					getNameFromType(pair[1], type), 
					type, 
					null,
					getNameFromType("?", SourceType.USER));
			if (child != null)
				child.setSource(source);
			if (root == null)
				root = source;
			child = source;
		}
		if (child != null) {
			child.setSource(parent);
		}
		return root;		
	}
}
