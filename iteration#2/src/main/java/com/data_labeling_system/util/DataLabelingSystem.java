package com.data_labeling_system.util;

import com.data_labeling_system.model.Dataset;
import com.data_labeling_system.model.User;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataLabelingSystem {
    private final Logger logger;
    private final List<Dataset> datasets;
    private final IOManager ioManager;
    private final UserManager userManager;
    private final InstanceTagger instanceTagger;

    public DataLabelingSystem() {
        this.logger = Logger.getLogger(DataLabelingSystem.class);
        this.ioManager = new IOManager();
        this.userManager = new UserManager();
        this.instanceTagger = new InstanceTagger();
        this.datasets = new ArrayList<>();
    }

    public void startSystem() {
        logger.info("The system has started");
        // Create output folders to organize datasets and metrics
        if (new File("outputs").mkdir())
            logger.info("'outputs' folder has been created.");
        else
            logger.error("Can't create 'outputs' folder.");
        if (new File("metrics").mkdir())
            logger.info("'metrics' folder has been created.");
        else
            logger.error("Can't create 'metrics' folder.");
        // Read json files and keep as string
        String configJson = this.ioManager.readInputFile("config.json");
        userManager.createUsers(configJson);
        JSONObject configObject = new JSONObject(configJson);
        JSONArray datasetArray = configObject.getJSONArray("datasets");
        int currentDatasetId = configObject.getInt("currentDatasetId");
        Dataset currentDataset = null;
        String inputFile;
        for (int i = 0; i < datasetArray.length(); i++) {
            JSONObject datasetObject = datasetArray.getJSONObject(i);
            int id = datasetObject.getInt("id");

            boolean doesFileExist = new File("./outputs/output" + id + ".json").exists();

            if (doesFileExist) {
                inputFile = "./outputs/output" + id + ".json";
            } else {
                inputFile = datasetObject.getString("filePath");
            }

            String datasetJson = this.ioManager.readInputFile(inputFile);
            JSONArray registeredUserIds = datasetObject.getJSONArray("users");

            List<User> registeredUsers = new ArrayList<>();

            for (int j = 0; j < registeredUserIds.length(); j++) {
                registeredUsers.add(userManager.findUser(registeredUserIds.getInt(j)));
            }

            Dataset dataset = new Dataset(datasetJson, registeredUsers);
            datasets.add(dataset);

            for (User user : registeredUsers) {
                user.getStatistic().addDataset(dataset);
            }

            if (id == currentDatasetId) {
                currentDataset = dataset;
            }
        }

        if (currentDataset == null) {
            logger.error("Current dataset is not defined.");
            return;
        }

        for (User user : this.userManager.getUsers()) {
            user.getStatistic().calculateMetrics();
        }

        for (Dataset dataset : datasets) {
            dataset.getStatistic().calculateMetrics(dataset);
        }

        // Assign updated objects to the instanceTagger object
        this.instanceTagger.setDataset(currentDataset);
        ArrayList<User> activeUsers = new ArrayList<>(currentDataset.getUsers());
        this.instanceTagger.setUsers(activeUsers);
        // Assign label to instances
        this.instanceTagger.assignLabels();
        // Take final dataset and write as json file
        currentDataset = this.instanceTagger.getDataset();
        this.ioManager.printFinalDataset(currentDataset, "outputs/output" + currentDatasetId + ".json");
        this.ioManager.printMetrics(this.datasets, this.userManager.getUsers());
    }
}
