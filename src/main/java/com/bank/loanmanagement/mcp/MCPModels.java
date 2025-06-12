package com.bank.loanmanagement.mcp;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Model Context Protocol (MCP) Data Models
 * Defines structured data formats for LLM integration
 */

@Data
@Builder
public class MCPResource {
    private String uri;
    private String name;
    private String description;
    private String mimeType;
    private List<String> capabilities;
}

@Data
@Builder
public class MCPResourcesResponse {
    private List<MCPResource> resources;
}

@Data
@Builder
public class MCPTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}

@Data
@Builder
public class MCPToolsResponse {
    private List<MCPTool> tools;
}

@Data
@Builder
public class MCPToolRequest {
    private String name;
    private Map<String, Object> arguments;
}

@Data
@Builder
public class MCPToolResponse {
    private boolean isError;
    private List<MCPContent> content;
}

@Data
@Builder
public class MCPContent {
    private String type;
    private Map<String, Object> data;
}