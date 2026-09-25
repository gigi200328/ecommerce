package com.ojt.ecommerce.storefront.mapper;
import com.ojt.ecommerce.entity.CustomerAddress;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface CustomerAddressMapper {
    CustomerAddressResponse toResponse(CustomerAddress entity);
    
    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    void updateEntityFromRequest(CustomerAddressRequest request, @MappingTarget CustomerAddress entity);
}