package com.minimart.service;
import com.minimart.validation.InventoryValidationException;


import com.minimart.repository.InventoryRepository;
import com.minimart.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class InventoryServiceTest {
    private final InventoryRepository repository=mock(InventoryRepository.class);
    private final InventoryService service=new InventoryServiceImpl(repository,mock(AuditService.class));

    @Test void rejectsInvalidTransfersBeforeChangingStock() {
        assertThrows(InventoryValidationException.class,()->service.transfer(1,2,2,5,"Bổ sung",7));
        assertThrows(InventoryValidationException.class,()->service.transfer(1,2,3,0,"Bổ sung",7));
        assertThrows(InventoryValidationException.class,()->service.transfer(1,2,3,-1,"Bổ sung",7));
        assertThrows(InventoryValidationException.class,()->service.transfer(1,2,3,5,"  ",7));
        verifyNoInteractions(repository);
    }
    @Test void cannotDeleteLocationHoldingStock() {
        when(repository.hasStock(2)).thenReturn(true);
        assertThrows(InventoryValidationException.class,()->service.deleteLocation(2,7));
        verify(repository,never()).deactivate(anyLong());
    }
    @Test void transferReferenceFitsDatabaseColumnAndLocksInStableOrder() {
        service.transfer(1,9,2,5,"Bổ sung",7);
        var order=inOrder(repository);
        order.verify(repository).lockLocation(2);
        order.verify(repository).lockLocation(9);
        verify(repository).transfer(eq(1L),eq(9L),eq(2L),eq(5),eq(7L),eq("Bổ sung"),argThat(s->s.startsWith("MOVE-") && s.length()<=40));
    }
}
