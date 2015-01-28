package com.jhr.jarvis.model;

import java.util.HashMap;
import java.util.Map;

public class Station {

    private String system;
    private String name;
    private long date;
    
    private Map<String,Commodity> availableCommodityExchanges = new HashMap<>();
    
    public Station(String name, String system) {
        super();
        this.name = name;
        this.system = system;
    }
    
    public Station(String name, String system, long date) {
        super();
        this.name = name;
        this.system = system;
        this.date = date;
    }

    public Station() {
        super();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Station [system=" + system + ", name=" + name + ", date=" + date + "]";
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Station other = (Station) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the system
     */
    public String getSystem() {
        return system;
    }

    /**
     * @param system the system to set
     */
    public void setSystem(String system) {
        this.system = system;
    }

    /**
     * @return the date
     */
    public long getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(long date) {
        this.date = date;
    }

    /**
     * @return the availableCommodityExchanges
     */
    public Map<String, Commodity> getAvailableCommodityExchanges() {
        return availableCommodityExchanges;
    }

    /**
     * @param availableCommodityExchanges the availableCommodityExchanges to set
     */
    public void setAvailableCommodityExchanges(Map<String, Commodity> availableCommodityExchanges) {
        this.availableCommodityExchanges = availableCommodityExchanges;
    }
}
