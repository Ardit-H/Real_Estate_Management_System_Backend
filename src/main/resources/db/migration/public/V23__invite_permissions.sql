
INSERT INTO public.permissions (name, http_method, api_path, description) VALUES
                                                                              ('invite.create', 'POST',  '/api/invites',        'Generate invitation link'),
                                                                              ('invite.verify', 'GET',   '/api/invites/*',      'Verify invitation token'),
                                                                              ('invite.use',    'PATCH', '/api/invites/*/use',  'Mark invitation as used')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('invite.create', 'invite.verify', 'invite.use')
ON CONFLICT DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name IN ('AGENT', 'CLIENT')
  AND p.name IN ('invite.verify', 'invite.use')
ON CONFLICT DO NOTHING;