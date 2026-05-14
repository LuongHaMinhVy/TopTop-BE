package com.back.notification.repo;

import com.back.notification.model.entity.Notification;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface INotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.actor " +
            "LEFT JOIN FETCH n.video " +
            "WHERE n.recipient = :recipient " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientWithDetails(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);
}
