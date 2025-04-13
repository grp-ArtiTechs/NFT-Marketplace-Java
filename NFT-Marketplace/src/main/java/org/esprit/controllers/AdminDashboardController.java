package org.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;
import org.esprit.services.UserService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.fxml.Initializable;

public class AdminDashboardController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> rolesColumn;

    @FXML
    private TableColumn<User, BigDecimal> balanceColumn;

    @FXML
    private TableColumn<User, String> createdAtColumn;

    @FXML
    private TableColumn<User, Void> actionsColumn;

    @FXML
    private Label statusLabel;

    // Blog Management
    @FXML private ListView<Blog> blogListView;
    @FXML private TextField blogTitleField;
    @FXML private TextArea blogContentArea;
    @FXML private ImageView blogImageView;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button saveBlogButton;
    @FXML private Button deleteBlogButton;
    @FXML private Button translateButton;
    
    private UserService userService;
    private BlogService blogService;
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User currentAdminUser;
    private Blog currentBlog;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    private final ObservableList<String> languages = FXCollections.observableArrayList(
        "French", "Spanish", "German", "Italian", "Arabic"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize user management
        userService = new UserService();
        setupTableColumns();
        loadAllUsers();

        // Initialize blog management
        blogService = new BlogService();
        languageComboBox.setItems(languages);
        
        // Initialize blog list view
        refreshBlogList();
        
        // Add selection listener to the blog list view
        blogListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadBlogDetails(newSelection);
                }
            }
        );
    }

    public void setCurrentUser(User user) {
        this.currentAdminUser = user;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        rolesColumn.setCellValueFactory(cellData -> {
            List<String> roles = cellData.getValue().getRoles();
            String rolesString = roles.stream().collect(Collectors.joining(", "));
            return new SimpleStringProperty(rolesString);
        });
        
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        
        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                String formattedDate = cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return new SimpleStringProperty(formattedDate);
            }
            return new SimpleStringProperty("N/A");
        });

        // Setup actions column with edit and delete buttons
        setupActionsColumn();
    }

    private void setupActionsColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox hbox = new HBox(5, editButton, deleteButton);

                    {
                        editButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                        deleteButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                        
                        editButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            openEditUserForm(user);
                        });
                        
                        deleteButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            confirmAndDeleteUser(user);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(hbox);
                        }
                    }
                };
            }
        };
        
        actionsColumn.setCellFactory(cellFactory);
    }

    private void loadAllUsers() {
        try {
            List<User> users = userService.getAll();
            userList.clear();
            userList.addAll(users);
            userTable.setItems(userList);
        } catch (Exception e) {
            showStatus("Error loading users: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            loadAllUsers();
            return;
        }
        
        try {
            // Search by email
            User user = userService.getByEmail(searchText);
            userList.clear();
            
            if (user != null) {
                userList.add(user);
                showStatus("User found.", false);
            } else {
                showStatus("No user found with this email.", true);
            }
            
            userTable.setItems(userList);
        } catch (Exception e) {
            showStatus("Error searching for user: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        loadAllUsers();
        showStatus("", false);
    }

    @FXML
    private void handleAddUser() {
        openAddUserForm();
    }

    private void openAddUserForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserForm.fxml"));
            Parent root = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setMode(UserFormController.FormMode.ADD);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add New User");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showStatus("Error opening add user form: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void openEditUserForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserForm.fxml"));
            Parent root = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setMode(UserFormController.FormMode.EDIT);
            controller.setUser(user);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit User");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showStatus("Error opening edit user form: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void confirmAndDeleteUser(User user) {
        // Don't allow admins to delete themselves
        if (currentAdminUser != null && user.getId() == currentAdminUser.getId()) {
            showStatus("You cannot delete your own account.", true);
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete User");
        alert.setContentText("Are you sure you want to delete user: " + user.getName() + " (" + user.getEmail() + ")?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user);
                loadAllUsers();
                showStatus("User deleted successfully.", false);
            } catch (Exception e) {
                showStatus("Error deleting user: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            ProfileController controller = loader.getController();
            controller.setUser(currentAdminUser);
            
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(profileView, 800, 600));
            stage.setTitle("NFT Marketplace - Profile");
            stage.show();
        } catch (IOException e) {
            showStatus("Error loading profile page: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView, 600, 400));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            showStatus("Error logging out: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    public void refreshUserList() {
        loadAllUsers();
        showStatus("User list refreshed.", false);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(!message.isEmpty());
        
        if (isError) {
            statusLabel.getStyleClass().removeAll("status-success");
            statusLabel.getStyleClass().add("status-error");
        } else {
            statusLabel.getStyleClass().removeAll("status-error");
            statusLabel.getStyleClass().add("status-success");
        }
    }

    @FXML
    private void handleCreateBlog() {
        clearBlogFields();
        currentBlog = new Blog();
        enableBlogFields(true);
    }    @FXML
    private void handleSaveBlog() {
        if (currentAdminUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Admin user information is required to create or edit a blog.");
            return;
        }

        if (currentBlog == null) {
            currentBlog = new Blog();
        }

        currentBlog.setTitle(blogTitleField.getText());
        currentBlog.setContent(blogContentArea.getText());
        currentBlog.setUser(currentAdminUser);
        
        Blog.ValidationResult validationResult = currentBlog.validate();
        if (!validationResult.isValid()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", 
                String.join("\n", validationResult.getErrors().values()));
            return;
        }

        try {
            if (currentBlog.getId() == null) {
                blogService.add(currentBlog);
            } else {
                blogService.update(currentBlog);
            }
            refreshBlogList();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Blog saved successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save blog: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteBlog() {
        if (currentBlog != null && currentBlog.getId() != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this blog?");
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        blogService.delete(currentBlog);
                        refreshBlogList();
                        clearBlogFields();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Blog deleted successfully!");
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Error", 
                            "Failed to delete blog: " + e.getMessage());
                    }
                }
            });
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                String fileName = "blog_" + System.currentTimeMillis() + 
                    selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                Path destination = Paths.get(UPLOAD_DIR + fileName);
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                
                currentBlog.setImageFilename(fileName);
                blogImageView.setImage(new Image(destination.toUri().toString()));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to upload image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleTranslate() {
        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a language first!");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Information", 
            "Translation feature will be implemented soon!");
    }

    private void refreshBlogList() {
        try {
            blogListView.setItems(FXCollections.observableArrayList(blogService.readAll()));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Failed to load blogs: " + e.getMessage());
        }
    }

    private void loadBlogDetails(Blog blog) {
        currentBlog = blog;
        blogTitleField.setText(blog.getTitle());
        blogContentArea.setText(blog.getContent());
        
        if (blog.getImageFilename() != null) {
            try {
                Path imagePath = Paths.get(UPLOAD_DIR + blog.getImageFilename());
                blogImageView.setImage(new Image(imagePath.toUri().toString()));
            } catch (Exception e) {
                blogImageView.setImage(null);
            }
        } else {
            blogImageView.setImage(null);
        }
        
        languageComboBox.setValue(blog.getTranslationLanguage());
        enableBlogFields(true);
    }

    private void clearBlogFields() {
        blogTitleField.clear();
        blogContentArea.clear();
        blogImageView.setImage(null);
        languageComboBox.setValue(null);
        currentBlog = null;
    }

    private void enableBlogFields(boolean enable) {
        blogTitleField.setDisable(!enable);
        blogContentArea.setDisable(!enable);
        saveBlogButton.setDisable(!enable);
        deleteBlogButton.setDisable(!enable);
        translateButton.setDisable(!enable);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}