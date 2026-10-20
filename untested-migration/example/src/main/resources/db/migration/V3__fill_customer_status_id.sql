UPDATE customer
SET status_id = s.id
FROM customer_status s
WHERE s.code = upper(trim(customer.status));

UPDATE customer
SET status_id = (SELECT id FROM customer_status WHERE code = 'INACTIVE')
WHERE status_id IS NULL;
