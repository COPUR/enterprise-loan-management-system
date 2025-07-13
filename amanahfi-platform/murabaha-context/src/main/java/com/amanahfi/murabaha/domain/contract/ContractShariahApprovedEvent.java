package com.amanahfi.murabaha.domain.contract;

import java.time.LocalDateTime;

public class ContractShariahApprovedEvent {
    private final String contractId;
    private final String boardMemberId;
    private final LocalDateTime timestamp;

    public ContractShariahApprovedEvent(String contractId, String boardMemberId) {
        this.contractId = contractId;
        this.boardMemberId = boardMemberId;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public String getBoardMemberId() { return boardMemberId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}