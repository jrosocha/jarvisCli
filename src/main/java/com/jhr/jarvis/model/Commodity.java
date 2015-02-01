package com.jhr.jarvis.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Commodity {

    private String name;
    private String group;
    
    private int buyPrice;
    private int supply;
    private int sellPrice;
    private int demand;
    private long date;
    
    public Commodity(String name) {
        super();
        this.name = name;
    }

    public Commodity(String name, String group) {
        super();
        this.name = name;
        this.group = group;
    }
    
    public Commodity(String name, int buyPrice, int supply, int sellPrice, int demand) {
        super();
        this.name = name;
        this.buyPrice = buyPrice;
        this.supply = supply;
        this.sellPrice = sellPrice;
        this.demand = demand;
    }
    
    public Commodity(String name, int buyPrice, int supply, int sellPrice, int demand, long date) {
        super();
        this.name = name;
        this.buyPrice = buyPrice;
        this.supply = supply;
        this.sellPrice = sellPrice;
        this.demand = demand;
        this.date = date;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Commodity [name=" + name + ", group=" + group + ", buyPrice=" + buyPrice + ", supply=" + supply + ", sellPrice=" + sellPrice + ", demand=" + demand + ", date=" + date + "]";
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("COMMODITY", name);
        map.put("GROUP", group);
        map.put("BUY @", buyPrice);
        map.put("SUPPLY", supply);
        map.put("SELL @", sellPrice);
        map.put("DEMAND", demand);
        map.put("DAYS OLD", (((new Date().getTime() - date)/1000/60/60/24) * 100) / 100);
        
        return map;
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
        Commodity other = (Commodity) obj;
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
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the buyPrice
     */
    public int getBuyPrice() {
        return buyPrice;
    }

    /**
     * @param buyPrice the buyPrice to set
     */
    public void setBuyPrice(int buyPrice) {
        this.buyPrice = buyPrice;
    }

    /**
     * @return the supply
     */
    public int getSupply() {
        return supply;
    }

    /**
     * @param supply the supply to set
     */
    public void setSupply(int supply) {
        this.supply = supply;
    }

    /**
     * @return the sellPrice
     */
    public int getSellPrice() {
        return sellPrice;
    }

    /**
     * @param sellPrice the sellPrice to set
     */
    public void setSellPrice(int sellPrice) {
        this.sellPrice = sellPrice;
    }

    /**
     * @return the demand
     */
    public int getDemand() {
        return demand;
    }

    /**
     * @param demand the demand to set
     */
    public void setDemand(int demand) {
        this.demand = demand;
    }

}
