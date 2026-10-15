package com.ojt.ecommerce.backofficeinventory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRegionDto {
	private String regionOrState;
	private String city;
	private List<String> townships;
}
