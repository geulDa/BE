package com.CUK.geulDa.ai.mcp;

import java.util.List;
import java.util.Map;

public interface McpServer {

    List<McpTool> getTools();

    Object executeTool(String toolName, Map<String, Object> params);
}
