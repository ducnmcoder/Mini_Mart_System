package com.minimart.service.impl;
import com.minimart.validation.InventoryValidationException;


import com.minimart.repository.InventoryRepository;
import com.minimart.service.AuditService;
import com.minimart.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository repository;
    private final AuditService audit;
    public InventoryServiceImpl(InventoryRepository repository, AuditService audit) { this.repository=repository; this.audit=audit; }
    public List<Map<String,Object>> locations() { return repository.locations(); }
    public List<Map<String,Object>> stock() { return repository.stock(); }
    public List<Map<String,Object>> history() { return repository.history(); }
    private String text(String value, int max, boolean required) {
        String result=value==null ? "" : value.trim();
        if ((required && result.isEmpty()) || result.length()>max) throw new InventoryValidationException("Vui lòng nhập đủ thông tin và đúng giới hạn ký tự.");
        return result;
    }
    @Transactional
    public void saveLocation(Long id, String code, String name, String description, long actor) {
        code=text(code,30,true).toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]+")) throw new InventoryValidationException("Mã gian hàng chỉ gồm chữ không dấu, số, dấu - hoặc _.");
        name=text(name,120,true); description=text(description,255,false);
        boolean creating=id==null;
        if (creating) id=repository.createLocation(code,name,description);
        else { repository.lockLocation(id); repository.updateLocation(id,code,name,description); }
        audit.record(actor,creating?"CREATE_LOCATION":"UPDATE_LOCATION","STORAGE_LOCATION",id.toString(),null,Map.of("code",code,"name",name,"description",description));
    }
    @Transactional
    public void deleteLocation(long id, long actor) {
        repository.lockLocation(id);
        if (repository.hasStock(id)) throw new InventoryValidationException("Gian hàng còn hàng. Hãy chuyển hết hàng trước khi xóa.");
        repository.deactivate(id);
        audit.record(actor,"DELETE_LOCATION","STORAGE_LOCATION",Long.toString(id),null,Map.of("is_active",false));
    }
    @Transactional
    public void transfer(long product, long source, long target, int quantity, String reason, long actor) {
        if (product<=0 || source<=0 || target<=0 || source==target || quantity<=0)
            throw new InventoryValidationException("Chọn sản phẩm, hai gian hàng khác nhau và số lượng lớn hơn 0.");
        reason=text(reason,500,true);
        repository.lockLocation(Math.min(source,target));
        repository.lockLocation(Math.max(source,target));
        repository.transfer(product,source,target,quantity,actor,reason,"MOVE-"+UUID.randomUUID().toString().replace("-",""));
    }
}
