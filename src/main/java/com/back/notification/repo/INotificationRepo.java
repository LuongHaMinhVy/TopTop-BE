package com.back.notification.repo;

import com.back.notification.model.entity.Notification;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface INotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    
    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.actor " +
            "LEFT JOIN FETCH n.video v " +
            "LEFT JOIN FETCH v.user " +
            "WHERE n.recipient = :recipient " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientWithDetails(User recipient);

    @Query(value = "SELECT n FROM Notification n " +
            "JOIN FETCH n.actor " +
            "LEFT JOIN FETCH n.video v " +
            "LEFT JOIN FETCH v.user " +
            "WHERE n.recipient = :recipient " +
            "ORDER BY n.createdAt DESC",
            countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.recipient = :recipient")
    Page<Notification> findByRecipientWithDetails(@Param("recipient") User recipient, Pageable pageable);

    long countByRecipientAndIsReadFalse(User recipient);
}
