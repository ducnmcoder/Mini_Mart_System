package com.minimart.service;

import java.util.List;
import java.util.Map;

public interface InventoryService {
    List<Map<String,Object>> locations();
    List<Map<String,Object>> stock();
    List<Map<String,Object>> history();
    void saveLocation(Long id, String code, String name, String description, long actor);
    void deleteLocation(long id, long actor);
    void transfer(long product, long source, long target, int quantity, String reason, long actor);
}
