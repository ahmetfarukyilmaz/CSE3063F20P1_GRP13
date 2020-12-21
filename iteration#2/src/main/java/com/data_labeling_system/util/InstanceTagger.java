package com.data_labeling_system.util;

import com.data_labeling_system.mechanism.LabelingMechanism;
import com.data_labeling_system.model.Assignment;
import com.data_labeling_system.model.Dataset;
import com.data_labeling_system.model.Instance;
import com.data_labeling_system.model.User;
import org.apache.log4j.Logger;

import java.util.List;

public class InstanceTagger {
    private final Logger logger;

    private Dataset dataset;
    private List<User> users;

    public InstanceTagger() {
        logger = Logger.getLogger(Instance.class);
    }

    public void assignLabels() {
        List<Assignment> assignments = this.dataset.getAssignments();
        logger.info("The list of assigment was created successfully.");

        //  Using the labeling mechanism the user has; assign user, instance and labels values into assignments
        while (!this.users.isEmpty()) {
            for (int i = 0; i < this.users.size(); i++) {
                User currentUser = this.users.get(i);
                int nextInstanceToBeLabelled = this.dataset.getNextInstancesToBeLabelled().get(currentUser);

                //If the user has completed all the labellings in current dataset
                if (this.dataset.getInstances().size() <= nextInstanceToBeLabelled) {
                    this.users.remove(i);
                    i--;
                    continue;
                }
                int randomNumber = (int) ((Math.random() * 100) + 1);
                int currentInstanceToBeLabelled =
                        ((randomNumber <= currentUser.getConsistencyCheckProbability() * 100)) ?
                                (int) (Math.random() * nextInstanceToBeLabelled) : nextInstanceToBeLabelled;

                LabelingMechanism labelingMechanism = currentUser.getMechanism();
                Assignment assignment = labelingMechanism.assign(currentUser, dataset.getInstances().get(currentInstanceToBeLabelled),
                        dataset.getLabels(), dataset.getMaxNumOfLabels());
                assignments.add(assignment);

                for (int j = 0; j < assignment.getLabels().size(); j++) {

                    logger.info("user id:" + currentUser.getId() + " " + currentUser.getName() + " tagged instance id:"
                            + assignment.getInstanceId() + " with class label:" + assignment.getLabels().get(j).getId()
                            + ":" + assignment.getLabels().get(j).getText() + ", instance:'"
                            + assignment.getInstance().getInstance() + "'");
                }
                this.dataset.getInstances().get(currentInstanceToBeLabelled).setFinalLabel(assignments);
                if (currentInstanceToBeLabelled == nextInstanceToBeLabelled)
                    this.dataset.getNextInstancesToBeLabelled().put(currentUser, ++nextInstanceToBeLabelled);


                //Output and other assignments...
            }
        }
        this.dataset.setAssignments(assignments);
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}