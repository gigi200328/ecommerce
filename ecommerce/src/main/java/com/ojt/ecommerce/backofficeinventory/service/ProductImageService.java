
package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.ProductImageRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductImageResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.ProductImageMapper;
import com.ojt.ecommerce.backofficeinventory.repository.ProductImageRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductImage;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.UploadType;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageService {

	private final ProductImageRepository productImageRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final ProductImageMapper productImageMapper;
	private final FileStorageService fileStorageService;

	/**
	 * Upload an image for a product.
	 *
	 * If isPrimary = true: - Existing primary image(s) will be changed to false. -
	 * Newly uploaded image will become the primary image.
	 */
	@Transactional
	public ProductImageResponseDto uploadProductImage(ProductImageRequestDto request) {

		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getProductId()));

		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

		String imageUrl = fileStorageService.store(request.getFile(), UploadType.PRODUCT_IMAGE);

		/*
		 * Remove newly uploaded file if the transaction fails.
		 */
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					try {
						fileStorageService.delete(imageUrl, UploadType.PRODUCT_IMAGE);
					} catch (RuntimeException ex) {
						System.err.println("Image cleanup failed: " + ex.getMessage());
					}
				}
			}
		});

		LocalDateTime now = LocalDateTime.now();

		boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());

		/*
		 * If the new image should be primary, remove primary status from existing
		 * primary images.
		 */
		if (isPrimary) {
			unsetExistingPrimaryImages(product.getProductId(), user, now);
		}

		ProductImage productImage = ProductImage.builder().product(product).imageUrl(imageUrl).isPrimary(isPrimary)
				.createdBy(user).createdAt(now).modifiedBy(user).modifiedAt(now).build();

		ProductImage saved = productImageRepository.save(productImage);

		return productImageMapper.toResponseDto(saved);
	}

	/**
	 * Get all images for a product.
	 */
	@Transactional(readOnly = true)
	public List<ProductImageResponseDto> getImagesByProductId(Long productId) {

		return productImageRepository.findByProductProductId(productId).stream().map(productImageMapper::toResponseDto)
				.toList();
	}

	/**
	 * Set an existing product image as the primary image.
	 *
	 * The current primary image will automatically become non-primary.
	 */
	@Transactional
	public ProductImageResponseDto setPrimaryImage(Long imageId, Long userId) {

		ProductImage image = productImageRepository.findById(imageId)
				.orElseThrow(() -> new EntityNotFoundException("Product image not found: " + imageId));

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

		LocalDateTime now = LocalDateTime.now();

		Long productId = image.getProduct().getProductId();

		/*
		 * Remove primary status from existing primary images.
		 */
		unsetExistingPrimaryImages(productId, user, now);

		/*
		 * Set selected image as primary.
		 */
		image.setIsPrimary(true);
		image.setModifiedBy(user);
		image.setModifiedAt(now);

		ProductImage saved = productImageRepository.save(image);

		return productImageMapper.toResponseDto(saved);
	}

	/**
	 * Remove primary status from all existing primary images belonging to the given
	 * product.
	 */
	private void unsetExistingPrimaryImages(Long productId, User user, LocalDateTime now) {

		List<ProductImage> existingPrimaryImages = productImageRepository
				.findByProductProductIdAndIsPrimaryTrue(productId);

		if (existingPrimaryImages.isEmpty()) {
			return;
		}

		for (ProductImage existingImage : existingPrimaryImages) {

			existingImage.setIsPrimary(false);
			existingImage.setModifiedBy(user);
			existingImage.setModifiedAt(now);
		}

		productImageRepository.saveAll(existingPrimaryImages);
	}

	/**
	 * Delete a product image.
	 */
	@Transactional
	public void deleteProductImage(Long imageId) {

		ProductImage image = productImageRepository.findById(imageId)
				.orElseThrow(() -> new EntityNotFoundException("Product image not found: " + imageId));

		String imageUrl = image.getImageUrl();

		productImageRepository.delete(image);

		/*
		 * Delete the physical file only after DB commit.
		 */
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void afterCommit() {
				try {
					fileStorageService.delete(imageUrl, UploadType.PRODUCT_IMAGE);
				} catch (RuntimeException ex) {
					System.err.println("Image deletion failed: " + ex.getMessage());
				}
			}
		});
	}
}
