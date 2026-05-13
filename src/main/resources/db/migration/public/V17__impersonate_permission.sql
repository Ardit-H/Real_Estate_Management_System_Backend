
INSERT INTO public.permissions (name, http_method, api_path, description)
VALUES
    ('admin.impersonate.start', 'POST', '/api/admin/impersonate/*', 'Impersonate a user'),
    ('admin.impersonate.exit',  'POST', '/api/admin/impersonate/exit', 'Exit impersonation')
ON CONFLICT (name) DO NOTHING;


INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('admin.impersonate.start', 'admin.impersonate.exit')
ON CONFLICT DO NOTHING;