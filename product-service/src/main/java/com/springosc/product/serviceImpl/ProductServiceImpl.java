package com.springosc.product.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductDataDTO;
import com.springosc.product.dao.CartRepository;
import com.springosc.product.dao.CategoriesRepository;
import com.springosc.product.dao.ProductRepository;
import com.springosc.product.dao.RecentlyViewedRepository;
import com.springosc.product.dto.*;
import com.springosc.product.entity.Cart;
import com.springosc.product.entity.Product;
import com.springosc.product.entity.RecentlyViewed;
import com.springosc.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final CategoriesRepository categoriesRepository;

    private final RecentlyViewedRepository recentlyViewedRepository;

    private final CartRepository cartRepository;

    private final KafkaTemplate<Object, String> kafkaTemplate;

    private final HazelcastInstance hazelcastInstance;

    private final FetchProductData fetchProductData;

    private IMap<String, CategoryProductMap> productData;

    private final IMap<String, List<String>> recentlyViewedMap;

    public ProductServiceImpl(ProductRepository productRepository, CategoriesRepository categoriesRepository, RecentlyViewedRepository recentlyViewedRepository, CartRepository cartRepository, KafkaTemplate<Object, String> kafkaTemplate, HazelcastInstance hazelcastInstance, FetchProductData fetchProductData,
                              IMap<String, CategoryProductMap> productData,
                              IMap<String, List<String>> recentlyViewedMap) {
        this.productRepository = productRepository;
        this.categoriesRepository = categoriesRepository;
        this.recentlyViewedRepository = recentlyViewedRepository;
        this.cartRepository = cartRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.hazelcastInstance = hazelcastInstance;
        this.fetchProductData = fetchProductData;
        this.productData = productData;
        this.recentlyViewedMap = recentlyViewedMap;
    }


    @Override
    public void publishProductsToKafka() {
        List<Product> products = productRepository.findAll();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<ProductDTO> productDTOList = new ArrayList<>();
            for (Product product : products) {
                ProductDTO productDTO = new ProductDTO(product.getCategory().getCategoryId(), product.getProductId(), product);
                productDTOList.add(productDTO);
            }
            String productJson = objectMapper.writeValueAsString(productDTOList);
            kafkaTemplate.send("product-data", productJson);
        } catch (Exception e) {
            log.error("Error occurred while producing data in topic");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        productData = hazelcastInstance.getMap("productData");
        publishProductsToKafka();
        fetchProductData.getProductData();
        fetchAndStoreAllRecentlyViewedProducts();
    }

    public void fetchAndStoreAllRecentlyViewedProducts() {
        try {
            List<RecentlyViewed> allRecentlyViewedProductsEntities = recentlyViewedRepository.findAll();

            for (RecentlyViewed recentlyViewed : allRecentlyViewedProductsEntities) {
                log.info("Inside for Loop to iterate recentlyViewed");
                String userId = recentlyViewed.getUserId();
                String productWithCategory = recentlyViewed.getProductId() + ":" + recentlyViewed.getCategoryId();

                log.info("Product with categories are: {}", productWithCategory);

                recentlyViewedMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(productWithCategory);
                log.info("Data Stored in Recently Viewed Map is: {}", recentlyViewed);
            }
        } catch (Exception e) {
            log.error("Error occurred while fetching and storing all recently viewed products: {}", e.getMessage());
        }
    }

    @Override
    public List<Map.Entry<String, Integer>> getCategories(Map<String, CategoryProductMap> productDetailsMap) {
        // Check if the input map is null or empty
        if (productDetailsMap == null || productDetailsMap.isEmpty()) {
            log.error("Product map is null or empty.");
            return Collections.emptyList();
        }

        // Create a new map to store category view counts
        Map<String, Integer> categoryViewCounts = new HashMap<>();

        // Iterate over each entry productDetailsMap
        for (Map.Entry<String, CategoryProductMap> categoryEntry : productDetailsMap.entrySet()) {
            // Extract category ID and CategoryProductMap object from the entry
            String categoryId = categoryEntry.getKey();
            CategoryProductMap productMap = categoryEntry.getValue();

            int categoryViewCount = 0;

            for (Map.Entry<String, ProductDataDTO> productEntry : productMap.getProductMapMap().entrySet()) {
                ProductDataDTO product = productEntry.getValue();
                categoryViewCount += product.getViewCount();
            }
            categoryViewCounts.put(categoryId, categoryViewCount);
        }

        // Convert the category view counts map to a list of map entries
        List<Map.Entry<String, Integer>> sortedCategoryViewCounts = new ArrayList<>(categoryViewCounts.entrySet());

        sortedCategoryViewCounts.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        log.info("Categories are: " + sortedCategoryViewCounts);

        return sortedCategoryViewCounts;
    }

    @Override
    public List<CategoryProductMap> getProducts(Map<String, CategoryProductMap> productData) {
        //new list to store sorted CategoryProductMap objects
        List<CategoryProductMap> sortedProductList = new ArrayList<>();

        for (CategoryProductMap productMap : productData.values()) {
            // converting the product map to a list of map entries
            List<Map.Entry<String, ProductDataDTO>> sortedProducts = new ArrayList<>(productMap.getProductMapMap().entrySet());

            sortedProducts.sort((a, b) -> {
                int viewCountA = a.getValue().getViewCount();
                int viewCountB = b.getValue().getViewCount();

                return Integer.compare(viewCountB, viewCountA);
            });

            //new linkedHashMap to preserve the order of sorted products
            Map<String, ProductDataDTO> sortedProductsMap = new LinkedHashMap<>();
            for (Map.Entry<String, ProductDataDTO> productEntry : sortedProducts) {
                sortedProductsMap.put(productEntry.getKey(), productEntry.getValue());
            }

            CategoryProductMap newCategoryProductMap = CategoryProductMap.newBuilder()
                    .putAllProductMap(sortedProductsMap)
                    .build();

            sortedProductList.add(newCategoryProductMap);
        }

        log.info("Sorted list of products: " + sortedProductList);

        return sortedProductList;
    }


    @Override
    public CustomResponseDTO generateDashboardData(String userId, Map<String, CategoryProductMap> productData) {
        CustomResponseDTO responseDTO = new CustomResponseDTO();
        try {
            boolean userExists = isUserExistsInRecentlyViewed(userId);
            if (userExists) {
                log.info("user exists: " + userId);
                responseDTO = existingUserDashboard(userId, productData);
                log.info("user is existing user: " + existingUserDashboard(userId, productData));
            } else {
                log.info("User is New User: ");
                List<Map.Entry<String, Integer>> categories = getCategories(productData);
                List<CategoryProductMap> products = getProducts(productData);

                ArrayList<ProductDetailsDTO> data = new ArrayList<>();

                ProductDetailsDTO categoryData = new ProductDetailsDTO();
                categoryData.setType("Categories");
                ArrayList<CategoryDTO> categoryDTOs = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : categories) {
                    CategoryDTO categoryDTO = new CategoryDTO();
                    String categoryId = entry.getKey();
                    String categoryName = categoriesRepository.findCategoryNameByCategoryId(categoryId);
                    categoryDTO.setCategoryId(categoryId);
                    categoryDTO.setCategoryName(categoryName);
                    categoryDTOs.add(categoryDTO);
                }
                categoryData.setCategories(categoryDTOs);
                data.add(categoryData);

                ProductDetailsDTO productDetailsDTO = new ProductDetailsDTO();
                productDetailsDTO.setType("Featured Products");
                ArrayList<FeaturedProductDTO> featuredProductDTOs = new ArrayList<>();
                for (CategoryProductMap categoryProductMap : products) {
                    for (Map.Entry<String, ProductDataDTO> entry : categoryProductMap.getProductMapMap().entrySet()) {
                        ProductDataDTO productDTO = entry.getValue();
                        FeaturedProductDTO featuredProductDTO = new FeaturedProductDTO();
                        featuredProductDTO.setProductId(entry.getKey());
                        featuredProductDTO.setCategoryId(productDTO.getCategoryId());
                        featuredProductDTO.setProdName(productDTO.getProductName());
                        featuredProductDTO.setProdMarketPrice(productDTO.getProductPrice());
                        featuredProductDTOs.add(featuredProductDTO);
                    }
                }
                productDetailsDTO.setFeaturedProducts(featuredProductDTOs);
                data.add(productDetailsDTO);

                responseDTO.setCode(200);
                DataObjectDTO dataObjectDTO = new DataObjectDTO();
                dataObjectDTO.setData(data);
                responseDTO.setDataObject(dataObjectDTO);
            }
        } catch (Exception e) {
            responseDTO.setCode(500);
            log.error("Error occurred while generating dashboard data: {}", e.getMessage());
        }
        return responseDTO;
    }


    public CustomResponseDTO existingUserDashboard(String userId, Map<String, CategoryProductMap> productData) {
        CustomResponseDTO responseDTO = new CustomResponseDTO();
        try {
            List<String> recentlyViewedProductIdsWithCategories = recentlyViewedMap.get(userId);
            log.info("Recently viewed product IDs with categories for user {}: {}", userId, recentlyViewedProductIdsWithCategories);

            if (recentlyViewedProductIdsWithCategories != null && !recentlyViewedProductIdsWithCategories.isEmpty()) {

                List<ProductDTO> recentlyViewedProducts = getProductDetails(recentlyViewedProductIdsWithCategories);
                log.info("Recently Viewed Products are: {}", recentlyViewedProducts);

                List<ProductDTO> similarProducts = getSimilarProducts(recentlyViewedProductIdsWithCategories);
                log.info("Similar Products are: {}", similarProducts);

                List<Map.Entry<String, Integer>> categories = getCategories(productData);

                List<DashboardCartDTO> cartProducts = getCartProducts(userId);
                log.info("Cart Products are: {}", cartProducts);

                List<ExistingUserProductDataDTO> data = new ArrayList<>();

                ExistingUserProductDataDTO existingUserProductDataDTO = new ExistingUserProductDataDTO();
                existingUserProductDataDTO.setType("Categories");
                ArrayList<CategoryDTO> categoryDTOs = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : categories) {
                    CategoryDTO categoryDTO = new CategoryDTO();    
                    String categoryId = entry.getKey();
                    String categoryName = categoriesRepository.findCategoryNameByCategoryId(categoryId);
                    categoryDTO.setCategoryId(categoryId);
                    categoryDTO.setCategoryName(categoryName);
                    categoryDTOs.add(categoryDTO);
                }
                existingUserProductDataDTO.setCategories(categoryDTOs);
                data.add(existingUserProductDataDTO);

                ExistingUserProductDataDTO recentlyViewedData = ExistingUserProductDataDTO.builder()
                        .type("Recently Viewed Products")
                        .recentlyViewed(recentlyViewedProducts)
                        .build();
                data.add(recentlyViewedData);

                ExistingUserProductDataDTO similarProductsData = ExistingUserProductDataDTO.builder()
                        .type("Similar Products")
                        .similarProducts(similarProducts)
                        .build();
                data.add(similarProductsData);

                ExistingUserProductDataDTO cartProductsData = ExistingUserProductDataDTO.builder()
                        .type("Cart")
                        .cartProducts(cartProducts)
                        .build();
                data.add(cartProductsData);

                DataObjectDTO<ExistingUserProductDataDTO> dataObjectDTO = new DataObjectDTO<>();
                dataObjectDTO.setData(data);

                responseDTO.setCode(200);
                responseDTO.setDataObject(dataObjectDTO);
                log.info("Constructed data object DTO: {}", dataObjectDTO);

            } else {
                log.info("No recently viewed products found for user {}", userId);
            }

        } catch (Exception e) {
            responseDTO.setCode(500);
            log.error("Error occurred while generating dashboard data: {}", e.getMessage());
        }
        return responseDTO;
    }


    private List<ProductDTO> getSimilarProducts(List<String> recentlyViewedProductIdsWithCategories) {
        log.info("Inside getSimilarProducts Method");

        List<ProductDTO> similarProducts = new ArrayList<>();

        try {
            for (String productWithCategory : recentlyViewedProductIdsWithCategories) {
                String[] parts = productWithCategory.split(":");
                if (parts.length != 2) {
                    log.error("Invalid product format: {}", productWithCategory);
                    continue;
                }
                String productId = parts[0];
                String categoryId = parts[1];

                log.info("Fetching products for category: {}", categoryId);

                CategoryProductMap categoryProductMap = productData.get(categoryId);
                if (categoryProductMap == null) {
                    log.error("No CategoryProductMap found for categoryId: {}", categoryId);
                    continue;
                }
                Map<String, ProductDataDTO> productMap = categoryProductMap.getProductMapMap();

                List<ProductDTO> productDTOList = productMap.values().stream()
                        .map(product -> {
                            ProductDTO productDTO = new ProductDTO();
                            productDTO.setProductId(product.getProductId());
                            productDTO.setCategoryId(product.getCategoryId());
                            productDTO.setProductName(product.getProductName());
                            productDTO.setProdMarketPrice(product.getProductPrice());
                            productDTO.setProductDescription(product.getProductDescription());
                            productDTO.setViewCount(product.getViewCount());
                            return productDTO;
                        })
                        .toList();

                List<ProductDTO> sortedProducts = productDTOList.stream()
                        .sorted(Comparator.comparingInt(com.springosc.product.dto.ProductDTO::getViewCount).reversed())
                        .collect(Collectors.toList());

                log.info("Sorted products: {}", sortedProducts);

                for (com.springosc.product.dto.ProductDTO productDTO : sortedProducts) {
                    String sortedProductId = productDTO.getProductId();
                    if (!productId.equals(sortedProductId)
                            && similarProducts.stream().noneMatch(p -> p.getProductId().equals(sortedProductId))
                            && !recentlyViewedProductIdsWithCategories.contains(sortedProductId + ":" + categoryId)) {
                        similarProducts.add(productDTO);
                        break;
                    }
                    log.info("Adding product to similar products: {}", productDTO);
                }
                if (similarProducts.size() >= 6) {
                    break;
                }
            }

            if (similarProducts.size() < 6) {
                String lastCategory = recentlyViewedProductIdsWithCategories.get(recentlyViewedProductIdsWithCategories.size() - 1).split(":")[1];
                CategoryProductMap lastCategoryProductMap = productData.get(lastCategory);
                if (lastCategoryProductMap != null) {
                    Map<String, ProductDataDTO> productMap = lastCategoryProductMap.getProductMapMap();
                    List<ProductDTO> lastCategoryProducts = productMap.values().stream()
                            .map(product -> {
                                ProductDTO similarProduct = new ProductDTO();
                                similarProduct.setProductId(product.getProductId());
                                similarProduct.setCategoryId(product.getCategoryId());
                                similarProduct.setProductName(product.getProductName());
                                similarProduct.setProdMarketPrice(product.getProductPrice());
                                similarProduct.setProductDescription(product.getProductDescription());
                                similarProduct.setViewCount(product.getViewCount());
                                return similarProduct;
                            })
                            .sorted(Comparator.comparingInt(ProductDTO::getViewCount).reversed())
                            .toList();
                    for (ProductDTO productDTO : lastCategoryProducts) {
                        if (!similarProducts.contains(productDTO) && !recentlyViewedProductIdsWithCategories.contains(productDTO.getProductId() + ":" + lastCategory)) {
                            similarProducts.add(productDTO);
                            if (similarProducts.size() >= 6) {
                                break;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error occurred while fetching similar products: {}", e.getMessage());
        }

        return similarProducts;
    }


    private List<DashboardCartDTO> getCartProducts(String userId) {
        List<DashboardCartDTO> cartProducts = new ArrayList<>();
        try {
            List<Cart> cartEntities = cartRepository.findByUserIdAndQuantityGreaterThan(userId, 0);
            log.info("Fetched {} cart products for user: {}", cartEntities.size(), userId);

            for (Cart cartEntity : cartEntities) {
                log.debug("Processing cart item: {}", cartEntity);
                DashboardCartDTO cartProductDTO = new DashboardCartDTO();
                cartProductDTO.setUserId(cartEntity.getUserId());
                cartProductDTO.setProductId(cartEntity.getProductId());
                cartProductDTO.setProdName(cartEntity.getProductName());
                cartProductDTO.setProdMarketPrice(cartEntity.getProductPrice());
                log.info(String.valueOf(cartProductDTO));
                cartProductDTO.setCartQty(cartEntity.getQuantity());

                cartProducts.add(cartProductDTO);
            }
        } catch (Exception e) {
            log.error("Error occurred while fetching cart products: {}", e.getMessage());
        }
        return cartProducts;
    }


    public List<ProductDTO> getProductDetails(List<String> recentlyViewedProductIdsWithCategories) {
        List<ProductDTO> productDetailsList = new ArrayList<>();
        for (String recentlyViewedProduct : recentlyViewedProductIdsWithCategories) {
            String[] parts = recentlyViewedProduct.split(":");
            if (parts.length != 2) {
                log.info("Invalid format for recently viewed product: {}", recentlyViewedProduct);
                continue;
            }
            String productId = parts[0];
            String categoryId = parts[1];

            CategoryProductMap categoryProductMap = productData.get(categoryId);

            if (categoryProductMap != null) {
                Map<String, ProductDataDTO> productMap = categoryProductMap.getProductMapMap();

                if (productMap.containsKey(productId)) {
                    ProductDataDTO productDTO = productMap.get(productId);

                    ProductDTO productDetails = new ProductDTO();
                    productDetails.setProductId(productId);
                    productDetails.setCategoryId(categoryId);
                    productDetails.setProductName(productDTO.getProductName());
                    productDetails.setProdMarketPrice(productDTO.getProductPrice());
                    productDetails.setProductDescription(productDTO.getProductDescription());
                    productDetails.setViewCount(productDTO.getViewCount());

                    productDetailsList.add(productDetails);
                } else {
                    log.error("Product not found for productId {} and categoryId {}", productId, categoryId);
                }
            } else {
                log.error("No CategoryProductMap found for categoryId: {}", categoryId);
            }
        }
        return productDetailsList;
    }

    public boolean isUserExistsInRecentlyViewed(String userId) {
        log.info("isUserExistsInRecentlyViewed Executed ");
        return recentlyViewedMap.containsKey(userId);
    }

}


