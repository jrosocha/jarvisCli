package com.jhr.jarvis.fx;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.exceptions.SystemNotFoundException;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

@Component
public class JarvisMain extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    
    private StarSystem currentSystem;
    private ObservableList<Station> stationData = FXCollections.observableArrayList();
    
    @Autowired
    private StarSystemService starSystemServive;
    
    @Autowired
    private StationService stationServive;
    
    public JarvisMain() {
        
        try {
            currentSystem = starSystemServive.findExactSystemOrientDb("CIGURU");
            stationData.addAll(stationServive.getStationsForSystemOrientDb(currentSystem.getName()));
        } catch (SystemNotFoundException e) {
            e.printStackTrace();
        }
        
        
    }
    
	@Override
	public void start(Stage primaryStage) {
		
	    this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Jarvis");

        initRootLayout();

        showOverview();
	    
	}

	public static void main(String[] args) {
		launch(args);
	}

	/**
     * Initializes the root layout.
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(JarvisMain.class.getResource("RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the person overview inside the root layout.
     */
    public void showOverview() {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(JarvisMain.class.getResource("StationOverview.fxml"));
            AnchorPane stationOverview = (AnchorPane) loader.load();

            // Set person overview into the center of root layout.
            rootLayout.setCenter(stationOverview);
            
            // Give the controller access to the main app.
            StationOverviewController controller = loader.getController();
            controller.setMainApp(this);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public ObservableList<Station> getStationData() {
        return stationData;
    }

    public void setStationData(ObservableList<Station> stationData) {
        this.stationData = stationData;
    }

}
