package com.ojt.ecommerce.backofficeinventory.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.ProductImportResultDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductImportResultDto.RowError;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.InventoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariantOptionValueRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationRepository;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.ProductVariantStatus;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.entity.VariantOptionValue;
import com.ojt.ecommerce.entity.Variation;
import com.ojt.ecommerce.entity.VariationOption;
import com.ojt.ecommerce.enums.BrandStatus;
import com.ojt.ecommerce.enums.ProductStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExcelServiceImpl implements ProductExcelService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final VariationRepository variationRepository;
    private final VariationOptionRepository variationOptionRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;

    private static final String[] HEADERS = {
        "Product Name *",
        "Description",
        "Category *",
        "Brand",
        "Selling Price (MMK) *",
        "Cost Price (MMK)",
        "Initial Stock *",
        "SKU",
        "Size",
        "Color",
        "Status *"
    };

    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Products Import");
            XSSFSheet lookupSheet = workbook.createSheet("Lookup_Data");

            // Setup Header Style
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            byte[] indigoRgb = new byte[]{(byte) 79, (byte) 70, (byte) 229}; // #4F46E5
            headerStyle.setFillForegroundColor(new XSSFColor(indigoRgb, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate Lookup Sheet with Categories & Brands from DB
            List<Category> categories = categoryRepository.findAll();
            List<Brand> brands = brandRepository.findAll();

            Row lookupHeader = lookupSheet.createRow(0);
            lookupHeader.createCell(0).setCellValue("Categories");
            lookupHeader.createCell(1).setCellValue("Brands");

            int catCount = 0;
            for (Category c : categories) {
                if (c.getCategoryName() != null && !c.getCategoryName().trim().isEmpty()) {
                    Row r = lookupSheet.getRow(catCount + 1);
                    if (r == null) r = lookupSheet.createRow(catCount + 1);
                    r.createCell(0).setCellValue(c.getCategoryName().trim());
                    catCount++;
                }
            }

            int brandCount = 0;
            for (Brand b : brands) {
                if (b.getBrandName() != null && !b.getBrandName().trim().isEmpty()) {
                    Row r = lookupSheet.getRow(brandCount + 1);
                    if (r == null) r = lookupSheet.createRow(brandCount + 1);
                    r.createCell(1).setCellValue(b.getBrandName().trim());
                    brandCount++;
                }
            }

            // Hide lookup sheet
            workbook.setSheetHidden(1, true);

            // Add Data Validations for Category, Brand, Status
            DataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);

            // 1. Category Dropdown (Column C, index 2)
            if (catCount > 0) {
                CellRangeAddressList catList = new CellRangeAddressList(1, 10000, 2, 2);
                DataValidationConstraint catConstraint = dvHelper.createFormulaListConstraint("Lookup_Data!$A$2:$A$" + (catCount + 1));
                DataValidation catValidation = dvHelper.createValidation(catConstraint, catList);
                catValidation.setShowErrorBox(true);
                catValidation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
                catValidation.createErrorBox("Category Selection", "Please select a category from the dropdown or type a valid one.");
                sheet.addValidationData(catValidation);
            }

            // 2. Brand Dropdown (Column D, index 3)
            if (brandCount > 0) {
                CellRangeAddressList brandList = new CellRangeAddressList(1, 10000, 3, 3);
                DataValidationConstraint brandConstraint = dvHelper.createFormulaListConstraint("Lookup_Data!$B$2:$B$" + (brandCount + 1));
                DataValidation brandValidation = dvHelper.createValidation(brandConstraint, brandList);
                brandValidation.setShowErrorBox(false);
                sheet.addValidationData(brandValidation);
            }

            // 3. Status Dropdown (Column K, index 10)
            CellRangeAddressList statusList = new CellRangeAddressList(1, 10000, 10, 10);
            DataValidationConstraint statusConstraint = dvHelper.createExplicitListConstraint(new String[]{"ACTIVE", "INACTIVE"});
            DataValidation statusValidation = dvHelper.createValidation(statusConstraint, statusList);
            statusValidation.setShowErrorBox(true);
            sheet.addValidationData(statusValidation);

            // Sample Rows for User Guidance
            CellStyle sampleStyle = workbook.createCellStyle();
            sampleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle numStyle = workbook.createCellStyle();
            numStyle.setAlignment(HorizontalAlignment.RIGHT);

            String firstCat = catCount > 0 ? categories.get(0).getCategoryName() : "General";
            String firstBrand = brandCount > 0 ? brands.get(0).getBrandName() : "Standard";

            Object[][] sampleData = {
                {"Premium Cotton Shirt", "Comfortable casual cotton shirt", firstCat, firstBrand, 28000, 18000, 50, "SHIRT-COT-M", "M", "Navy", "ACTIVE"},
                {"Summer Graphic Tee", "Trendy soft summer graphic t-shirt", firstCat, firstBrand, 19500, 12000, 100, "TEE-SUM-L", "L", "White", "ACTIVE"}
            };

            for (int rowIdx = 0; rowIdx < sampleData.length; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                row.setHeightInPoints(22);
                Object[] data = sampleData[rowIdx];
                for (int colIdx = 0; colIdx < data.length; colIdx++) {
                    Cell c = row.createCell(colIdx);
                    if (data[colIdx] instanceof Number) {
                        c.setCellValue(((Number) data[colIdx]).doubleValue());
                        c.setCellStyle(numStyle);
                    } else {
                        c.setCellValue(String.valueOf(data[colIdx]));
                        c.setCellStyle(sampleStyle);
                    }
                }
            }

            // Adjust column widths
            int[] colWidths = {26, 32, 20, 18, 22, 18, 15, 18, 12, 12, 14};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    @Transactional
    public ProductImportResultDto importProductsFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty.");
        }

        List<RowError> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("No sheets found in Excel file.");
            }

            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 1) {
                return ProductImportResultDto.builder()
                        .totalRows(0)
                        .successCount(0)
                        .failedCount(0)
                        .errors(errors)
                        .build();
            }

            // In-memory lookups
            Map<String, Category> categoryMap = new HashMap<>();
            categoryRepository.findAll().forEach(c -> {
                if (c.getCategoryName() != null) {
                    categoryMap.put(c.getCategoryName().trim().toLowerCase(), c);
                }
            });

            Map<String, Brand> brandMap = new HashMap<>();
            brandRepository.findAll().forEach(b -> {
                if (b.getBrandName() != null) {
                    brandMap.put(b.getBrandName().trim().toLowerCase(), b);
                }
            });

            Set<String> existingSkus = new HashSet<>();
            productVariantRepository.findAll().forEach(v -> {
                if (v.getSku() != null) {
                    existingSkus.add(v.getSku().trim().toUpperCase());
                }
            });

            User defaultUser = userRepository.findById(1L)
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

            if (defaultUser == null) {
                throw new IllegalStateException("No default system user found to associate imported products.");
            }

            // Ensure variations "Size" and "Colour" exist in database
            Variation sizeVariation = getOrCreateVariation("Size");
            Variation colorVariation = getOrCreateVariation("Colour");

            LocalDateTime now = LocalDateTime.now();

            // Loop through data rows (from row 1, skipping row 0 header)
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (isRowEmpty(row)) {
                    continue; // Skip blank rows
                }

                totalRows++;
                int displayRowNumber = r + 1;

                String productName = getCellString(row.getCell(0));
                String description = getCellString(row.getCell(1));
                String categoryName = getCellString(row.getCell(2));
                String brandName = getCellString(row.getCell(3));
                BigDecimal sellingPrice = getCellBigDecimal(row.getCell(4));
                BigDecimal costPrice = getCellBigDecimal(row.getCell(5));
                Integer stock = getCellInteger(row.getCell(6));
                String sku = getCellString(row.getCell(7));
                String size = getCellString(row.getCell(8));
                String color = getCellString(row.getCell(9));
                String statusStr = getCellString(row.getCell(10));

                // 1. Validation
                if (productName.isEmpty()) {
                    errors.add(new RowError(displayRowNumber, productName, sku, "Product Name is required."));
                    continue;
                }

                if (categoryName.isEmpty()) {
                    errors.add(new RowError(displayRowNumber, productName, sku, "Category is required."));
                    continue;
                }

                if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(new RowError(displayRowNumber, productName, sku, "Valid positive selling price is required."));
                    continue;
                }

                if (stock == null || stock < 0) {
                    errors.add(new RowError(displayRowNumber, productName, sku, "Initial stock must be 0 or greater."));
                    continue;
                }

                // SKU handling
                if (sku.isEmpty()) {
                    sku = generateAutoSku(productName, size, color);
                }

                String upperSku = sku.toUpperCase();
                if (existingSkus.contains(upperSku)) {
                    errors.add(new RowError(displayRowNumber, productName, sku, "SKU '" + sku + "' already exists in database."));
                    continue;
                }

                // 2. Resolve Category
                Category category = categoryMap.get(categoryName.toLowerCase());
                if (category == null) {
                    category = Category.builder()
                            .categoryName(categoryName)
                            .description("Auto-created from Excel import")
                            .createdBy(defaultUser)
                            .createdAt(now)
                            .build();
                    category = categoryRepository.save(category);
                    categoryMap.put(categoryName.toLowerCase(), category);
                }

                // 3. Resolve Brand
                Brand brand = null;
                if (!brandName.isEmpty()) {
                    brand = brandMap.get(brandName.toLowerCase());
                    if (brand == null) {
                        brand = Brand.builder()
                                .brandName(brandName)
                                .description("Auto-created from Excel import")
                                .status(BrandStatus.ACTIVE)
                                .createdBy(defaultUser)
                                .createdAt(now)
                                .build();
                        brand = brandRepository.save(brand);
                        brandMap.put(brandName.toLowerCase(), brand);
                    }
                }

                // 4. Resolve Status
                ProductStatus prodStatus = "INACTIVE".equalsIgnoreCase(statusStr)
                        ? ProductStatus.INACTIVE
                        : ProductStatus.ACTIVE;

                try {
                    // Create Product
                    Product product = Product.builder()
                            .productName(productName)
                            .description(description)
                            .category(category)
                            .brand(brand)
                            .status(prodStatus)
                            .isPerishable(false)
                            .createdBy(defaultUser)
                            .createdAt(now)
                            .build();
                    Product savedProduct = productRepository.save(product);

                    // Create ProductVariant
                    ProductVariant variant = ProductVariant.builder()
                            .product(savedProduct)
                            .sku(sku)
                            .sellingPrice(sellingPrice)
                            .costPrice(costPrice != null ? costPrice : BigDecimal.ZERO)
                            .status(ProductVariantStatus.ACTIVE)
                            .build();
                    ProductVariant savedVariant = productVariantRepository.save(variant);
                    existingSkus.add(upperSku);

                    // Assign Size option if present
                    if (!size.isEmpty() && sizeVariation != null) {
                        VariationOption sizeOption = getOrCreateOption(sizeVariation, size);
                        VariantOptionValue mapping = VariantOptionValue.builder()
                                .variant(savedVariant)
                                .option(sizeOption)
                                .build();
                        variantOptionValueRepository.save(mapping);
                    }

                    // Assign Color option if present
                    if (!color.isEmpty() && colorVariation != null) {
                        VariationOption colorOption = getOrCreateOption(colorVariation, color);
                        VariantOptionValue mapping = VariantOptionValue.builder()
                                .variant(savedVariant)
                                .option(colorOption)
                                .build();
                        variantOptionValueRepository.save(mapping);
                    }

                    // Create Inventory
                    String invStatus = stock > 10 ? "NORMAL" : (stock > 0 ? "LOW_STOCK" : "OUT_OF_STOCK");
                    Inventory inventory = Inventory.builder()
                            .variant(savedVariant)
                            .quantity(stock)
                            .reservedQuantity(0)
                            .reorderLevel(10)
                            .status(invStatus)
                            .isPerishable(false)
                            .createdBy(defaultUser)
                            .createdAt(now)
                            .build();
                    inventoryRepository.save(inventory);

                    successCount++;
                } catch (Exception ex) {
                    log.error("Failed to import product row {}: {}", displayRowNumber, ex.getMessage());
                    errors.add(new RowError(displayRowNumber, productName, sku, ex.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("Excel import failed: ", e);
            throw new RuntimeException("Could not process Excel file: " + e.getMessage(), e);
        }

        return ProductImportResultDto.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    @Override
    @Transactional
    public ProductImportResultDto batchCreateProducts(List<com.ojt.ecommerce.backofficeinventory.dto.ProductBatchRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return ProductImportResultDto.builder().totalRows(0).successCount(0).failedCount(0).errors(List.of()).build();
        }

        List<RowError> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = rows.size();

        Map<String, Category> categoryMap = new HashMap<>();
        categoryRepository.findAll().forEach(c -> {
            if (c.getCategoryName() != null) {
                categoryMap.put(c.getCategoryName().trim().toLowerCase(), c);
            }
        });

        Map<String, Brand> brandMap = new HashMap<>();
        brandRepository.findAll().forEach(b -> {
            if (b.getBrandName() != null) {
                brandMap.put(b.getBrandName().trim().toLowerCase(), b);
            }
        });

        Set<String> existingSkus = new HashSet<>();
        productVariantRepository.findAll().forEach(v -> {
            if (v.getSku() != null) {
                existingSkus.add(v.getSku().trim().toUpperCase());
            }
        });

        User defaultUser = userRepository.findById(1L)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

        if (defaultUser == null) {
            throw new IllegalStateException("No default system user found to associate imported products.");
        }

        Variation sizeVariation = getOrCreateVariation("Size");
        Variation colorVariation = getOrCreateVariation("Colour");
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < rows.size(); i++) {
            com.ojt.ecommerce.backofficeinventory.dto.ProductBatchRowDto dto = rows.get(i);
            int displayRowNumber = i + 1;

            String productName = dto.getProductName() != null ? dto.getProductName().trim() : "";
            String categoryName = dto.getCategoryName() != null ? dto.getCategoryName().trim() : "";
            BigDecimal sellingPrice = dto.getSellingPrice();
            Integer stock = dto.getStock();

            if (productName.isEmpty()) {
                errors.add(new RowError(displayRowNumber, productName, dto.getSku(), "Product Name is required."));
                continue;
            }

            if (categoryName.isEmpty()) {
                errors.add(new RowError(displayRowNumber, productName, dto.getSku(), "Category is required."));
                continue;
            }

            if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new RowError(displayRowNumber, productName, dto.getSku(), "Valid positive selling price is required."));
                continue;
            }

            if (stock == null || stock < 0) {
                errors.add(new RowError(displayRowNumber, productName, dto.getSku(), "Stock must be 0 or greater."));
                continue;
            }

            String sku = dto.getSku() != null ? dto.getSku().trim() : "";
            if (sku.isEmpty()) {
                sku = generateAutoSku(productName, dto.getSize() != null ? dto.getSize() : "", dto.getColor() != null ? dto.getColor() : "");
            }

            String upperSku = sku.toUpperCase();
            if (existingSkus.contains(upperSku)) {
                errors.add(new RowError(displayRowNumber, productName, sku, "SKU '" + sku + "' already exists in database."));
                continue;
            }

            // Resolve Category
            Category category = categoryMap.get(categoryName.toLowerCase());
            if (category == null) {
                category = Category.builder()
                        .categoryName(categoryName)
                        .description("Auto-created from Web Grid")
                        .createdBy(defaultUser)
                        .createdAt(now)
                        .build();
                category = categoryRepository.save(category);
                categoryMap.put(categoryName.toLowerCase(), category);
            }

            // Resolve Brand
            Brand brand = null;
            String brandName = dto.getBrandName() != null ? dto.getBrandName().trim() : "";
            if (!brandName.isEmpty()) {
                brand = brandMap.get(brandName.toLowerCase());
                if (brand == null) {
                    brand = Brand.builder()
                            .brandName(brandName)
                            .description("Auto-created from Web Grid")
                            .status(BrandStatus.ACTIVE)
                            .createdBy(defaultUser)
                            .createdAt(now)
                            .build();
                    brand = brandRepository.save(brand);
                    brandMap.put(brandName.toLowerCase(), brand);
                }
            }

            ProductStatus prodStatus = "INACTIVE".equalsIgnoreCase(dto.getStatus())
                    ? ProductStatus.INACTIVE
                    : ProductStatus.ACTIVE;

            try {
                Product product = Product.builder()
                        .productName(productName)
                        .description(dto.getDescription())
                        .category(category)
                        .brand(brand)
                        .status(prodStatus)
                        .isPerishable(false)
                        .createdBy(defaultUser)
                        .createdAt(now)
                        .build();
                Product savedProduct = productRepository.save(product);

                ProductVariant variant = ProductVariant.builder()
                        .product(savedProduct)
                        .sku(sku)
                        .sellingPrice(sellingPrice)
                        .costPrice(dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO)
                        .status(ProductVariantStatus.ACTIVE)
                        .build();
                ProductVariant savedVariant = productVariantRepository.save(variant);
                existingSkus.add(upperSku);

                if (dto.getSize() != null && !dto.getSize().trim().isEmpty() && sizeVariation != null) {
                    VariationOption sizeOption = getOrCreateOption(sizeVariation, dto.getSize().trim());
                    VariantOptionValue mapping = VariantOptionValue.builder()
                            .variant(savedVariant)
                            .option(sizeOption)
                            .build();
                    variantOptionValueRepository.save(mapping);
                }

                if (dto.getColor() != null && !dto.getColor().trim().isEmpty() && colorVariation != null) {
                    VariationOption colorOption = getOrCreateOption(colorVariation, dto.getColor().trim());
                    VariantOptionValue mapping = VariantOptionValue.builder()
                            .variant(savedVariant)
                            .option(colorOption)
                            .build();
                    variantOptionValueRepository.save(mapping);
                }

                String invStatus = stock > 10 ? "NORMAL" : (stock > 0 ? "LOW_STOCK" : "OUT_OF_STOCK");
                Inventory inventory = Inventory.builder()
                        .variant(savedVariant)
                        .quantity(stock)
                        .reservedQuantity(0)
                        .reorderLevel(10)
                        .status(invStatus)
                        .isPerishable(false)
                        .createdBy(defaultUser)
                        .createdAt(now)
                        .build();
                inventoryRepository.save(inventory);

                successCount++;
            } catch (Exception ex) {
                log.error("Failed to import product row {}: {}", displayRowNumber, ex.getMessage());
                errors.add(new RowError(displayRowNumber, productName, sku, ex.getMessage()));
            }
        }

        return ProductImportResultDto.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    private Variation getOrCreateVariation(String name) {
        return variationRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> variationRepository.save(Variation.builder().name(name).build()));
    }

    private VariationOption getOrCreateOption(Variation variation, String value) {
        return variationOptionRepository.findByVariation_VariationIdAndValueIgnoreCase(variation.getVariationId(), value)
                .orElseGet(() -> variationOptionRepository.save(
                        VariationOption.builder()
                                .variation(variation)
                                .value(value.trim())
                                .build()
                ));
    }

    private String generateAutoSku(String name, String size, String color) {
        String clean = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (clean.length() > 6) clean = clean.substring(0, 6);
        String s = size.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String c = color.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        StringBuilder sb = new StringBuilder(clean);
        if (!c.isEmpty()) sb.append("-").append(c);
        if (!s.isEmpty()) sb.append("-").append(s);
        sb.append("-").append(random);
        return sb.toString();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    double num = cell.getNumericCellValue();
                    if (num == Math.floor(num)) {
                        return String.valueOf((long) num);
                    }
                    return String.valueOf(num);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private BigDecimal getCellBigDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String str = cell.getStringCellValue().replace(",", "").trim();
                if (str.isEmpty()) return null;
                return new BigDecimal(str);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Integer getCellInteger(Cell cell) {
        BigDecimal bd = getCellBigDecimal(cell);
        return bd != null ? bd.intValue() : null;
    }
}
