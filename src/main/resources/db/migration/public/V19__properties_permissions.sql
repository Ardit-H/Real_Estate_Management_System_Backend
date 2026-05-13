INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name = 'CLIENT'
  AND p.name = 'property.price.history'
ON CONFLICT DO NOTHING;