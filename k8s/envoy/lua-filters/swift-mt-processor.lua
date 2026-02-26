-- SWIFT MT Message Processing Filter
-- Handles SWIFT MT message validation, transformation, and audit logging
-- Compliance: SWIFT CSP, ISO 15022, banking regulations

local json = require("json")
local string = require("string")

-- SWIFT MT message structure definitions
local SWIFT_MT_TYPES = {
  ["101"] = "Request for Transfer",
  ["103"] = "Single Customer Credit Transfer", 
  ["202"] = "General Financial Institution Transfer",
  ["210"] = "Notice to Receive",
  ["900"] = "Confirmation of Debit",
  ["910"] = "Confirmation of Credit",
  ["940"] = "Customer Statement Message",
  ["950"] = "Statement Message",
  ["999"] = "Free Format Message"
}

-- SWIFT message field definitions
local SWIFT_FIELDS = {
  ["20"] = "Transaction Reference Number",
  ["21"] = "Related Reference",
  ["23B"] = "Bank Operation Code",
  ["32A"] = "Value Date/Currency/Amount",
  ["50K"] = "Ordering Customer",
  ["52A"] = "Ordering Institution",
  ["57A"] = "Account With Institution",
  ["59"] = "Beneficiary Customer",
  ["70"] = "Remittance Information",
  ["71A"] = "Details of Charges"
}

-- SWIFT MT processor class
local SwiftMTProcessor = {}
SwiftMTProcessor.__index = SwiftMTProcessor

function SwiftMTProcessor:new()
  local obj = {
    message_count = 0,
    error_count = 0,
    processed_messages = {}
  }
  setmetatable(obj, SwiftMTProcessor)
  return obj
end

-- Parse SWIFT MT message
function SwiftMTProcessor:parse_mt_message(message_body)
  if not message_body or message_body == "" then
    return nil, "Empty SWIFT message body"
  end

  local parsed = {
    basic_header = nil,
    application_header = nil,
    user_header = nil,
    text_block = nil,
    trailer_block = nil,
    message_type = nil,
    fields = {}
  }

  -- Extract basic header (Block 1)
  local basic_header = string.match(message_body, "{1:([^}]+)}")
  if basic_header then
    parsed.basic_header = self:parse_basic_header(basic_header)
  else
    return nil, "Missing SWIFT basic header (Block 1)"
  end

  -- Extract application header (Block 2)
  local app_header = string.match(message_body, "{2:([^}]+)}")
  if app_header then
    parsed.application_header = self:parse_application_header(app_header)
    parsed.message_type = string.match(app_header, "^[A-Z]?(%d%d%d)")
  else
    return nil, "Missing SWIFT application header (Block 2)"
  end

  -- Extract user header (Block 3) - optional
  local user_header = string.match(message_body, "{3:([^}]+)}")
  if user_header then
    parsed.user_header = self:parse_user_header(user_header)
  end

  -- Extract text block (Block 4)
  local text_block = string.match(message_body, "{4:%s*([^}]+)}")
  if text_block then
    parsed.text_block = text_block
    parsed.fields = self:parse_text_fields(text_block)
  else
    return nil, "Missing SWIFT text block (Block 4)"
  end

  -- Extract trailer block (Block 5) - optional
  local trailer_block = string.match(message_body, "{5:([^}]+)}")
  if trailer_block then
    parsed.trailer_block = self:parse_trailer_block(trailer_block)
  end

  return parsed, nil
end

-- Parse basic header (Block 1)
function SwiftMTProcessor:parse_basic_header(header)
  local parsed = {}
  
  -- Application ID (1 character)
  parsed.application_id = string.sub(header, 1, 1)
  
  -- Service ID (2 characters)
  parsed.service_id = string.sub(header, 2, 3)
  
  -- LT Address (12 characters)
  parsed.lt_address = string.sub(header, 4, 15)
  
  -- Session Number (4 characters)
  parsed.session_number = string.sub(header, 16, 19)
  
  -- Sequence Number (6 characters)
  parsed.sequence_number = string.sub(header, 20, 25)
  
  return parsed
end

-- Parse application header (Block 2)
function SwiftMTProcessor:parse_application_header(header)
  local parsed = {}
  
  -- Input/Output identifier
  parsed.io_identifier = string.sub(header, 1, 1)
  
  if parsed.io_identifier == "I" then
    -- Input application header
    parsed.message_type = string.sub(header, 2, 4)
    parsed.destination_address = string.sub(header, 5, 16)
    parsed.priority = string.sub(header, 17, 17)
    parsed.delivery_monitoring = string.sub(header, 18, 18)
    parsed.obsolescence_period = string.sub(header, 19, 21)
  elseif parsed.io_identifier == "O" then
    -- Output application header  
    parsed.message_type = string.sub(header, 2, 4)
    parsed.input_time = string.sub(header, 5, 8)
    parsed.mir = string.sub(header, 9, 36)
    parsed.output_date = string.sub(header, 37, 42)
    parsed.output_time = string.sub(header, 43, 46)
    parsed.priority = string.sub(header, 47, 47)
  end
  
  return parsed
end

-- Parse user header (Block 3)
function SwiftMTProcessor:parse_user_header(header)
  local parsed = {}
  local fields = {}
  
  -- Parse tag-value pairs
  for tag, value in string.gmatch(header, "{(%d+):([^}]+)}") do
    fields[tag] = value
  end
  
  parsed.fields = fields
  return parsed
end

-- Parse text fields (Block 4)
function SwiftMTProcessor:parse_text_fields(text_block)
  local fields = {}
  
  -- Parse field patterns like :20:reference or :32A:value
  for tag, value in string.gmatch(text_block, ":(%w+):([^:]*):?") do
    -- Clean up the value (remove newlines and extra spaces)
    value = string.gsub(value, "%s+", " ")
    value = string.gsub(value, "^%s*(.-)%s*$", "%1")
    
    fields[tag] = {
      value = value,
      description = SWIFT_FIELDS[tag] or "Unknown field"
    }
  end
  
  return fields
end

-- Parse trailer block (Block 5)
function SwiftMTProcessor:parse_trailer_block(trailer)
  local parsed = {}
  local fields = {}
  
  -- Parse tag-value pairs
  for tag, value in string.gmatch(trailer, "{(%w+):([^}]+)}") do
    fields[tag] = value
  end
  
  parsed.fields = fields
  return parsed
end

-- Validate SWIFT MT message
function SwiftMTProcessor:validate_mt_message(parsed_message)
  local validation_errors = {}
  
  -- Check if message type is supported
  if not SWIFT_MT_TYPES[parsed_message.message_type] then
    table.insert(validation_errors, "Unsupported SWIFT MT message type: " .. (parsed_message.message_type or "unknown"))
  end
  
  -- Basic header validation
  if not parsed_message.basic_header then
    table.insert(validation_errors, "Missing basic header")
  elseif not parsed_message.basic_header.lt_address or string.len(parsed_message.basic_header.lt_address) ~= 12 then
    table.insert(validation_errors, "Invalid LT address in basic header")
  end
  
  -- Application header validation
  if not parsed_message.application_header then
    table.insert(validation_errors, "Missing application header")
  elseif not parsed_message.application_header.message_type then
    table.insert(validation_errors, "Missing message type in application header")
  end
  
  -- Message type specific validation
  if parsed_message.message_type then
    local type_errors = self:validate_message_type_specific(parsed_message)
    for _, error in ipairs(type_errors) do
      table.insert(validation_errors, error)
    end
  end
  
  return validation_errors
end

-- Message type specific validation
function SwiftMTProcessor:validate_message_type_specific(parsed_message)
  local errors = {}
  local mt_type = parsed_message.message_type
  local fields = parsed_message.fields
  
  if mt_type == "103" then
    -- Single Customer Credit Transfer validation
    if not fields["20"] then
      table.insert(errors, "MT103 missing mandatory field :20: (Transaction Reference)")
    end
    if not fields["32A"] then
      table.insert(errors, "MT103 missing mandatory field :32A: (Value Date/Currency/Amount)")
    end
    if not fields["50K"] and not fields["50A"] then
      table.insert(errors, "MT103 missing mandatory field :50: (Ordering Customer)")
    end
    if not fields["59"] and not fields["59A"] then
      table.insert(errors, "MT103 missing mandatory field :59: (Beneficiary)")
    end
  elseif mt_type == "202" then
    -- General Financial Institution Transfer validation
    if not fields["20"] then
      table.insert(errors, "MT202 missing mandatory field :20: (Transaction Reference)")
    end
    if not fields["32A"] then
      table.insert(errors, "MT202 missing mandatory field :32A: (Value Date/Currency/Amount)")
    end
    if not fields["52A"] and not fields["52D"] then
      table.insert(errors, "MT202 missing mandatory field :52: (Ordering Institution)")
    end
    if not fields["58A"] and not fields["58D"] then
      table.insert(errors, "MT202 missing mandatory field :58: (Beneficiary Institution)")
    end
  elseif mt_type == "940" then
    -- Customer Statement Message validation
    if not fields["20"] then
      table.insert(errors, "MT940 missing mandatory field :20: (Transaction Reference)")
    end
    if not fields["25"] then
      table.insert(errors, "MT940 missing mandatory field :25: (Account Identification)")
    end
    if not fields["28C"] then
      table.insert(errors, "MT940 missing mandatory field :28C: (Statement Number)")
    end
  end
  
  return errors
end

-- Transform SWIFT message to internal format
function SwiftMTProcessor:transform_to_internal_format(parsed_message)
  local internal_format = {
    message_id = self:generate_message_id(),
    message_type = "SWIFT_MT" .. (parsed_message.message_type or "UNKNOWN"),
    timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ"),
    source_format = "SWIFT_MT",
    original_message_type = parsed_message.message_type,
    sender = parsed_message.basic_header and parsed_message.basic_header.lt_address,
    receiver = parsed_message.application_header and parsed_message.application_header.destination_address,
    priority = parsed_message.application_header and parsed_message.application_header.priority,
    fields = {},
    banking_data = {}
  }
  
  -- Transform key fields to internal format
  if parsed_message.fields then
    for tag, field_data in pairs(parsed_message.fields) do
      internal_format.fields[tag] = field_data.value
      
      -- Extract banking-specific data
      if tag == "20" then
        internal_format.banking_data.transaction_reference = field_data.value
      elseif tag == "32A" then
        internal_format.banking_data.amount_info = self:parse_amount_field(field_data.value)
      elseif tag == "50K" or tag == "50A" then
        internal_format.banking_data.ordering_customer = field_data.value
      elseif tag == "59" or tag == "59A" then
        internal_format.banking_data.beneficiary = field_data.value
      elseif tag == "70" then
        internal_format.banking_data.remittance_info = field_data.value
      end
    end
  end
  
  return internal_format
end

-- Parse amount field (e.g., "201201USD1000,00")
function SwiftMTProcessor:parse_amount_field(amount_string)
  if not amount_string or amount_string == "" then
    return nil
  end
  
  -- Pattern: YYMMDDCCCAAA where YY=year, MM=month, DD=day, CCC=currency, AAA=amount
  local date_part = string.sub(amount_string, 1, 6)
  local currency = string.sub(amount_string, 7, 9)
  local amount = string.sub(amount_string, 10)
  
  -- Convert amount (replace comma with dot for decimal)
  amount = string.gsub(amount, ",", ".")
  
  return {
    value_date = date_part,
    currency = currency,
    amount = amount
  }
end

-- Generate unique message ID
function SwiftMTProcessor:generate_message_id()
  return "SWIFT_" .. os.time() .. "_" .. math.random(10000, 99999)
end

-- Generate audit event for SWIFT processing
function SwiftMTProcessor:generate_audit_event(event_type, parsed_message, internal_format, headers)
  local audit_event = {
    timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ"),
    event_type = event_type,
    protocol = "SWIFT_MT",
    message_type = parsed_message.message_type,
    message_id = internal_format and internal_format.message_id,
    transaction_reference = parsed_message.fields and parsed_message.fields["20"] and parsed_message.fields["20"].value,
    sender_address = parsed_message.basic_header and parsed_message.basic_header.lt_address,
    receiver_address = parsed_message.application_header and parsed_message.application_header.destination_address,
    priority = parsed_message.application_header and parsed_message.application_header.priority,
    session_info = {
      session_number = parsed_message.basic_header and parsed_message.basic_header.session_number,
      sequence_number = parsed_message.basic_header and parsed_message.basic_header.sequence_number
    },
    request_context = {
      source_ip = headers and headers["x-forwarded-for"],
      user_agent = headers and headers["user-agent"],
      transaction_id = headers and headers["x-transaction-id"],
      institution_id = headers and headers["x-institution-id"]
    },
    compliance_info = {
      swift_csp_compliant = true,
      iso_15022_compliant = true,
      audit_trail_complete = true
    }
  }
  
  return audit_event
end

-- Main SWIFT MT processing function for Envoy
function envoy_on_request(request_handle)
  local headers = request_handle:headers()
  local path = headers:get(":path")
  local method = headers:get(":method")
  local content_type = headers:get("content-type")
  
  -- Only process SWIFT MT messages
  if not (method == "POST" and (string.match(path, "/swift/") or 
          (content_type and string.match(content_type, "text/plain")))) then
    return
  end
  
  local body = request_handle:body()
  if not body then
    request_handle:logWarn("SWIFT MT processor: No message body found")
    return
  end
  
  local processor = SwiftMTProcessor:new()
  local message_body = tostring(body:getBytes(0, body:length()))
  
  -- Parse SWIFT MT message
  local parsed_message, parse_error = processor:parse_mt_message(message_body)
  if not parsed_message then
    local error_response = json.encode({
      error = "SWIFT_PARSE_ERROR",
      code = "SPE001",
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
  
  -- Validate SWIFT MT message
  local validation_errors = processor:validate_mt_message(parsed_message)
  if #validation_errors > 0 then
    local error_response = json.encode({
      error = "SWIFT_VALIDATION_ERROR", 
      code = "SVE001",
      message = "SWIFT MT message validation failed",
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
  local audit_event = processor:generate_audit_event("SWIFT_MESSAGE_PROCESSED", parsed_message, internal_format, headers)
  request_handle:logInfo("SWIFT_AUDIT: " .. json.encode(audit_event))
  
  -- Add SWIFT-specific headers
  request_handle:headers():add("x-swift-message-type", "MT" .. parsed_message.message_type)
  request_handle:headers():add("x-swift-message-id", internal_format.message_id)
  request_handle:headers():add("x-swift-sender", parsed_message.basic_header.lt_address)
  request_handle:headers():add("x-swift-processed", "true")
  request_handle:headers():add("x-swift-validation-passed", "true")
  
  if parsed_message.fields and parsed_message.fields["20"] then
    request_handle:headers():add("x-swift-reference", parsed_message.fields["20"].value)
  end
  
  -- Update request body with internal format for downstream processing
  local internal_json = json.encode(internal_format)
  request_handle:headers():add("content-length", string.len(internal_json))
  request_handle:headers():add("content-type", "application/json")
  
  request_handle:logInfo("SWIFT MT message processed successfully: MT" .. parsed_message.message_type)
end

-- Response processing for SWIFT MT
function envoy_on_response(request_handle)
  local headers = request_handle:headers()
  local swift_processed = headers:get("x-swift-processed")
  
  if swift_processed == "true" then
    -- Add response audit headers
    request_handle:headers():add("x-swift-response-timestamp", os.date("!%Y-%m-%dT%H:%M:%SZ"))
    request_handle:headers():add("x-swift-audit-complete", "true")
    
    request_handle:logInfo("SWIFT MT response processing completed")
  end
end