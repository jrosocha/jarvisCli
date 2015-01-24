package com.jhr.jarvis.model;

public class Exchange {
    
    private Station from;
    private Station to;
    
    public Exchange(Station from, Station to) {
        super();
        this.from = from;
        this.to = to;
    }
    /**
     * @return the from
     */
    public Station getFrom() {
        return from;
    }
    /**
     * @param from the from to set
     */
    public void setFrom(Station from) {
        this.from = from;
    }
    /**
     * @return the to
     */
    public Station getTo() {
        return to;
    }
    /**
     * @param to the to to set
     */
    public void setTo(Station to) {
        this.to = to;
    }
    
}
