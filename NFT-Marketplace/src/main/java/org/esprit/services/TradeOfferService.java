package org.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.esprit.models.TradeOffer;
import org.esprit.models.User;
import org.esprit.models.Artwork;
import org.esprit.utils.DatabaseConnection;

public class TradeOfferService implements IService<TradeOffer> {
    private final Connection connection;
    private final UserService userService;
    private final ArtworkService artworkService;

    public TradeOfferService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
        artworkService = new ArtworkService();
    }

    @Override
    public void add(TradeOffer tradeOffer) throws Exception {
        String sql = "INSERT INTO trade_offer (sender, receiver_name, offered_item, received_item, description, creation_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, tradeOffer.getSender().getId());
            stmt.setInt(2, tradeOffer.getReceiverName().getId());
            stmt.setInt(3, tradeOffer.getOfferedItem().getId());
            stmt.setInt(4, tradeOffer.getReceivedItem().getId());
            stmt.setString(5, tradeOffer.getDescription());
            stmt.setTimestamp(6, Timestamp.valueOf(tradeOffer.getCreationDate()));
            stmt.setString(7, tradeOffer.getStatus());
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    tradeOffer.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(TradeOffer tradeOffer) throws Exception {
        String sql = "UPDATE trade_offer SET sender = ?, receiver_name = ?, offered_item = ?, " +
                     "received_item = ?, description = ?, status = ? WHERE id = ?";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, tradeOffer.getSender().getId());
            stmt.setInt(2, tradeOffer.getReceiverName().getId());
            stmt.setInt(3, tradeOffer.getOfferedItem().getId());
            stmt.setInt(4, tradeOffer.getReceivedItem().getId());
            stmt.setString(5, tradeOffer.getDescription());
            stmt.setString(6, tradeOffer.getStatus());
            stmt.setInt(7, tradeOffer.getId());
            
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(TradeOffer tradeOffer) throws Exception {
        String sql = "DELETE FROM trade_offer WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, tradeOffer.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<TradeOffer> getAll() throws Exception {
        List<TradeOffer> tradeOffers = new ArrayList<>();
        String sql = "SELECT * FROM trade_offer";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                tradeOffers.add(mapResultSetToTradeOffer(rs));
            }
        }
        
        return tradeOffers;
    }

    @Override
    public TradeOffer getOne(int id) throws Exception {
        String sql = "SELECT * FROM trade_offer WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTradeOffer(rs);
                }
            }
        }
        
        return null;
    }

    // Helper method used by controller
    public List<TradeOffer> getAllTradeOffers() throws SQLException {
        List<TradeOffer> tradeOffers = new ArrayList<>();
        String query = "SELECT t.*, " +
                      "s.id as sender_id, s.name as sender_name, " +
                      "r.id as receiver_id, r.name as receiver_name, " +
                      "o.id as offered_id, o.title as offered_title, " +
                      "rc.id as received_id, rc.title as received_title " +
                      "FROM trade_offer t " +
                      "JOIN user s ON t.sender = s.id " +
                      "JOIN user r ON t.receiver_name = r.id " +
                      "JOIN artwork o ON t.offered_item = o.id " +
                      "JOIN artwork rc ON t.received_item = rc.id";
                      
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                TradeOffer trade = new TradeOffer();
                trade.setId(rs.getInt("id"));
                
                // Set sender
                User sender = new User();
                sender.setId(rs.getInt("sender_id"));
                sender.setName(rs.getString("sender_name"));
                trade.setSender(sender);
                
                // Set receiver
                User receiver = new User();
                receiver.setId(rs.getInt("receiver_id"));
                receiver.setName(rs.getString("receiver_name"));
                trade.setReceiverName(receiver);
                
                // Set offered artwork
                Artwork offeredArtwork = new Artwork();
                offeredArtwork.setId(rs.getInt("offered_id"));
                offeredArtwork.setTitle(rs.getString("offered_title"));
                trade.setOfferedItem(offeredArtwork);
                
                // Set received artwork
                Artwork receivedArtwork = new Artwork();
                receivedArtwork.setId(rs.getInt("received_id"));
                receivedArtwork.setTitle(rs.getString("received_title"));
                trade.setReceivedItem(receivedArtwork);
                
                trade.setDescription(rs.getString("description"));
                trade.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                trade.setStatus(rs.getString("status"));
                
                tradeOffers.add(trade);
            }
        }
        
        return tradeOffers;
    }

    public void updateTradeOffer(TradeOffer trade) {
        try {
            update(trade);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTradeStatus(int tradeId, String newStatus) throws SQLException {
        String query = "UPDATE trade_offer SET status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, tradeId);
            pstmt.executeUpdate();
        }
    }

    private TradeOffer mapResultSetToTradeOffer(ResultSet rs) throws Exception {
        TradeOffer tradeOffer = new TradeOffer();
        tradeOffer.setId(rs.getInt("id"));
        
        User sender = userService.getOne(rs.getInt("sender"));
        tradeOffer.setSender(sender);
        
        User receiver = userService.getOne(rs.getInt("receiver_name"));
        tradeOffer.setReceiverName(receiver);
        
        Artwork offeredItem = artworkService.getOne(rs.getInt("offered_item"));
        tradeOffer.setOfferedItem(offeredItem);
        
        Artwork receivedItem = artworkService.getOne(rs.getInt("received_item"));
        tradeOffer.setReceivedItem(receivedItem);
        
        tradeOffer.setDescription(rs.getString("description"));
        tradeOffer.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        tradeOffer.setStatus(rs.getString("status"));
        
        return tradeOffer;
    }
}