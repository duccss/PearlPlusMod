package com.zenith.pearlplus.model;

import java.util.List;

public record PearlLoadResponse(
    String status,
    List<String> output
) {
}
