package com.fsf.habitup.DTO;

import com.fsf.habitup.entity.Documents;

public class DocumentDTO {

    private Long id;
    private String documentName;
    private String documentType;
    private String documentUrl;
    private Long userId;

    public static DocumentDTO fromEntity(Documents document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setDocumentName(document.getDocumentName());
        dto.setDocumentType(document.getDocumentType());
        dto.setDocumentUrl(document.getDocumentUrl());
        dto.setUserId(document.getUser().getUserId());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
