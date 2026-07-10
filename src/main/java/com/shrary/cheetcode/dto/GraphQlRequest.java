package com.shrary.cheetcode.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Generic GraphQL request body: { "query": "...", "variables": { ... } }
 */
@Data
@Builder
public class GraphQlRequest {
    private String query;
    private Map<String, Object> variables;
}
