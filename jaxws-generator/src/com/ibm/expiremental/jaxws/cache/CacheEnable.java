package com.ibm.expiremental.jaxws.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ejs.util.am.AlarmManager;

public class CacheEnable implements AlarmListener {

	private static int reloadInterval;
	private static final File PROP_FILE = new File(System.getenv("USER_INSTALL_ROOT") + "/properties/cache-enable.properties");
	private static final String CLASS = CacheEnable.class.getName();
	private static final Logger LOG = Logger.getLogger(CLASS);
	
	private static final String SEPARATOR = "=";
	private static final Collection<String> enable = Arrays.asList("true", "on", "enable", "1", "+");
	private static final Collection<String> disable = Arrays.asList("false", "off", "disable", "0", "-");
	private static final Collection<String> enableWildcard = Arrays.asList("wildcard", "all", "enableAll", "wildcardOn", "++");
	private static final Collection<String> disableWildcard = Arrays.asList("wildcardOff", "disableAll", "wildcardDisable", "disableWildcard", "--");
	private static CacheEnable _instance;

	private Set<String> actions;
	private Set<String> disabledActions;
	private Set<String> wildcards;
	private Set<String> disabledWildcards;
	private long lastModified;
	private boolean alarm = true;
	
	public CacheEnable() {
		int defaultInterval = 3000;
		try {
			reloadInterval = Integer.valueOf(System.getProperty("com.ibm.expiremental.jaxws.cache.CacheEnable.reloadInterval"));
			if (reloadInterval < defaultInterval)
				reloadInterval = defaultInterval;
		} catch (NumberFormatException e) {
			reloadInterval = defaultInterval;
		}
		
		if (LOG.isLoggable(Level.INFO))
			LOG.info("Properties watcher '" + Integer.toHexString(hashCode()) +"' turned on with reload interval: " + reloadInterval +" ms");
	}

	@Override
	public void alarm(Object paramObject) {
		if (PROP_FILE.exists()) {
			if (PROP_FILE.lastModified() > lastModified) {
				readProperties();
			}
		}
		
		if (alarm)
			AlarmManager.createDeferrable(reloadInterval, this);
	}

	public static boolean isValid(String action) {
		if (_instance == null) {
			init();
		}
		
		if (_instance.disabledActions.contains(action))
			return false;
		
		if (_instance.actions.contains(action))
			return true;
		
		for (String wildcard : _instance.disabledWildcards) {
			try {
				if (action.matches(wildcard))
					return false;
			} catch (Exception e) {
				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning(e.getMessage());
				}
			}
		}
		
		for (String wildcard : _instance.wildcards) {
			try {
				if (action.matches(wildcard))
					return true;
			} catch (Exception e) {
				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning(e.getMessage());
				}
			}
		}
		
		return false;
	}
	
	protected void readProperties() {
		if (LOG.isLoggable(Level.INFO))
			LOG.info("Reading properties from " + PROP_FILE);
		
		try(BufferedReader br = new BufferedReader(new FileReader(PROP_FILE))) {
			actions = new HashSet<>();
			disabledActions = new HashSet<>();
			wildcards = new HashSet<>();
			disabledWildcards = new HashSet<>();
			
			String line = null;
			while ((line = br.readLine()) != null) {
				parseLine(line);
			}
		} catch (IOException e) {
			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning(e.getMessage());
			}
		} finally {
			lastModified = PROP_FILE.lastModified();
		}
	}

	private void parseLine(String line) {
		try {
			String[] splitted = line.split(SEPARATOR);
			String key = getKey(splitted);
			String value = getValue(splitted);
			
			if (disable.contains(value)) {
				disabledActions.add(key);
				if (LOG.isLoggable(Level.INFO))
					LOG.info("action disabled: " + key);
			} else if (enable.contains(value)) {
				actions.add(key);
				if (LOG.isLoggable(Level.INFO))
					LOG.info("action added: " + key);
			} else if (disableWildcard.contains(value)) {
				disabledWildcards.add(key);
				if (LOG.isLoggable(Level.INFO))
					LOG.info("disable wildcard added: " + key);
			} else if (enableWildcard.contains(value)) {
				wildcards.add(key);
				if (LOG.isLoggable(Level.INFO))
					LOG.info("wildcard added: " + key);
			} else {
				if (LOG.isLoggable(Level.INFO))
					LOG.info("skipped: '" + key + "', value was not recognized: " + value);
			}
		} catch (Exception e) {
			if (LOG.isLoggable(Level.FINEST)) {
				LOG.fine("Line: '" + line +"' is not processed");
			}
		}
	}

	private String getKey(String[] splitted) {
		if (splitted[0].trim().isEmpty())
			throw new IllegalArgumentException();
		return splitted[0];
	}

	private String getValue(String[] splitted) {
		try {
			return splitted[1];
		} catch (ArrayIndexOutOfBoundsException e) {
			return enable.iterator().next();
		}
	}

	private synchronized static void init() {
		if (_instance == null) {
			_instance = new CacheEnable();
			_instance.alarm(null);
		}
	}
	
	public static void kill() {
		if (_instance != null && _instance.alarm) {
			_instance.alarm = false;
			_instance = null;
			if (LOG.isLoggable(Level.INFO))
				LOG.info("Properties watcher '" +Integer.toHexString(_instance.hashCode()) + "' for " + PROP_FILE + " turned off");
		}
	}
}
