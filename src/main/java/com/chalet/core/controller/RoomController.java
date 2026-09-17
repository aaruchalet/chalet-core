package com.chalet.core.controller;

import com.chalet.core.dto.common.ApiResponse;
import com.chalet.core.dto.request.RoomRequest;
import com.chalet.core.dto.response.RoomResponse;
import com.chalet.core.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<RoomResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.success(roomService.findAll()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<RoomResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(roomService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<RoomResponse>> create(
          @Valid @RequestBody RoomRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(roomService.create(request)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<RoomResponse>> update(
          @PathVariable Long id,
          @Valid @RequestBody RoomRequest request) {
    return ResponseEntity.ok(ApiResponse.success(roomService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    roomService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
