package dojo.boot.l3;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Reference solution: constructor binding into a record, validated when the context starts. */
@ConfigurationProperties(prefix = "shipping")
@Validated
public record ShippingProperties(
		@NotBlank @Pattern(regexp = "https://.+") String carrierUrl,
		@DefaultValue("2s") Duration timeout,
		@DefaultValue("10") @Min(1) @Max(50) int maxParcels) {
}
