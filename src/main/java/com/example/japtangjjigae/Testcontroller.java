package com.example.japtangjjigae;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test API", description = "테스트 도메인 API")
@RestController
public class Testcontroller {

    @Operation(
        summary = "테스트 하는 기능",
        description = "테스트하기 위해 임시로 만든 API"
    )
    @GetMapping("/api/v1/test")
    public ResponseEntity<?> testGet(){
        Map<String, Object> resultBody = Map.of(
            "content", "테스트 중"
        );
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(new MediaType("application", "json"));

        return new ResponseEntity<>(resultBody, httpHeaders, HttpStatus.OK);
    }

}
