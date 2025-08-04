package com.alexgls.springboot.client.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@ToString
public class UploadFilePathResponse {
    private String method;
    private String href;
    private boolean templated;
    private String operationId;
}
