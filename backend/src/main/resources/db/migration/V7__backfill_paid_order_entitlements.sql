INSERT INTO entitlement_grants (
    user_id,
    order_item_id,
    product_id,
    package_id,
    access_policy,
    status,
    granted_at
)
SELECT o.user_id,
       oi.id,
       oi.product_id,
       oi.package_id,
       oi.access_policy,
       'ACTIVE',
       COALESCE(pe.processed_at, o.updated_at, o.created_at)
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
LEFT JOIN payment_attempts pa ON pa.order_id = o.id
LEFT JOIN payment_events pe
       ON pe.payment_attempt_id = pa.id
      AND pe.processing_status = 'APPLIED'
WHERE o.status = 'PAID'
ON DUPLICATE KEY UPDATE id = entitlement_grants.id;
