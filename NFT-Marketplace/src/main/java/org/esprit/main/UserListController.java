package org.esprit.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class UserListController implements Initializable {

    @FXML
    private TableView<User> userTableView;
    
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
    private TableColumn<User, String> walletAddressColumn;
    
    @FXML
    private TableColumn<User, LocalDateTime> createdAtColumn;

    private UserService userService;
    private ObservableList<User> userList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        userList = FXCollections.observableArrayList();
        
        // Configure table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        rolesColumn.setCellValueFactory(cellData -> {
            List<String> roles = cellData.getValue().getRoles();
            String roleStr = roles != null ? String.join(", ", roles) : "";
            return javafx.beans.binding.Bindings.createStringBinding(() -> roleStr);
        });
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        walletAddressColumn.setCellValueFactory(new PropertyValueFactory<>("walletAddress"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Format date cell
        createdAtColumn.setCellFactory(column -> new javafx.scene.control.TableCell<User, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        
        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAll();
            userList.setAll(users);
            userTableView.setItems(userList);
        } catch (Exception e) {
            e.printStackTrace();
            // In a real application, you'd show an error dialog here
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    @FXML
    private void refreshUserList() {
        loadUsers();
    }
}