package com.example.basiccrmfwf.sales.adapter.persistence.mapper;

import com.example.basiccrmfwf.sales.domain.model.Region;
import com.example.basiccrmfwf.sales.domain.model.SaleServiceItem;
import com.example.basiccrmfwf.sales.domain.model.SalesTransaction;
import com.example.basiccrmfwf.sales.adapter.persistence.entity.RegionEntity;
import com.example.basiccrmfwf.sales.adapter.persistence.entity.SaleServiceItemEntity;
import com.example.basiccrmfwf.sales.adapter.persistence.entity.SalesTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between SalesTransaction domain model and JPA entity.
 */
@Mapper(componentModel = "spring")
public interface SalesTransactionEntityMapper {
    
    @Mapping(target = "facility", source = "facility")
    @Mapping(target = "serviceType", ignore = true) // Will be handled separately if needed
    @Mapping(target = "saleServiceItems", source = "saleServiceItems", qualifiedByName = "mapItemsToEntities")
    SalesTransactionEntity toEntity(SalesTransaction domain);
    
    @Mapping(target = "facility", source = "facility")
    @Mapping(target = "serviceType", ignore = true) // Will be handled separately if needed
    @Mapping(target = "saleServiceItems", source = "saleServiceItems", qualifiedByName = "mapItemsToDomain")
    SalesTransaction toDomain(SalesTransactionEntity entity);
    
    // Helper mappings for nested objects
    default RegionEntity map(Region region) {
        if (region == null) return null;
        return RegionEntity.builder()
                .id(region.getId())
                .shopName(region.getShopName())
                .shopType(region.getShopType())
                .region(region.getRegion())
                .stockId(region.getStockId())
                .build();
    }
    
    default Region map(RegionEntity entity) {
        if (entity == null) return null;
        return Region.builder()
                .id(entity.getId())
                .shopName(entity.getShopName())
                .shopType(entity.getShopType())
                .region(entity.getRegion())
                .stockId(entity.getStockId())
                .build();
    }
    
    // Note: SaleServiceItem mapping is simplified for now
    // Full mapping would require ServiceType domain model
    @Named("mapItemsToEntities")
    default List<SaleServiceItemEntity> mapItemsToEntities(List<SaleServiceItem> items) {
        if (items == null) return null;
        // Simplified mapping - will be enhanced when ServiceType domain is created
        return items.stream()
                .map(item -> SaleServiceItemEntity.builder()
                        .id(item.getId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Named("mapItemsToDomain")
    default List<SaleServiceItem> mapItemsToDomain(List<SaleServiceItemEntity> entities) {
        if (entities == null) return null;
        // Simplified mapping - will be enhanced when ServiceType domain is created
        return entities.stream()
                .map(entity -> SaleServiceItem.builder()
                        .id(entity.getId())
                        .quantity(entity.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}
