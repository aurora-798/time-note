package com.note.ai.rerank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerankRequest {

    private String model;

    private String query;

    private List<String> documents;

    private String instruct;
}
