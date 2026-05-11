package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

    @Override
	public Product process(final Product product) {
		final String loyaltyData = jdbcTemplate.query(
			"SELECT loyalityData FROM loyality_data WHERE productSku = ?",
			rs -> rs.next() ? rs.getString("loyalityData") : null,
			product.productSku()
		);

		final Product transformedProduct = loyaltyData == null
			? product
			: new Product(
				product.productId(),
				product.productSku(),
				product.productName(),
				product.productAmount(),
				loyaltyData
			);

		log.info("Transforming ({}) into ({})", product, transformedProduct);

		return transformedProduct;
	}

}
