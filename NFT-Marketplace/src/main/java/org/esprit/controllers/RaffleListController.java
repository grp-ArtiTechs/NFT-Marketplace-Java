package org.esprit.controllers;

import java.io.IOException;
import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.RaffleService;
import org.esprit.services.ArtworkService;
import org.esprit.models.Artwork;
import org.esprit.controllers.UserDashboardController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.web.WebView;

public class RaffleListController {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> statusFilter;
    
    @FXML
    private GridPane rafflesGrid;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private RaffleService raffleService;
    
    @FXML
    public void initialize() {
        raffleService = new RaffleService();
        
        // Initialize the controller
        statusFilter.getItems().addAll("All", "active", "ended");
        statusFilter.setValue("All");
        
        // Add listener for search field and status filter
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRaffles());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshRaffles());

        // Add sceneProperty listener to load chatbot stylesheet after scene is set
        rafflesGrid.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    loadChatbotStylesheet(newScene);
                });
            }
        });

        // Initial load of raffles
        refreshRaffles();
    }

    /**
     * Loads the chatbot stylesheet when the scene is available
     */
    private void loadChatbotStylesheet(Scene scene) {
        try {
            String chatbotStylesheet = getClass().getResource("/styles/chatbot.css").toExternalForm();
            if (!scene.getStylesheets().contains(chatbotStylesheet)) {
                scene.getStylesheets().add(chatbotStylesheet);
            }
        } catch (Exception e) {
            System.err.println("Error loading chatbot stylesheet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        // Refresh raffles again after user is set
        refreshRaffles();
    }
      @FXML
    private void handleCreateRaffle(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateRaffle.fxml"));
            Parent createRaffleView = loader.load();
            
            CreateRaffleController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(createRaffleView));
            stage.setTitle("Create New Raffle");
            
            // Set the stage to fullscreen
            stage.setMaximized(true);
            
            stage.show();
            
        } catch (IOException e) {
            showError("Could not open create raffle view: " + e.getMessage());
        }
    }
    
    public void refreshRaffles() {
        try {
            // Clear the current grid
            rafflesGrid.getChildren().clear();
            rafflesGrid.getRowConstraints().clear();
            rafflesGrid.getColumnConstraints().clear();
            
            // Add column constraints for even spacing
            for (int i = 0; i < 3; i++) {
                javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
                col.setHgrow(javafx.scene.layout.Priority.SOMETIMES);
                col.setMinWidth(300);
                col.setPrefWidth(320);
                rafflesGrid.getColumnConstraints().add(col);
            }
            
            // Get all raffles
            List<Raffle> raffles = raffleService.getAll();
            
            // Filter based on search text and status
            String searchText = searchField.getText().toLowerCase();
            String statusValue = statusFilter.getValue();
            
            raffles = raffles.stream()
                .filter(raffle -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                        raffle.getTitle().toLowerCase().contains(searchText) ||
                        raffle.getRaffleDescription().toLowerCase().contains(searchText);
                        
                    boolean matchesStatus = statusValue.equals("All") ||
                        raffle.getStatus().equalsIgnoreCase(statusValue);
                        
                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());
            
            // Display raffles in grid
            int col = 0;
            int row = 0;
            final int MAX_COLUMNS = 3;
            
            // If no raffles match the filter criteria
            if (raffles.isEmpty()) {
                Label noRafflesLabel = new Label("No raffles found matching your criteria");
                noRafflesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #757575; -fx-padding: 20px;");
                rafflesGrid.add(noRafflesLabel, 0, 0, 3, 1); // span across all columns
            } else {
                for (Raffle raffle : raffles) {
                    VBox raffleCard = createRaffleCard(raffle);
                    rafflesGrid.add(raffleCard, col, row);
                    
                    // Add some visual spacing between cards
                    GridPane.setMargin(raffleCard, new Insets(10));
                    
                    col++;
                    if (col >= MAX_COLUMNS) {
                        col = 0;
                        row++;
                        
                        // Add row constraints for consistent height
                        javafx.scene.layout.RowConstraints rowConstraint = new javafx.scene.layout.RowConstraints();
                        rowConstraint.setVgrow(javafx.scene.layout.Priority.ALWAYS);
                        rowConstraint.setMinHeight(450); // Adjust as needed
                        rafflesGrid.getRowConstraints().add(rowConstraint);
                    }
                }
            }
            
            statusLabel.setVisible(false);
            
        } catch (SQLException e) {
            showError("Error loading raffles: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private VBox createRaffleCard(Raffle raffle) {
        VBox card = new VBox(10);
        card.getStyleClass().add("raffle-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2); -fx-background-radius: 5;");
        
        // Create image container with consistent size for all cards
        javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane();
        imageContainer.setMinWidth(280);
        imageContainer.setMinHeight(200);
        imageContainer.setPrefWidth(280);
        imageContainer.setPrefHeight(200);
        imageContainer.setMaxWidth(280);
        imageContainer.setMaxHeight(200);
        imageContainer.setStyle("-fx-background-color: #BBDEFB; -fx-border-color: #3F51B5; -fx-border-width: 1px; -fx-background-radius: 3; -fx-border-radius: 3;");
        
        // Add artwork image
        ImageView artworkImage = new ImageView();
        artworkImage.setFitWidth(280);
        artworkImage.setFitHeight(200);
        artworkImage.setPreserveRatio(true);
        
        boolean imageLoaded = false;
        
        try {
            ArtworkService artworkService = new ArtworkService();
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            
            if (artwork != null && artwork.getImageName() != null) {
                // Check if the image name is an Imgur URL
                if (artwork.getImageName().startsWith("http")) {
                    // Create a WebView to load the Imgur image with referrerpolicy
                    javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
                    webView.setPrefWidth(280);
                    webView.setPrefHeight(200);
                    webView.setMaxWidth(280);
                    webView.setMaxHeight(200);
                    
                    // Create HTML content with referrerpolicy
                    String htmlContent = String.format(
                        "<html><body style='margin:0;padding:0;'><img src='%s' style='width:100%%;height:100%%;object-fit:cover;' referrerpolicy='no-referrer'/></body></html>",
                        artwork.getImageName()
                    );
                    
                    webView.getEngine().loadContent(htmlContent);
                    imageContainer.getChildren().add(webView);
                    imageLoaded = true;
                } else {
                    // Handle local images as before
                    // DEBUG: List available files in uploads directory
                    try {
                        File uploadsDir = new File("src/main/resources/uploads");
                        if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                            System.out.println("Available files in uploads directory:");
                            File[] files = uploadsDir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    System.out.println("  - " + file.getName());
                                }
                            } else {
                                System.out.println("  No files found or cannot list files");
                            }
                        } else {
                            System.out.println("Uploads directory not found at: " + uploadsDir.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        System.err.println("Error listing files in uploads directory: " + e.getMessage());
                    }
                    
                    System.out.println("Attempting to load image: " + artwork.getImageName());
                    
                    // Try multiple approaches to load the image
                    
                    // 1. Try absolute path with src/main/resources
                    File imageFile = new File("src/main/resources/uploads/" + artwork.getImageName());
                    if (imageFile.exists()) {
                        try {
                            Image image = new Image(imageFile.toURI().toString());
                            if (!image.isError()) {
                                artworkImage.setImage(image);
                                imageLoaded = true;
                                System.out.println("Loaded image from src/main/resources/uploads: " + artwork.getImageName());
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load image from src/main/resources path: " + e.getMessage());
                        }
                    }
                    
                    // 2. Try using the class resource loader
                    if (!imageLoaded) {
                        try {
                            String imagePath = "/uploads/" + artwork.getImageName();
                            java.io.InputStream is = getClass().getResourceAsStream(imagePath);
                            if (is != null) {
                                Image image = new Image(is);
                                if (!image.isError()) {
                                    artworkImage.setImage(image);
                                    imageLoaded = true;
                                    System.out.println("Loaded image from resource stream: " + artwork.getImageName());
                                }
                            } else {
                                System.out.println("Resource stream is null for: " + imagePath);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load image from resource stream: " + e.getMessage());
                        }
                    }
                    
                    // 3. Try absolute path with direct project path
                    if (!imageLoaded) {
                        try {
                            File projectRoot = new File("").getAbsoluteFile();
                            File uploadsDir = new File(projectRoot, "uploads");
                            File directImageFile = new File(uploadsDir, artwork.getImageName());
                            
                            if (directImageFile.exists()) {
                                Image image = new Image(directImageFile.toURI().toString());
                                if (!image.isError()) {
                                    artworkImage.setImage(image);
                                    imageLoaded = true;
                                    System.out.println("Loaded image from direct project path: " + directImageFile.getPath());
                                }
                            } else {
                                System.out.println("Image file not found at path: " + directImageFile.getPath());
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load image from direct project path: " + e.getMessage());
                        }
                    }
                    
                    // 4. Try using the file class loader
                    if (!imageLoaded) {
                        try {
                            ClassLoader classLoader = getClass().getClassLoader();
                            java.net.URL imageUrl = classLoader.getResource("uploads/" + artwork.getImageName());
                            if (imageUrl != null) {
                                Image image = new Image(imageUrl.toString());
                                if (!image.isError()) {
                                    artworkImage.setImage(image);
                                    imageLoaded = true;
                                    System.out.println("Loaded image using class loader: " + imageUrl);
                                }
                            } else {
                                System.out.println("Image URL not found using class loader");
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load image using class loader: " + e.getMessage());
                        }
                    }
                    
                    // 5. Fallback: Try to find any image in uploads directory to use as a fallback
                    if (!imageLoaded) {
                        try {
                            File uploadsDir = new File("src/main/resources/uploads");
                            if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                                File[] imageFiles = uploadsDir.listFiles((dir, name) -> 
                                    name.toLowerCase().endsWith(".png") || 
                                    name.toLowerCase().endsWith(".jpg") || 
                                    name.toLowerCase().endsWith(".jpeg")
                                );
                                
                                if (imageFiles != null && imageFiles.length > 0) {
                                    // Use the first image found as a fallback
                                    File fallbackFile = imageFiles[0];
                                    System.out.println("Using fallback image: " + fallbackFile.getName());
                                    
                                    try {
                                        Image image = new Image(fallbackFile.toURI().toString());
                                        if (!image.isError()) {
                                            artworkImage.setImage(image);
                                            imageLoaded = true;
                                            System.out.println("Loaded fallback image: " + fallbackFile.getName());
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Failed to load fallback image: " + e.getMessage());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error finding fallback image: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in artwork loading: " + e.getMessage());
            e.printStackTrace();
        }
        
        // If image not loaded, add placeholder
        if (!imageLoaded) {
            // Create placeholder text
            Label placeholderLabel = new Label("Artwork\nImage");
            placeholderLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #3F51B5; -fx-font-weight: bold; -fx-alignment: center;");
            placeholderLabel.setWrapText(true);
            placeholderLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            imageContainer.getChildren().add(placeholderLabel);
        }
        
        // Add a border and vbox for card contents
        VBox contentBox = new VBox(4); // even less spacing
        contentBox.setPadding(new Insets(8, 0, 0, 0));
        contentBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        // Status label (Ended or Active)
        Label statusLabel = null;
        if ("ended".equalsIgnoreCase(raffle.getStatus())) {
            statusLabel = new Label("Ended");
            statusLabel.getStyleClass().add("ended-label");
        } else if ("active".equalsIgnoreCase(raffle.getStatus())) {
            statusLabel = new Label("Active");
            statusLabel.getStyleClass().add("active-label");
        }
        if (statusLabel != null) {
            statusLabel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            VBox.setMargin(statusLabel, new Insets(0, 0, 0, 0));
            contentBox.getChildren().add(statusLabel);
        }

        Label titleLabel = new Label(raffle.getTitle());
        titleLabel.getStyleClass().add("raffle-title");
        titleLabel.setWrapText(true);

        Label artworkLabel = new Label("Artwork: " + (raffle.getArtworkTitle() != null ? raffle.getArtworkTitle() : ""));
        artworkLabel.getStyleClass().add("raffle-details");
        artworkLabel.setWrapText(true);

        Label creatorLabel = new Label("Created by: " + (raffle.getCreatorName() != null ? raffle.getCreatorName() : ""));
        creatorLabel.getStyleClass().add("raffle-details");
        creatorLabel.setWrapText(true);

        contentBox.getChildren().addAll(titleLabel, artworkLabel, creatorLabel);

        // Spacer to push the button to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        contentBox.getChildren().add(spacer);

        Button detailsButton = new Button("View Details");
        detailsButton.getStyleClass().add("primary-button");
        detailsButton.setPrefWidth(160);
        detailsButton.setMinWidth(120);
        detailsButton.setMaxWidth(180);
        detailsButton.setStyle("-fx-font-size: 16px; -fx-background-radius: 16px; -fx-padding: 6 0 6 0;");
        detailsButton.setOnAction(e -> showRaffleDetails(raffle));
        VBox.setMargin(detailsButton, new Insets(8, 0, 0, 0));
        contentBox.getChildren().add(detailsButton);

        card.getChildren().clear();
        card.getChildren().addAll(imageContainer, contentBox);
        card.setStyle("-fx-background-color: #232228; -fx-background-radius: 20px; -fx-effect: dropshadow(gaussian, #00000088, 10, 0.2, 0, 4); -fx-padding: 20; -fx-border-color: #383848; -fx-border-width: 1.5; -fx-border-radius: 20px;");
        return card;
    }
    
    private void showRaffleDetails(Raffle raffle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleDetails.fxml"));
            Parent detailsView = loader.load();
            
            RaffleDetailsController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setCurrentUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            Scene scene = new Scene(detailsView);
            stage.setScene(scene);
            stage.setTitle("Raffle Details: " + raffle.getTitle());
            
            // Set a reasonable size that ensures all content is visible
            stage.setMinWidth(600);
            stage.setMinHeight(650);
            stage.setWidth(600);
            stage.setHeight(650);
            
            stage.show();
            
        } catch (IOException e) {
            showError("Could not open raffle details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }

    /**
     * Cleanup resources before controller is destroyed
     * Called when application is closing to handle any necessary cleanup
     */
    public void cleanup() {
        // Close any open dialogs
        if (searchField != null && searchField.getScene() != null && searchField.getScene().getWindow() != null) {
            searchField.getScene().getWindow().hide();
        }
        
        // Clear references
        currentUser = null;
        raffleService = null;
        
        // Clear UI elements
        if (rafflesGrid != null) {
            rafflesGrid.getChildren().clear();
        }
        if (statusFilter != null) {
            statusFilter.getItems().clear();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Return to the user dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDashboard.fxml"));
            Parent dashboardView = loader.load();
            
            UserDashboardController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            Scene scene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) scene.getWindow();
            
            scene.setRoot(dashboardView);
            stage.setTitle("NFT Marketplace - User Dashboard");
        } catch (IOException e) {
            showError("Failed to return to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to get artwork value (dummy for now)
    private String getArtworkValue(Integer artworkId) {
        // You can implement actual logic to fetch artwork value if needed
        return "300";
    }
}