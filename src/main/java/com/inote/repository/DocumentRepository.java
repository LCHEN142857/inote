package com.inote.repository;

import com.inote.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    Optional<Document> findByIdAndOwnerId(String id, String ownerId);

    List<Document> findAllByOwnerIdAndFileNameOrderByUpdatedAtDesc(String ownerId, String fileName);

    List<Document> findAllByIdInAndOwnerId(List<String> ids, String ownerId);

    List<Document> findAllByIdInAndOwnerIdAndActive(List<String> ids, String ownerId, Boolean active);
}
