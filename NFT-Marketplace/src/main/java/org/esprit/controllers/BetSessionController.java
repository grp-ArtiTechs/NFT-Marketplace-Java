package org.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.services.BetSessionService;
import org.esprit.services.UserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BetSessionController implements Initializable {

    @FXML
    private TableView<BetSession> tableView;
    
    @FXML
    private TableColumn<BetSession, String> authorColumn;
    
    @FXML
    private TableColumn<BetSession, String> artworkColumn;
    
    @FXML
    private TableColumn<BetSession, LocalDateTime> createdAtColumn;
    
    @FXML
    private TableColumn<BetSession, LocalDateTime> startTimeColumn;
    
    @FXML
    private TableColumn<BetSession, LocalDateTime> endTimeColumn;
    
    @FXML
    private TableColumn<BetSession, Number> initialPriceColumn;
    
    @FXML
    private TableColumn<BetSession, Number> currentPriceColumn;
    
    @FXML
    private TableColumn<BetSession, String> statusColumn;
    
    @FXML
    private TableColumn<BetSession, Void> actionsColumn;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button updateButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private Button statsButton;
    
    private BetSessionService betSessionService;
    private UserService userService;
    
    // Add field for current user
    private User currentUser;
    
    public BetSessionController() {
        betSessionService = new BetSessionService();
        userService = new UserService();
    }
    
    // Add method to set current user
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup table columns
        authorColumn.setCellValueFactory(cellData -> {
            User author = cellData.getValue().getAuthor();
            return author != null ? 
                   new SimpleStringProperty(author.getName()) : 
                   new SimpleStringProperty("N/A");
        });
        
        artworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");        });
          createdAtColumn.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());
        createdAtColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime createdAt, boolean empty) {
                super.updateItem(createdAt, empty);
                
                if (empty || createdAt == null) {
                    setText(null);
                    return;
                }
                
                // Format date in a readable way
                setText(createdAt.format(formatter));
            }
        });
        
        startTimeColumn.setCellValueFactory(cellData -> cellData.getValue().startTimeProperty());
        startTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime startTime, boolean empty) {
                super.updateItem(startTime, empty);
                
                if (empty || startTime == null) {
                    setText(null);
                    return;
                }
                
                // Format date in a readable way
                setText(startTime.format(formatter));
            }
        });
          // Custom cell factory to format date display for end time
        endTimeColumn.setCellValueFactory(cellData -> cellData.getValue().endTimeProperty());
        endTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime endTime, boolean empty) {
                super.updateItem(endTime, empty);
                
                if (empty || endTime == null) {
                    setText(null);
                    return;
                }
                
                // Format date in a readable way for all sessions
                setText(endTime.format(formatter));
            }
        });
          initialPriceColumn.setCellValueFactory(cellData -> cellData.getValue().initialPriceProperty());
        currentPriceColumn.setCellValueFactory(cellData -> cellData.getValue().currentPriceProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Set up actions column with icon buttons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("\uD83D\uDC41"); // Eye icon
            private final Button editButton = new Button("\uD83D\uDD8C"); // Unicode for paintbrush/edit
            private final Button deleteButton = new Button("\uD83D\uDDD1"); // Trash icon
            private final HBox hbox = new HBox(5, viewButton, editButton, deleteButton);
            {
                viewButton.setStyle("-fx-font-size: 12px; -fx-padding: 2px 5px;");
                editButton.setStyle("-fx-font-size: 12px; -fx-padding: 2px 5px;");
                deleteButton.setStyle("-fx-font-size: 12px; -fx-padding: 2px 5px;");
                viewButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    tableView.getSelectionModel().select(session);
                    viewBetSessionDetails();
                });
                editButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    tableView.getSelectionModel().select(session);
                    showUpdateDialog();
                });
                deleteButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    tableView.getSelectionModel().select(session);
                    deleteSelectedBetSession();
                });
            }            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        
        // Load data
        loadBetSessions();
    }
    private void loadBetSessions() {
        try {
            tableView.setItems(FXCollections.observableArrayList(betSessionService.getAllBetSessions()));
        } catch (Exception e) {
            System.err.println("Error loading bet sessions: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load bet sessions from the database.");
            alert.showAndWait();
        }
    }
      @FXML
    private void showAddDialog() {
        // Check if a user is logged in
        if (currentUser == null) {
            Alert alert = new Alert(AlertType.WARNING, 
                    "You must be logged in to create a bet session.");
            alert.showAndWait();
            return;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add New Bet Session");
        
        // Create form components
        Label authorInfoLabel = new Label("Author: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");
        TextField artworkIdField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField initialPriceField = new TextField();
        Label statusLabel = new Label("Status: pending (automatically set)");
        
        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        formLayout.add(new Label("Author:"), 0, 0);
        formLayout.add(authorInfoLabel, 1, 0);
        formLayout.add(new Label("Artwork ID:"), 0, 1);
        formLayout.add(artworkIdField, 1, 1);
        formLayout.add(new Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);        formLayout.add(new Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new Label("Status:"), 0, 5);
        formLayout.add(statusLabel, 1, 5);
        
        // Add Mystery Mode checkbox
        CheckBox mysteryModeCheckBox = new CheckBox("Enable Mystery Mode");
        formLayout.add(new Label("Mystery Mode:"), 0, 6);
        formLayout.add(mysteryModeCheckBox, 1, 6);
          // Buttons
        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        
        HBox buttonLayout = new HBox(10, saveButton, cancelButton);
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new Insets(10));
        
        saveButton.setOnAction(e -> {
            try {
                // Create new BetSession from form data
                BetSession newBetSession = new BetSession();
                
                // Set Author from current user automatically
                newBetSession.setAuthor(currentUser);
                
                // Set Artwork from ID field
                try {
                    int artworkId = Integer.parseInt(artworkIdField.getText());
                    Artwork artwork = new Artwork();
                    artwork.setId(artworkId);
                    newBetSession.setArtwork(artwork);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR, 
                            "Please enter a valid numeric ID for Artwork.");
                    alert.showAndWait();
                    return;
                }
                
                // Get time values - set to noon by default
                LocalTime defaultTime = LocalTime.of(12, 0);
                
                if (startDatePicker.getValue() != null) {
                    newBetSession.setStartTime(startDatePicker.getValue().atTime(defaultTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    newBetSession.setEndTime(endDatePicker.getValue().atTime(defaultTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    newBetSession.setInitialPrice(initialPrice);
                    
                    // Automatically set current price to initial price
                    newBetSession.setCurrentPrice(initialPrice);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR, 
                            "Please enter a valid price.");
                    alert.showAndWait();
                    return;
                }
                
                // Set status to pending automatically
                newBetSession.setStatus("pending");
                  // Set creation date to current date/time
                newBetSession.setCreatedAt(LocalDateTime.now());
                
                // Set mysterious mode from checkbox
                newBetSession.setMysteriousMode(mysteryModeCheckBox.isSelected());
                
                // Save to database
                betSessionService.addBetSession(newBetSession);
                
                // Refresh table
                loadBetSessions();
                
                // Close dialog
                dialog.close();
                
            } catch (Exception ex) {
                Alert alert = new Alert(AlertType.ERROR, 
                        "Error saving bet session: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        VBox root = new VBox(10, formLayout, buttonLayout);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    @FXML
    private void showUpdateDialog() {
        BetSession selectedBetSession = tableView.getSelectionModel().getSelectedItem();
        if (selectedBetSession == null) {
            Alert alert = new Alert(AlertType.WARNING, 
                    "Please select a bet session to update.");
            alert.showAndWait();
            return;
        }
        
        // Check if bet session is in 'pending' status
        if (!"pending".equals(selectedBetSession.getStatus())) {
            Alert alert = new Alert(AlertType.WARNING,
                    "Only pending bet sessions can be updated.");
            alert.setTitle("Cannot Update");
            alert.setHeaderText("Bet Session Not Pending");
            alert.showAndWait();
            return;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Update Bet Session");
        
        // Create form components - only for editable fields
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField initialPriceField = new TextField();
        TextField currentPriceField = new TextField();
        ComboBox<String> statusComboBox = new ComboBox<>();
        
        // Display author and artwork information (read-only)
        String authorInfo = selectedBetSession.getAuthor() != null ? 
                           "ID: " + selectedBetSession.getAuthor().getId() + 
                           (selectedBetSession.getAuthor().getName() != null ? 
                           ", Name: " + selectedBetSession.getAuthor().getName() : "") : 
                           "N/A";
                           
        String artworkInfo = selectedBetSession.getArtwork() != null ? 
                            "ID: " + selectedBetSession.getArtwork().getId() + 
                            (selectedBetSession.getArtwork().getTitle() != null ? 
                            ", Title: " + selectedBetSession.getArtwork().getTitle() : "") : 
                            "N/A";
        
        Label authorLabel = new Label(authorInfo);
        Label artworkLabel = new Label(artworkInfo);
        
        // Status options - restrict to relevant statuses
        statusComboBox.getItems().addAll("pending", "active");
        
        // Set values from selected bet session
        if (selectedBetSession.getStartTime() != null) {
            startDatePicker.setValue(selectedBetSession.getStartTime().toLocalDate());
        }
        
        if (selectedBetSession.getEndTime() != null) {
            endDatePicker.setValue(selectedBetSession.getEndTime().toLocalDate());
        }
        
        initialPriceField.setText(String.valueOf(selectedBetSession.getInitialPrice()));
        currentPriceField.setText(String.valueOf(selectedBetSession.getCurrentPrice()));
        statusComboBox.setValue(selectedBetSession.getStatus());
        
        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        formLayout.add(new Label("Author (read-only):"), 0, 0);
        formLayout.add(authorLabel, 1, 0);
        formLayout.add(new Label("Artwork (read-only):"), 0, 1);
        formLayout.add(artworkLabel, 1, 1);
        formLayout.add(new Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new Label("Current Price:"), 0, 5);
        formLayout.add(currentPriceField, 1, 5);
        formLayout.add(new Label("Status:"), 0, 6);
        formLayout.add(statusComboBox, 1, 6);
        
        // Buttons
        Button updateButton = new Button("Update");
        Button cancelButton = new Button("Cancel");
        
        HBox buttonLayout = new HBox(10, updateButton, cancelButton);
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new Insets(10));
        
        updateButton.setOnAction(e -> {
            try {
                // Author and Artwork remain unchanged
                // Only update time, price and status
                
                // Get time values - preserve existing time if available, otherwise use noon
                LocalTime defaultTime = LocalTime.of(12, 0);
                LocalTime existingStartTime = selectedBetSession.getStartTime() != null ? 
                                                      selectedBetSession.getStartTime().toLocalTime() : 
                                                      defaultTime;
                LocalTime existingEndTime = selectedBetSession.getEndTime() != null ? 
                                                    selectedBetSession.getEndTime().toLocalTime() : 
                                                    defaultTime;
                
                if (startDatePicker.getValue() != null) {
                    selectedBetSession.setStartTime(startDatePicker.getValue().atTime(existingStartTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    selectedBetSession.setEndTime(endDatePicker.getValue().atTime(existingEndTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    selectedBetSession.setInitialPrice(initialPrice);
                    
                    double currentPrice = Double.parseDouble(currentPriceField.getText());
                    selectedBetSession.setCurrentPrice(currentPrice);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR, 
                            "Please enter valid prices.");
                    alert.showAndWait();
                    return;
                }
                
                selectedBetSession.setStatus(statusComboBox.getValue());
                
                // Save to database
                betSessionService.updateBetSession(selectedBetSession);
                
                // Refresh table
                loadBetSessions();
                
                // Close dialog
                dialog.close();
                
            } catch (Exception ex) {
                Alert alert = new Alert(AlertType.ERROR, 
                        "Error updating bet session: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        VBox root = new VBox(10, formLayout, buttonLayout);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    @FXML
    private void deleteSelectedBetSession() {
        BetSession selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Create confirmation dialog
            Alert confirmDialog = new Alert(AlertType.CONFIRMATION,
                    "Are you sure you want to delete this bet session?",
                    ButtonType.YES,
                    ButtonType.NO
            );
            confirmDialog.setTitle("Confirm Deletion");
            confirmDialog.setHeaderText("Delete Bet Session #" + selected.getId());
            
            // Show dialog and wait for response
            ButtonType result = confirmDialog.showAndWait().orElse(ButtonType.NO);
            
            // If user confirms, delete the bet session
            if (result == ButtonType.YES) {
                try {
                    betSessionService.deleteBetSession(selected.getId());
                    loadBetSessions();
                } catch (SQLException ex) {
                    Alert errorAlert = new Alert(AlertType.ERROR,
                            "Error deleting bet session: " + ex.getMessage());
                    errorAlert.setTitle("Database Error");
                    errorAlert.setHeaderText("Failed to Delete Bet Session");
                    errorAlert.showAndWait();
                }
            }
        } else {
            // No bet session selected, show warning
            Alert warning = new Alert(AlertType.WARNING,
                    "Please select a bet session to delete."
            );
            warning.setTitle("No Selection");
            warning.setHeaderText("No Bet Session Selected");
            warning.showAndWait();
        }
    }
    
    @FXML
    private void viewBetSessionDetails() {
        BetSession selectedBetSession = tableView.getSelectionModel().getSelectedItem();
        if (selectedBetSession == null) {
            Alert alert = new Alert(AlertType.WARNING, 
                    "Please select a bet session to view.");
            alert.showAndWait();
            return;
        }
        
        try {
            // Create new stage for the detail view
            Stage detailStage = new Stage();
            detailStage.initModality(Modality.APPLICATION_MODAL);
            detailStage.setTitle("Bet Session Details");
            
            // Create layout for details
            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);
            
            // Header and info sections
            Label titleLabel = new Label("Bet Session Details");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            
            // Session info
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(15);
            infoGrid.setVgap(10);
            infoGrid.setPadding(new Insets(10));
            
            // Artwork image
            ImageView artworkImageView = new ImageView();
            artworkImageView.setFitWidth(300);
            artworkImageView.setFitHeight(300);
            artworkImageView.setPreserveRatio(true);
              // Try to load artwork image
            if (selectedBetSession.getArtwork() != null && selectedBetSession.getArtwork().getImageName() != null) {
                // Load image using the same path format as in MyBetSessionsController
                String imagePath = "src/main/resources/uploads/" + selectedBetSession.getArtwork().getImageName();
                java.io.File imageFile = new java.io.File(imagePath);
                
                try {
                    // Load image if file exists
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        artworkImageView.setImage(image);
                        
                        System.out.println("Debug - Loading image: " + imagePath);
                        System.out.println("Debug - Mystery Mode: " + selectedBetSession.isMysteriousMode());
                        System.out.println("Debug - Number of Bids: " + selectedBetSession.getNumberOfBids());
                        
                        // Apply blur effect only if the session is in mystery mode
                        if (selectedBetSession.isMysteriousMode()) {
                            // Apply blur effect based on number of bids
                            // 0 bids = full blur (100%), 10+ bids = no blur (0%)
                            int numberOfBids = selectedBetSession.getNumberOfBids();
                            if (numberOfBids < 10) {
                                // Calculate blur amount (from 10 to 0)
                                double blurAmount = 10.0 - numberOfBids;
                                // Scale it to a reasonable range (10 = very blurry, 0 = clear)
                                blurAmount = Math.max(blurAmount * 2.5, 1.0); // Ensure at least some blur
                                
                                // Apply Gaussian blur effect
                                javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(blurAmount);
                                artworkImageView.setEffect(blur);
                                System.out.println("Debug - Applied blur with strength: " + blurAmount);
                                
                                // Add a label showing the number of bids
                                Label blurLabel = new Label("Mystery Mode - Current bids: " + numberOfBids + "/10");
                                blurLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");
                                root.getChildren().add(4, blurLabel); // Insert after the image
                            }
                        }
                    } else {
                        // If file doesn't exist, try using resource stream as fallback
                        System.out.println("Debug - Image file not found at: " + imagePath + ", trying fallback methods");
                        
                        // Try with different extensions and using resource stream
                        int artworkId = selectedBetSession.getArtwork().getId();
                        String resourcePath = "/uploads/artwork_" + artworkId + ".jpg";
                        Image image = new Image(getClass().getResourceAsStream(resourcePath));
                        
                        if (image.getWidth() == 0) {
                            resourcePath = "/uploads/artwork_" + artworkId + ".png";
                            image = new Image(getClass().getResourceAsStream(resourcePath));
                        }
                        
                        if (image.getWidth() == 0) {
                            image = new Image(getClass().getResourceAsStream("/assets/default/artwork-placeholder.png"));
                        }
                        
                        artworkImageView.setImage(image);
                        
                        // Apply blur effect if in mystery mode (same logic as above)
                        if (selectedBetSession.isMysteriousMode()) {
                            int numberOfBids = selectedBetSession.getNumberOfBids();
                            if (numberOfBids < 10) {
                                double blurAmount = (10.0 - numberOfBids) * 2.5;
                                javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(blurAmount);
                                artworkImageView.setEffect(blur);
                                
                                Label blurLabel = new Label("Mystery Mode - Current bids: " + numberOfBids + "/10");
                                blurLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");
                                root.getChildren().add(4, blurLabel);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fall back to default image
                    System.err.println("Error loading image: " + e.getMessage());
                    try {
                        Image defaultImage = new Image(getClass().getResourceAsStream("/assets/default/artwork-placeholder.png"));
                        artworkImageView.setImage(defaultImage);
                    } catch (Exception ex) {
                        System.err.println("Could not load default artwork image: " + ex.getMessage());
                    }
                }
            }
            
            // Add info to grid
            // infoGrid.add(new Label("ID:"), 0, 0);
            // infoGrid.add(new Label(String.valueOf(selectedBetSession.getId())), 1, 0);
            // Do not display BetSession ID
            if (selectedBetSession.getAuthor() != null) {
                infoGrid.add(new Label("Author:"), 0, 0);
                infoGrid.add(new Label(selectedBetSession.getAuthor().getName()), 1, 0);
            }
            if (selectedBetSession.getArtwork() != null) {
                infoGrid.add(new Label("Artwork:"), 0, 1);
                infoGrid.add(new Label(selectedBetSession.getArtwork().getTitle()), 1, 1);
            }
            infoGrid.add(new Label("Initial Price:"), 0, 2);
            infoGrid.add(new Label(String.valueOf(selectedBetSession.getInitialPrice())), 1, 2);
            infoGrid.add(new Label("Current Price:"), 0, 3);
            infoGrid.add(new Label(String.valueOf(selectedBetSession.getCurrentPrice())), 1, 3);
            infoGrid.add(new Label("Status:"), 0, 4);            infoGrid.add(new Label(selectedBetSession.getStatus()), 1, 4);
            if (selectedBetSession.getStartTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                infoGrid.add(new Label("Start Time:"), 0, 5);
                infoGrid.add(new Label(selectedBetSession.getStartTime().format(formatter)), 1, 5);
            }            
            if (selectedBetSession.getEndTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                infoGrid.add(new Label("End Time:"), 0, 6);
                infoGrid.add(new Label(selectedBetSession.getEndTime().format(formatter)), 1, 6);
            }
            
            // Add mysterious mode indicator
            infoGrid.add(new Label("Mysterious Mode:"), 0, 7);
            infoGrid.add(new Label(selectedBetSession.isMysteriousMode() ? "Enabled" : "Disabled"), 1, 7);
            
            // Add artwork description section - show either original or mysterious description
            VBox descriptionBox = new VBox(10);
            Label descriptionTitle = new Label();
            Label descriptionContent = new Label();
            
            // Style the description content for better readability
            descriptionContent.setWrapText(true);
            descriptionContent.setMaxWidth(450);
            descriptionContent.setStyle("-fx-font-size: 14px;");
              // Debug message to see what values we're working with
            System.out.println("Debug - Mysterious Mode: " + selectedBetSession.isMysteriousMode());
            System.out.println("Debug - Generated Description: " + (selectedBetSession.getGeneratedDescription() != null ? selectedBetSession.getGeneratedDescription() : "null"));
            
            if (selectedBetSession.isMysteriousMode() && selectedBetSession.getGeneratedDescription() != null 
                    && !selectedBetSession.getGeneratedDescription().isEmpty()) {
                // Show mysterious description if mysterious mode is enabled and description exists
                descriptionTitle.setText("Mysterious Description:");
                descriptionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                descriptionContent.setText(selectedBetSession.getGeneratedDescription());
                System.out.println("Debug - Using generated description");
            } else {
                // Show original description
                descriptionTitle.setText("Artwork Description:");
                descriptionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                if (selectedBetSession.getArtwork() != null && selectedBetSession.getArtwork().getDescription() != null) {
                    descriptionContent.setText(selectedBetSession.getArtwork().getDescription());
                    System.out.println("Debug - Using artwork description");
                } else {
                    descriptionContent.setText("No description available.");
                    System.out.println("Debug - No description available");
                }
            }
            
            descriptionBox.getChildren().addAll(descriptionTitle, descriptionContent);
            
            // Close button
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> detailStage.close());
              // Add all components to root layout
            root.getChildren().addAll(
                titleLabel, 
                new Separator(), 
                new Label("Artwork Image:"),
                artworkImageView, 
                new Separator(),
                infoGrid,
                new Separator(),
                descriptionBox,
                closeButton
            );
            
            // Create scene and show the stage
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/bet-session.css").toExternalForm());
            
            detailStage.setScene(scene);
            detailStage.show();
        } catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR, 
                    "Error showing bet session details: " + e.getMessage());
            alert.showAndWait();            e.printStackTrace();
        }
    }

    void setBetSession(BetSession selectedSession) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    // Example method for placing a bid (add this where you handle bid placement)
    @FXML
    public void showBetStatistics() {
        try {
            // Load the BetStatistics.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BetStatistics.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            BetStatisticsController statisticsController = loader.getController();
            
            // Create a new stage for the statistics window
            Stage statisticsStage = new Stage();
            statisticsStage.initModality(Modality.APPLICATION_MODAL);
            statisticsStage.setTitle("Bet Statistics Dashboard");
            
            // Set the scene
            Scene scene = new Scene(root);
            statisticsStage.setScene(scene);
            
            // Show the stage
            statisticsStage.show();
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR, 
                    "Error opening statistics window: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
