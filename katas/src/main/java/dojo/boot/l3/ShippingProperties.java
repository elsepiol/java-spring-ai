package dojo.boot.l3;

import java.time.Duration;

/**
 * KATA boot L3 "ShippingProperties" (tracks/07-spring-boot-web/L3-production.md, section 2: typed configuration).
 *
 * Bind the "shipping.*" properties into this record, validated at STARTUP (a bad value must stop
 * the application, not fail the first shipment at 3 a.m.):
 *   shipping.carrier-url   required, must start with https://
 *   shipping.timeout       a Duration ("3s", "500ms"); default 2s
 *   shipping.max-parcels   1..50; default 10
 * and make ShippingConfig register it. Use @ConfigurationProperties, @Validated, Bean Validation
 * annotations and @DefaultValue.
 */
public record ShippingProperties(String carrierUrl, Duration timeout, int maxParcels) {
}
