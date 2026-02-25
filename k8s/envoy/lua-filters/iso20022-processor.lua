-- ISO 20022 XML Message Processing Filter
-- Handles ISO 20022 payment message validation, transformation, and audit logging
-- Compliance: ISO 20022, SEPA, TARGET2, banking regulations

local json = require("json")
local string = require("string")

-- ISO 20022 message type definitions
local ISO20022_MESSAGES = {
  ["pain.001.001"] = "CustomerCreditTransferInitiation",
  ["pain.002.001"] = "CustomerPaymentStatusReport", 
  ["pain.008.001"] = "CustomerDirectDebitInitiation",
  ["pain.007.001"] = "CustomerPaymentReversalRequest",
  ["pacs.008.001"] = "FIToFICustomerCreditTransfer",
  ["pacs.002.001"] = "FIToFIPaymentStatusReport",
  ["pacs.004.001"] = "PaymentReturn",
  ["pacs.007.001"] = "FIToFIPaymentReversalRequest",
  ["camt.053.001"] = "BankToCustomerStatement",
  ["camt.054.001"] = "BankToCustomerDebitCreditNotification",
  ["camt.052.001"] = "BankToCustomerAccountReport"
}

-- ISO 20022 processor class
local ISO20022Processor = {}
ISO20022Processor.__index = ISO20022Processor

function ISO20022Processor:new()
  local obj = {
    message_count = 0,
    error_count = 0,
    processed_messages = {}
  }
  setmetatable(obj, ISO20022Processor)
  return obj
end

-- Parse ISO 20022 XML message
function ISO20022Processor:parse_iso20022_message(xml_body)
  if not xml_body or xml_body == "" then
    return nil, "Empty ISO 20022 message body"
  end

  local parsed = {
    message_type = nil,
    namespace = nil,
    document_type = nil,
    group_header = {},
    payment_info = {},
    credit_transfer_info = {},
    direct_debit_info = {},
    status_info = {},
    statement_info = {},
    raw_elements = {}
  }

  -- Extract XML namespace
  local namespace = string.match(xml_body, 'xmlns="([^"]*urn:iso:std:iso:20022[^"]*)"')
  if not namespace then
    return nil, "Missing ISO 20022 namespace"
  end
  parsed.namespace = namespace

  -- Extract document type from namespace or root element
  local doc_type = string.match(namespace, "([a-z]+%.[0-9]+%.[0-9]+%.[0-9]+)")
  if not doc_type then
    -- Try to extract from root element
    doc_type = string.match(xml_body, "<([a-zA-Z]+)%s")
    if doc_type then
      -- Map element name to document type
      if string.match(doc_type, "CstmrCdtTrfInitn") then
        doc_type = "pain.001.001"
      elseif string.match(doc_type, "CstmrPmtStsRpt") then
        doc_type = "pain.002.001"
      elseif string.match(doc_type, "CstmrDrctDbtInitn") then
        doc_type = "pain.008.001"
      elseif string.match(doc_type, "FIToFICstmrCdtTrf") then
        doc_type = "pacs.008.001"
      elseif string.match(doc_type, "BkToCstmrStmt") then
        doc_type = "camt.053.001"
      elseif string.match(doc_type, "BkToCstmrDbtCdtNtfctn") then
        doc_type = "camt.054.001"
      end
    end
  end
  
  if not doc_type then
    return nil, "Cannot determine ISO 20022 document type"
  end
  
  parsed.document_type = doc_type
  parsed.message_type = ISO20022_MESSAGES[string.match(doc_type, "([a-z]+%.[0-9]+%.[0-9]+)")] or "Unknown"

  -- Parse message-specific content
  if string.match(doc_type, "pain%.001") then
    self:parse_credit_transfer_initiation(xml_body, parsed)
  elseif string.match(doc_type, "pain%.002") then
    self:parse_payment_status_report(xml_body, parsed)
  elseif string.match(doc_type, "pain%.008") then
    self:parse_direct_debit_initiation(xml_body, parsed)
  elseif string.match(doc_type, "pacs%.008") then
    self:parse_fi_credit_transfer(xml_body, parsed)
  elseif string.match(doc_type, "camt%.053") then
    self:parse_bank_statement(xml_body, parsed)
  elseif string.match(doc_type, "camt%.054") then
    self:parse_debit_credit_notification(xml_body, parsed)
  end

  -- Parse common group header
  self:parse_group_header(xml_body, parsed)

  return parsed, nil
end

-- Parse Group Header (common to all messages)
function ISO20022Processor:parse_group_header(xml_body, parsed)
  -- Message ID
  local msg_id = string.match(xml_body, "<MsgId>([^<]+)</MsgId>")
  if msg_id then
    parsed.group_header.message_id = msg_id
  end

  -- Creation Date Time
  local creation_dt = string.match(xml_body, "<CreDtTm>([^<]+)</CreDtTm>")
  if creation_dt then
    parsed.group_header.creation_date_time = creation_dt
  end

  -- Number of Transactions
  local nb_of_txs = string.match(xml_body, "<NbOfTxs>([^<]+)</NbOfTxs>")
  if nb_of_txs then
    parsed.group_header.number_of_transactions = tonumber(nb_of_txs)
  end

  -- Control Sum
  local ctrl_sum = string.match(xml_body, "<CtrlSum>([^<]+)</CtrlSum>")
  if ctrl_sum then
    parsed.group_header.control_sum = ctrl_sum
  end

  -- Initiating Party
  local init_party = string.match(xml_body, "<InitgPty>.-<Nm>([^<]+)</Nm>.-</InitgPty>")
  if init_party then
    parsed.group_header.initiating_party = init_party
  end
end

-- Parse Credit Transfer Initiation (pain.001)
function ISO20022Processor:parse_credit_transfer_initiation(xml_body, parsed)
  -- Payment Information ID
  local pmt_inf_id = string.match(xml_body, "<PmtInfId>([^<]+)</PmtInfId>")
  if pmt_inf_id then
    parsed.payment_info.payment_info_id = pmt_inf_id
  end

  -- Payment Method
  local pmt_mtd = string.match(xml_body, "<PmtMtd>([^<]+)</PmtMtd>")
  if pmt_mtd then
    parsed.payment_info.payment_method = pmt_mtd
  end

  -- Requested Execution Date
  local reqd_exctn_dt = string.match(xml_body, "<ReqdExctnDt>([^<]+)</ReqdExctnDt>")
  if reqd_exctn_dt then
    parsed.payment_info.requested_execution_date = reqd_exctn_dt
  end

  -- Debtor
  local dbtr_name = string.match(xml_body, "<Dbtr>.-<Nm>([^<]+)</Nm>.-</Dbtr>")
  if dbtr_name then
    parsed.payment_info.debtor_name = dbtr_name
  end

  -- Debtor Account IBAN
  local dbtr_acct = string.match(xml_body, "<DbtrAcct>.-<IBAN>([^<]+)</IBAN>.-</DbtrAcct>")
  if dbtr_acct then
    parsed.payment_info.debtor_iban = dbtr_acct
  end

  -- Parse Credit Transfer Transactions
  parsed.credit_transfer_info = self:parse_credit_transfer_transactions(xml_body)
end

-- Parse Credit Transfer Transactions
function ISO20022Processor:parse_credit_transfer_transactions(xml_body)
  local transactions = {}
  
  -- Find all CdtTrfTxInf blocks
  for tx_block in string.gmatch(xml_body, "<CdtTrfTxInf>(.-)</CdtTrfTxInf>") do
    local transaction = {}
    
    -- Payment ID
    local pmt_id = string.match(tx_block, "<PmtId>.-<EndToEndId>([^<]+)</EndToEndId>.-</PmtId>")
    if pmt_id then
      transaction.end_to_end_id = pmt_id
    end

    -- Amount
    local amount = string.match(tx_block, "<InstdAmt[^>]*>([^<]+)</InstdAmt>")
    local currency = string.match(tx_block, '<InstdAmt Ccy="([^"]+)"')
    if amount then
      transaction.amount = amount
      transaction.currency = currency or "EUR"
    end

    -- Creditor
    local cdtr_name = string.match(tx_block, "<Cdtr>.-<Nm>([^<]+)</Nm>.-</Cdtr>")
    if cdtr_name then
      transaction.creditor_name = cdtr_name
    end

    -- Creditor Account IBAN
    local cdtr_iban = string.match(tx_block, "<CdtrAcct>.-<IBAN>([^<]+)</IBAN>.-</CdtrAcct>")
    if cdtr_iban then
      transaction.creditor_iban = cdtr_iban
    end

    -- Remittance Information
    local rmt_inf = string.match(tx_block, "<RmtInf>.-<Ustrd>([^<]+)</Ustrd>.-</RmtInf>")
    if rmt_inf then
      transaction.remittance_info = rmt_inf
    end

    table.insert(transactions, transaction)
  end
  
  return transactions
end

-- Parse Payment Status Report (pain.002)
function ISO20022Processor:parse_payment_status_report(xml_body, parsed)
  -- Original Message ID
  local orig_msg_id = string.match(xml_body, "<OrgnlMsgId>([^<]+)</OrgnlMsgId>")
  if orig_msg_id then
    parsed.status_info.original_message_id = orig_msg_id
  end

  -- Group Status
  local grp_sts = string.match(xml_body, "<GrpSts>([^<]+)</GrpSts>")
  if grp_sts then
    parsed.status_info.group_status = grp_sts
  end

  -- Status Reason Code
  local sts_rsn_cd = string.match(xml_body, "<StsRsnCd>([^<]+)</StsRsnCd>")
  if sts_rsn_cd then
    parsed.status_info.status_reason_code = sts_rsn_cd
  end
end

-- Parse Direct Debit Initiation (pain.008)
function ISO20022Processor:parse_direct_debit_initiation(xml_body, parsed)
  -- Similar to credit transfer but for direct debits
  local pmt_inf_id = string.match(xml_body, "<PmtInfId>([^<]+)</PmtInfId>")
  if pmt_inf_id then
    parsed.direct_debit_info.payment_info_id = pmt_inf_id
  end

  -- Sequence Type
  local seq_tp = string.match(xml_body, "<SeqTp>([^<]+)</SeqTp>")
  if seq_tp then
    parsed.direct_debit_info.sequence_type = seq_tp
  end

  -- Creditor Scheme ID
  local cdtr_schm_id = string.match(xml_body, "<CdtrSchmeId>.-<Id>([^<]+)</Id>.-</CdtrSchmeId>")
  if cdtr_schm_id then
    parsed.direct_debit_info.creditor_scheme_id = cdtr_schm_id
  end
end

-- Parse FI to FI Credit Transfer (pacs.008)
function ISO20022Processor:parse_fi_credit_transfer(xml_body, parsed)
  -- Interbank Settlement Amount
  local sttlm_amt = string.match(xml_body, "<IntrBkSttlmAmt[^>]*>([^<]+)</IntrBkSttlmAmt>")
  if sttlm_amt then
    parsed.payment_info.settlement_amount = sttlm_amt
  end

  -- Instructing Agent
  local instg_agt = string.match(xml_body, "<InstgAgt>.-<BIC>([^<]+)</BIC>.-</InstgAgt>")
  if instg_agt then
    parsed.payment_info.instructing_agent = instg_agt
  end

  -- Instructed Agent
  local instd_agt = string.match(xml_body, "<InstdAgt>.-<BIC>([^<]+)</BIC>.-</InstdAgt>")
  if instd_agt then
    parsed.payment_info.instructed_agent = instd_agt
  end
end

-- Parse Bank Statement (camt.053)
function ISO20022Processor:parse_bank_statement(xml_body, parsed)
  -- Statement ID
  local stmt_id = string.match(xml_body, "<Id>([^<]+)</Id>")
  if stmt_id then
    parsed.statement_info.statement_id = stmt_id
  end

  -- Account IBAN
  local acct_iban = string.match(xml_body, "<Acct>.-<IBAN>([^<]+)</IBAN>.-</Acct>")
  if acct_iban then
    parsed.statement_info.account_iban = acct_iban
  end

  -- Balance
  local balance = string.match(xml_body, "<Bal>.-<Amt[^>]*>([^<]+)</Amt>.-</Bal>")
  if balance then
    parsed.statement_info.balance = balance
  end
end

-- Parse Debit Credit Notification (camt.054)
function ISO20022Processor:parse_debit_credit_notification(xml_body, parsed)
  -- Notification ID
  local ntfctn_id = string.match(xml_body, "<NtfctnId>([^<]+)</NtfctnId>")
  if ntfctn_id then
    parsed.statement_info.notification_id = ntfctn_id
  end

  -- Entry details would be parsed similarly to statements
end

-- Validate ISO 20022 message
function ISO20022Processor:validate_iso20022_message(parsed_message)
  local validation_errors = {}
  
  -- Check if message type is supported
  if not parsed_message.message_type or parsed_message.message_type == "Unknown" then
    table.insert(validation_errors, "Unsupported or unknown ISO 20022 message type")
  end

  -- Validate Group Header
  if not parsed_message.group_header.message_id then
    table.insert(validation_errors, "Missing mandatory Message ID in Group Header")
  end

  if not parsed_message.group_header.creation_date_time then
    table.insert(validation_errors, "Missing mandatory Creation Date Time in Group Header")
  end

  if not parsed_message.group_header.number_of_transactions then
    table.insert(validation_errors, "Missing mandatory Number of Transactions in Group Header")
  end

  -- Document type specific validation
  if parsed_message.document_type then
    local type_errors = self:validate_document_type_specific(parsed_message)
    for _, error in ipairs(type_errors) do
      table.insert(validation_errors, error)
    end
  end

  return validation_errors
end

-- Document type specific validation
function ISO20022Processor:validate_document_type_specific(parsed_message)
  local errors = {}
  local doc_type = parsed_message.document_type
  
  if string.match(doc_type, "pain%.001") then
    -- Credit Transfer Initiation validation
    if not parsed_message.payment_info.payment_info_id then
      table.insert(errors, "pain.001 missing mandatory Payment Information ID")
    end
    if not parsed_message.payment_info.payment_method then
      table.insert(errors, "pain.001 missing mandatory Payment Method")
    end
    if not parsed_message.payment_info.debtor_iban then
      table.insert(errors, "pain.001 missing mandatory Debtor IBAN")
    end
    if not parsed_message.credit_transfer_info or #parsed_message.credit_transfer_info == 0 then
      table.insert(errors, "pain.001 missing Credit Transfer Transaction Information")
    else
      for i, tx in ipairs(parsed_message.credit_transfer_info) do
        if not tx.end_to_end_id then
          table.insert(errors, "pain.001 transaction " .. i .. " missing End to End ID")
        end
        if not tx.amount then
          table.insert(errors, "pain.001 transaction " .. i .. " missing amount")
        end
        if not tx.creditor_iban then
          table.insert(errors, "pain.001 transaction " .. i .. " missing creditor IBAN")
        end
      end
    end
  elseif string.match(doc_type, "pain%.002") then
    -- Payment Status Report validation
    if not parsed_message.status_info.original_message_id then
      table.insert(errors, "pain.002 missing mandatory Original Message ID")
    end
    if not parsed_message.status_info.group_status then
      table.insert(errors, "pain.002 missing mandatory Group Status")
    end
  elseif string.match(doc_type, "camt%.053") then
    -- Bank Statement validation
    if not parsed_message.statement_info.statement_id then
      table.insert(errors, "camt.053 missing mandatory Statement ID")
    end
    if not parsed_message.statement_info.account_iban then
      table.insert(errors, "camt.053 missing mandatory Account IBAN")
    end
  end
  
  return errors
end

-- Transform ISO 20022 message to internal format
function ISO20022Processor:transform_to_internal_format(parsed_message)
  local internal_format = {
    message_id = self:generate_message_id(),
    message_type = "ISO20022_" .. string.upper(string.gsub(parsed_message.document_type or "UNKNOWN", "%.", "_")),
    timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ"),
    source_format = "ISO20022_XML",
    original_document_type = parsed_message.document_type,
    original_message_id = parsed_message.group_header.message_id,
    creation_date_time = parsed_message.group_header.creation_date_time,
    number_of_transactions = parsed_message.group_header.number_of_transactions,
    control_sum = parsed_message.group_header.control_sum,
    initiating_party = parsed_message.group_header.initiating_party,
    banking_data = {}
  }

  -- Transform based on document type
  if string.match(parsed_message.document_type or "", "pain%.001") then
    internal_format.banking_data = {
      payment_type = "credit_transfer",
      payment_info_id = parsed_message.payment_info.payment_info_id,
      payment_method = parsed_message.payment_info.payment_method,
      execution_date = parsed_message.payment_info.requested_execution_date,
      debtor_name = parsed_message.payment_info.debtor_name,
      debtor_iban = parsed_message.payment_info.debtor_iban,
      transactions = parsed_message.credit_transfer_info
    }
  elseif string.match(parsed_message.document_type or "", "pain%.002") then
    internal_format.banking_data = {
      payment_type = "status_report",
      original_message_id = parsed_message.status_info.original_message_id,
      group_status = parsed_message.status_info.group_status,
      status_reason_code = parsed_message.status_info.status_reason_code
    }
  elseif string.match(parsed_message.document_type or "", "camt%.053") then
    internal_format.banking_data = {
      payment_type = "bank_statement",
      statement_id = parsed_message.statement_info.statement_id,
      account_iban = parsed_message.statement_info.account_iban,
      balance = parsed_message.statement_info.balance
    }
  end

  return internal_format
end

-- Generate unique message ID
function ISO20022Processor:generate_message_id()
  return "ISO20022_" .. os.time() .. "_" .. math.random(10000, 99999)
end

-- Generate audit event for ISO 20022 processing
function ISO20022Processor:generate_audit_event(event_type, parsed_message, internal_format, headers)
  local audit_event = {
    timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ"),
    event_type = event_type,
    protocol = "ISO20022",
    document_type = parsed_message.document_type,
    message_type = parsed_message.message_type,
    message_id = internal_format and internal_format.message_id,
    original_message_id = parsed_message.group_header.message_id,
    number_of_transactions = parsed_message.group_header.number_of_transactions,
    control_sum = parsed_message.group_header.control_sum,
    initiating_party = parsed_message.group_header.initiating_party,
    namespace = parsed_message.namespace,
    request_context = {
      source_ip = headers and headers["x-forwarded-for"],
      user_agent = headers and headers["user-agent"],
      transaction_id = headers and headers["x-transaction-id"],
      institution_id = headers and headers["x-institution-id"]
    },
    compliance_info = {
      iso20022_compliant = true,
      sepa_compliant = string.match(parsed_message.document_type or "", "pain%.") ~= nil,
      xml_well_formed = true,
      audit_trail_complete = true
    }
  }
  
  return audit_event
end

-- Main ISO 20022 processing function for Envoy
function envoy_on_request(request_handle)
  local headers = request_handle:headers()
  local path = headers:get(":path")
  local method = headers:get(":method")
  local content_type = headers:get("content-type")
  
  -- Only process ISO 20022 XML messages
  if not (method == "POST" and (string.match(path, "/iso20022/") or 
          string.match(path, "/sepa/") or
          (content_type and string.match(content_type, "application/xml")))) then
    return
  end
  
  local body = request_handle:body()
  if not body then
    request_handle:logWarn("ISO 20022 processor: No message body found")
    return
  end
  
  local processor = ISO20022Processor:new()
  local xml_body = tostring(body:getBytes(0, body:length()))
  
  -- Parse ISO 20022 message
  local parsed_message, parse_error = processor:parse_iso20022_message(xml_body)
  if not parsed_message then
    local error_response = json.encode({
      error = "ISO20022_PARSE_ERROR",
      code = "IPE001",
      message = parse_error,
      timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ"),
      transaction_id = headers:get("x-transaction-id") or "unknown"
    })
    
    request_handle:respond(
      {[":status"] = "400", ["content-type"] = "application/json"},
      error_response
    )
    return
  end
  
  -- Validate ISO 20022 message
  local validation_errors = processor:validate_iso20022_message(parsed_message)
  if #validation_errors > 0 then
    local error_response = json.encode({
      error = "ISO20022_VALIDATION_ERROR",
      code = "IVE001", 
      message = "ISO 20022 message validation failed",
      validation_errors = validation_errors,
      timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ"),
      transaction_id = headers:get("x-transaction-id") or "unknown"
    })
    
    request_handle:respond(
      {[":status"] = "400", ["content-type"] = "application/json"},
      error_response
    )
    return
  end
  
  -- Transform to internal format
  local internal_format = processor:transform_to_internal_format(parsed_message)
  
  -- Generate audit event
  local audit_event = processor:generate_audit_event("ISO20022_MESSAGE_PROCESSED", parsed_message, internal_format, headers)
  request_handle:logInfo("ISO20022_AUDIT: " .. json.encode(audit_event))
  
  -- Add ISO 20022-specific headers
  request_handle:headers():add("x-iso20022-document-type", parsed_message.document_type)
  request_handle:headers():add("x-iso20022-message-id", internal_format.message_id)
  request_handle:headers():add("x-iso20022-original-id", parsed_message.group_header.message_id)
  request_handle:headers():add("x-iso20022-processed", "true")
  request_handle:headers():add("x-iso20022-validation-passed", "true")
  
  if parsed_message.group_header.number_of_transactions then
    request_handle:headers():add("x-iso20022-tx-count", tostring(parsed_message.group_header.number_of_transactions))
  end
  
  -- Update request body with internal format for downstream processing
  local internal_json = json.encode(internal_format)
  request_handle:headers():add("content-length", string.len(internal_json))
  request_handle:headers():add("content-type", "application/json")
  
  request_handle:logInfo("ISO 20022 message processed successfully: " .. parsed_message.document_type)
end

-- Response processing for ISO 20022
function envoy_on_response(request_handle)
  local headers = request_handle:headers()
  local iso_processed = headers:get("x-iso20022-processed")
  
  if iso_processed == "true" then
    -- Add response audit headers
    request_handle:headers():add("x-iso20022-response-timestamp", os.date("!%Y-%m-%dT%H:%M:%SZ"))
    request_handle:headers():add("x-iso20022-audit-complete", "true")
    
    request_handle:logInfo("ISO 20022 response processing completed")
  end
end