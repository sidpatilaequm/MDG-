package com.example.multimedia.file_upload_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_requisition_item_vendors")
public class PurchaseRequisitionItemVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_requisition_item_id", nullable = false)
    private Long purchaseRequisitionItemId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "bp_no", length = 255)
    private String bpNo;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPurchaseRequisitionItemId() {
        return purchaseRequisitionItemId;
    }

    public void setPurchaseRequisitionItemId(Long purchaseRequisitionItemId) {
        this.purchaseRequisitionItemId = purchaseRequisitionItemId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getBpNo() {
        return bpNo;
    }

    public void setBpNo(String bpNo) {
        this.bpNo = bpNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
