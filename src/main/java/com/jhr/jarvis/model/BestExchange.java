package com.jhr.jarvis.model;

public class BestExchange {

    private String buySystemName;
    private String buyStationName;
    private String commodity;
    private int buyPrice;
    private int supply;

    private String sellSystemName;
    private String sellStationName;
    private int sellPrice;
    private int demand;
    
    private int perUnitProfit;

    
    public BestExchange() {
        super();
    }
    
    public BestExchange(Station fromStation, Station toStation, Commodity buyCommodity, Commodity sellCommodity) {
        super();
        setBuySystemName(fromStation.getSystem());
        setBuyStationName(fromStation.getName());
        setCommodity(buyCommodity.getName());
        setBuyPrice(buyCommodity.getBuyPrice());
        setSupply(buyCommodity.getSupply());
        setSellSystemName(toStation.getSystem());
        setSellStationName(toStation.getName());
        setSellPrice(sellCommodity.getSellPrice());
        setDemand(sellCommodity.getDemand());
        setPerUnitProfit(sellCommodity.getSellPrice() - buyCommodity.getBuyPrice());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BestExchange [buySystemName=" + buySystemName + ", buyStationName=" + buyStationName + ", commodity=" + commodity + ", buyPrice=" + buyPrice + ", supply=" + supply
                + ", sellSystemName=" + sellSystemName + ", sellStationName=" + sellStationName + ", sellPrice=" + sellPrice + ", demand=" + demand + ", perUnitProfit=" + perUnitProfit + "]";
    }
    
    
    /* accessors / mutators */
    
    /**
     * @return the buySystemName
     */
    public String getBuySystemName() {
        return buySystemName;
    }

    /**
     * @param buySystemName the buySystemName to set
     */
    public void setBuySystemName(String buySystemName) {
        this.buySystemName = buySystemName;
    }

    /**
     * @return the buyStationName
     */
    public String getBuyStationName() {
        return buyStationName;
    }

    /**
     * @param buyStationName the buyStationName to set
     */
    public void setBuyStationName(String buyStationName) {
        this.buyStationName = buyStationName;
    }

    /**
     * @return the commodity
     */
    public String getCommodity() {
        return commodity;
    }

    /**
     * @param commodity the commodity to set
     */
    public void setCommodity(String commodity) {
        this.commodity = commodity;
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
     * @return the sellSystemName
     */
    public String getSellSystemName() {
        return sellSystemName;
    }

    /**
     * @param sellSystemName the sellSystemName to set
     */
    public void setSellSystemName(String sellSystemName) {
        this.sellSystemName = sellSystemName;
    }

    /**
     * @return the sellStationName
     */
    public String getSellStationName() {
        return sellStationName;
    }

    /**
     * @param sellStationName the sellStationName to set
     */
    public void setSellStationName(String sellStationName) {
        this.sellStationName = sellStationName;
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

    /**
     * @return the perUnitProfit
     */
    public int getPerUnitProfit() {
        return perUnitProfit;
    }

    /**
     * @param perUnitProfit the perUnitProfit to set
     */
    public void setPerUnitProfit(int perUnitProfit) {
        this.perUnitProfit = perUnitProfit;
    }
    
}
