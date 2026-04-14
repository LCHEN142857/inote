package com.inote.repository;

import com.inote.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findAllByOrderByUpdatedAtDesc();
}
