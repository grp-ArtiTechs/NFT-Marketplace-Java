package org.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import org.esprit.models.User;
import org.esprit.services.ImgurService;
import org.esprit.services.UserService;
import org.esprit.utils.ConfigManager;
import org.esprit.utils.PasswordHasher;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EditProfileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField walletField;

    @FXML
    private TextField githubField;
    
    @FXML
    private TextField currentPassword;
    
    @FXML
    private TextField newPassword;
    
    @FXML
    private TextField confirmPassword;

    @FXML
    private Label statusLabel;

    @FXML
    private ImageView profileImageView;
    
    @FXML
    private ProgressIndicator uploadProgressIndicator;

    private User currentUser;
    private final UserService userService;
    private final ImgurService imgurService;
    private File selectedProfilePicFile;    public EditProfileController() {
        userService = new UserService();
        imgurService = new ImgurService();
    }
    
    public void initialize() {
        // This method is automatically called after the FXML file has been loaded
        // We'll populate fields in the setUser method
        
        // Hide progress indicator initially
        if (uploadProgressIndicator != null) {
            uploadProgressIndicator.setVisible(false);
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        populateFields();
    }

    private void populateFields() {
        if (currentUser != null) {
            nameField.setText(currentUser.getName());
            emailField.setText(currentUser.getEmail());

            // Set optional fields if they exist
            if (currentUser.getWalletAddress() != null) {
                walletField.setText(currentUser.getWalletAddress());
            }

            if (currentUser.getGithubUsername() != null) {
                githubField.setText(currentUser.getGithubUsername());
            }

            // Load profile picture
            loadProfilePicture();
        }
    }

    private void loadProfilePicture() {
        try {
            String profilePicPath = currentUser.getProfilePicture();
            if (profilePicPath == null || profilePicPath.isEmpty()) {
                // If null or empty, use default image
                profilePicPath = "/assets/default/default_profile.jpg";
            }

            Image profileImage;
            
            // Check if the profile picture is a URL (GitHub avatar) or a local resource
            if (profilePicPath.startsWith("http://") || profilePicPath.startsWith("https://")) {
                // For remote URLs (like GitHub avatars)
                profileImage = new Image(profilePicPath);
                System.out.println("Loading remote profile image from: " + profilePicPath);
            } else {
                // For local resource paths
                profileImage = new Image(getClass().getResourceAsStream(profilePicPath));
                System.out.println("Loading local profile image from: " + profilePicPath);
            }
            
            profileImageView.setImage(profileImage);

            // Apply styling to make the image circular
            profileImageView.setStyle(
                    "-fx-background-radius: 50%; " +
                            "-fx-background-color: white; " +
                            "-fx-border-radius: 50%; " +
                            "-fx-border-color: #dddddd; " +
                            "-fx-border-width: 2px;");

        } catch (Exception e) {
            System.err.println("Error loading profile picture: " + e.getMessage());
            // Load default image on error
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/assets/default/default_profile.jpg"));
                profileImageView.setImage(defaultImage);
            } catch (Exception ex) {
                System.err.println("Error loading default profile picture: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleChangeProfilePic(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Store the selected file for later use when saving
                selectedProfilePicFile = selectedFile;

                // Just display the selected image temporarily
                Image newImage = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(newImage);

                showStatus("Profile picture selected. Click Save Changes to update.");
            } catch (Exception e) {
                showStatus("Error processing image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String walletAddress = walletField.getText().trim();
        String githubUsername = githubField.getText().trim();

        try {
            // Create a copy of the current user for validation
            final User updatedUser = new User();
            updatedUser.setId(currentUser.getId());
            updatedUser.setName(name);
            updatedUser.setEmail(email);
            updatedUser.setWalletAddress(walletAddress.isEmpty() ? null : walletAddress);
            updatedUser.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
            
            // Set other fields from current user to avoid losing data
            updatedUser.setRoles(currentUser.getRoles());
            updatedUser.setBalance(currentUser.getBalance());
            updatedUser.setCreatedAt(currentUser.getCreatedAt());
            updatedUser.setProfilePicture(currentUser.getProfilePicture());
            
            // Password handling - special case that requires controller-level validation
            final boolean[] passwordChanged = {false}; // Use array to make it effectively final
            // Check if password fields exist in the form
            if (newPassword != null && currentPassword != null && confirmPassword != null) {
                if (!newPassword.getText().isEmpty()) {
                    // Verify current password using BCrypt
                    if (!PasswordHasher.verifyPassword(currentPassword.getText(), currentUser.getPassword())) {
                        showStatus("Current password is incorrect.");
                        return;
                    }

                    // Check if new passwords match
                    if (!newPassword.getText().equals(confirmPassword.getText())) {
                        showStatus("New passwords do not match.");
                        return;
                    }

                    // Set the new password (hashed) for validation
                    updatedUser.setPassword(PasswordHasher.hashPassword(newPassword.getText()));
                    passwordChanged[0] = true;
                } else {
                    // Skip password validation by setting a placeholder
                    // We'll restore the actual password after validation
                    updatedUser.setPassword("placeholder");
                }
            } else {
                // Password fields don't exist in this version of the form
                updatedUser.setPassword("placeholder");
            }

            // Check if the email is being changed and if it's already in use
            if (!email.equals(currentUser.getEmail())) {
                User existingUser = userService.getByEmail(email);
                if (existingUser != null && existingUser.getId() != currentUser.getId()) {
                    showStatus("This email is already in use by another account.");
                    return;
                }
            }
            
            // Validate basic fields manually instead of using the full User validation
            if (name.isEmpty()) {
                showStatus("Name cannot be blank");
                return;
            }
            
            if (email.isEmpty()) {
                showStatus("Email cannot be blank");
                return;
            }
            
            // Validate email format with a regex pattern
            String emailRegex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
            if (!Pattern.compile(emailRegex).matcher(email).matches()) {
                showStatus("Invalid email format");
                return;
            }
            
            // Validate GitHub username if provided
            if (!githubUsername.isEmpty()) {
                // GitHub username validation (alphanumeric with hyphens, no consecutive hyphens)
                String githubRegex = "^[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$";
                if (!Pattern.compile(githubRegex).matcher(githubUsername).matches()) {
                    showStatus("Invalid GitHub username format");
                    return;
                }
            }

            // Update the user with validated data
            currentUser.setName(name);
            currentUser.setEmail(email);
            currentUser.setWalletAddress(walletAddress.isEmpty() ? null : walletAddress);
            currentUser.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
            
            // Process and save profile picture if one was selected
            if (selectedProfilePicFile != null) {
                // Try to upload to Imgur if configured
                if (imgurService.isConfigured()) {
                    // Show progress indicator
                    if (uploadProgressIndicator != null) {
                        uploadProgressIndicator.setVisible(true);
                    }
                    
                    // Disable save button during upload
                    ((Node) event.getSource()).setDisable(true);
                    
                    showStatus("Uploading image to Imgur...");
                    
                    // Create a background task for uploading
                    Task<String> uploadTask = new Task<String>() {
                        @Override
                        protected String call() throws Exception {
                            return imgurService.uploadImage(selectedProfilePicFile);
                        }
                    };
                    
                    // Handle success
                    uploadTask.setOnSucceeded(e -> {
                        String imgurUrl = uploadTask.getValue();
                        currentUser.setProfilePicture(imgurUrl);
                        
                        // Save user and return to profile page
                        saveUserAndReturn(event, passwordChanged[0], updatedUser);
                    });
                    
                    // Handle failure
                    uploadTask.setOnFailed(e -> {
                        Platform.runLater(() -> {
                            // Hide progress indicator
                            if (uploadProgressIndicator != null) {
                                uploadProgressIndicator.setVisible(false);
                            }
                            
                            // Re-enable save button
                            ((Node) event.getSource()).setDisable(false);
                            
                            // Fall back to local storage
                            showStatus("Failed to upload to Imgur. Falling back to local storage.");
                            saveProfilePictureLocally(event, passwordChanged[0], updatedUser);
                        });
                    });
                    
                    // Start the task
                    new Thread(uploadTask).start();
                    return; // Exit the method here since we're handling the save asynchronously
                } else {
                    // No Imgur configuration, save locally
                    saveProfilePictureLocally(event, passwordChanged[0], updatedUser);
                    return; // Exit after local save is initiated
                }
            }

            // No profile picture to process, just save user data
            saveUserAndReturn(event, passwordChanged[0], updatedUser);
        } catch (Exception e) {
            showStatus("Error updating profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToProfile(event);
    }

    private void navigateToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();

            // Pass the user to the profile controller
            ProfileController controller = loader.getController();
            controller.setUser(currentUser);

            // If we just updated the profile, show a success message
            if (statusLabel.isVisible() && !statusLabel.getText().startsWith("Error")) {
                controller.refreshUserData();
            }

            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();

            stage.setScene(new Scene(profileView, 800, 600));
            stage.show();
        } catch (IOException e) {
            showStatus("Error navigating back to profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }

    private void saveProfilePictureLocally(ActionEvent event, boolean passwordChanged, User updatedUser) {
        try {
            // Create user-specific filename to avoid conflicts
            String fileName = "user_" + currentUser.getId() + "_" + selectedProfilePicFile.getName();

            // Path to the uploads directory
            Path uploadsDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "uploads");

            // Create directory if it doesn't exist
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
            }

            // Copy file to uploads directory
            Path targetPath = uploadsDir.resolve(fileName);
            Files.copy(selectedProfilePicFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Save the relative path to be stored in database
            currentUser.setProfilePicture("/uploads/" + fileName);
            
            // Save user and return to profile page
            saveUserAndReturn(event, passwordChanged, updatedUser);
        } catch (IOException e) {
            // Show error and re-enable UI
            showStatus("Error saving profile picture: " + e.getMessage());
            e.printStackTrace();
            if (uploadProgressIndicator != null) {
                uploadProgressIndicator.setVisible(false);
            }
            ((Node) event.getSource()).setDisable(false);
        }
    }
    
    private void saveUserAndReturn(ActionEvent event, boolean passwordChanged, User updatedUser) {
        try {
            // Update the current user with validated data
            currentUser.setName(updatedUser.getName());
            currentUser.setEmail(updatedUser.getEmail());
            currentUser.setWalletAddress(updatedUser.getWalletAddress());
            currentUser.setGithubUsername(updatedUser.getGithubUsername());
            
            // Only update password if it was changed
            if (passwordChanged) {
                currentUser.setPassword(updatedUser.getPassword());
            }

            // Save changes to database
            userService.update(currentUser);

            // Return to profile page with updated user
            Platform.runLater(() -> navigateToProfile(event));
        } catch (Exception e) {
            Platform.runLater(() -> {
                showStatus("Error updating profile: " + e.getMessage());
                e.printStackTrace();
                
                // Hide progress indicator
                if (uploadProgressIndicator != null) {
                    uploadProgressIndicator.setVisible(false);
                }
                
                // Re-enable save button
                ((Node) event.getSource()).setDisable(false);
            });
        }
    }
}