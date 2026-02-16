package com.pearlplus.pearlplus.model;

import java.util.List;

public record PearlLoadResponse(
    String status,
    List<String> output
) {
}
