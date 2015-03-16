package com.jhr.jarvis.fx;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;

import com.jhr.jarvis.model.Station;

public class StationOverviewController {

    private JarvisMain jarvisMain;
    
    @FXML
    private TableView<Station> stationTable;
    @FXML
    private TableColumn<Station, String> stationNameColumn;
    @FXML
    private TableColumn<Station, Boolean> stationBlackMarketFlagColumn;
    @FXML
    private TableColumn<Station, Long> stationDataAgeColumn;
    
    
    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    private void initialize() {
        // Initialize the person table with the two columns.
        stationNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue(), "name"));
        stationBlackMarketFlagColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue(), "blackMarket"));
        stationDataAgeColumn.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue(), "date").asObject());
    }

    /**
     * Is called by the main application to give a reference back to itself.
     * 
     * @param mainApp
     */
    public void setMainApp(JarvisMain jarvisMain) {
        this.jarvisMain = jarvisMain;

        // Add observable list data to the table
        stationTable.setItems(jarvisMain.getStationData());
    }
}
